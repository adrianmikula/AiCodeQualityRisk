package com.aicodequalityrisk.plugin.analysis

import com.intellij.openapi.diagnostic.Logger
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterJava
import org.treesitter.TreeSitterKotlin
import org.treesitter.TreeSitterScala

class TreeSitterFuzzyDetector {
    private val logger = Logger.getInstance(TreeSitterFuzzyDetector::class.java)
    private val javaParser = TSParser().apply {
        setLanguage(TreeSitterJava())
    }
    private val kotlinParser = TSParser().apply {
        setLanguage(TreeSitterKotlin())
    }
    private val scalaParser = TSParser().apply {
        setLanguage(TreeSitterScala())
    }
    private val thresholdCalculator = AdaptiveThresholdCalculator()
    private val shingleBuilder = MultiGranularShingleBuilder()
    private val entropyCalculator = EntropyScoreCalculator()
    private val astComparator = ASTSubtreeComparator()
    private val repetitionIntensityCalculator = LLMRepetitionIntensity()

    fun detect(code: String, filePath: String?): FuzzyMetrics {
        logger.debug("Tree-sitter fuzzy detect invoked for $filePath")
        if (!isSupported(filePath)) return FuzzyMetrics()

        return try {
            val parser = getParserForFile(filePath)
            val tree = parser.parseString(null, code)
            val metrics = analyzeTree(tree.rootNode, code, filePath)
            if (metrics.duplicateMethodCount > 0) {
                logger.debug("Fuzzy detector found ${metrics.duplicateMethodCount} duplicate method pairs for $filePath")
            }
            metrics
        } catch (error: Throwable) {
            logger.warn("Tree-sitter parse failed for $filePath", error)
            FuzzyMetrics()
        }
    }

    private fun isSupported(filePath: String?): Boolean {
        val extension = filePath?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()
        return extension == "java" || extension == "kt" || extension == "kts" || extension == "scala"
    }

    private fun getParserForFile(filePath: String?): TSParser {
        val extension = filePath?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()
        return when (extension) {
            "scala" -> scalaParser
            "kt", "kts" -> kotlinParser
            else -> javaParser
        }
    }


    private fun analyzeTree(root: TSNode, source: String, filePath: String?): FuzzyMetrics {
        val methods = collectMethodNodes(root)
        val entropyScores = entropyCalculator.calculateEntropyScores(methods, root, source)

        // Create both enhanced (shingle-based) and AST-based fingerprints
        val enhancedFingerprints = methods.map { node ->
            createEnhancedFingerprint(node, source)
        }
        
        val astFingerprints = methods.map { node ->
            createASTFingerprint(node, source)
        }

        // Calculate similarity using both approaches
        val similarPairs = enhancedFingerprints.flatMapIndexed { index, fingerprint ->
            (index + 1 until enhancedFingerprints.size).mapNotNull { otherIndex ->
                val other = enhancedFingerprints[otherIndex]
                val astFp1 = astFingerprints[index]
                val astFp2 = astFingerprints[otherIndex]
                
                val adaptiveThreshold = thresholdCalculator.calculateThreshold(
                    fingerprint, other, filePath
                )
                
                // Use shingle-based similarity as primary, AST-based as secondary
                val shingleSimilarity = fingerprint.getSimilarityScore(other)
                val structuralSimilarity = astFp1.getSimilarityScore(astFp2, astComparator)
                val treeEditDistance = astFp1.getTreeEditDistance(astFp2, astComparator)
                
                // Combine similarities: 60% shingle, 40% structural
                val combinedSimilarity = (shingleSimilarity * 0.6) + (structuralSimilarity * 0.4)

                if (combinedSimilarity >= adaptiveThreshold) {
                    MethodSimilarityPair(
                        firstMethod = fingerprint.name,
                        secondMethod = other.name,
                        similarity = combinedSimilarity,
                        threshold = adaptiveThreshold,
                        shingleBreakdown = createShingleBreakdown(fingerprint, other),
                        structuralSimilarity = structuralSimilarity,
                        treeEditDistance = treeEditDistance
                    )
                } else {
                    null
                }
            }
        }

        // Calculate LLM repetition intensity
        val repetitionIntensity = repetitionIntensityCalculator.calculateIntensity(
            similarPairs,
            methods.size,
            threshold = 0.5
        )

        return FuzzyMetrics(
            duplicateMethodCount = similarPairs.size,
            maxSimilarityScore = similarPairs.maxOfOrNull { it.similarity } ?: 0.0,
            duplicateMethodPairs = similarPairs,
            adaptiveThresholdsEnabled = true,
            multiGranularShinglingEnabled = true,
            entropyScoresEnabled = true,
            boilerplateBloatScore = entropyScores.boilerplateBloatScore,
            verboseCommentScore = entropyScores.verboseCommentScore,
            overDefensiveScore = entropyScores.overDefensiveScore,
            poorNamingScore = entropyScores.poorNamingScore,
            frameworkMisuseScore = entropyScores.frameworkMisuseScore,
            excessiveDocumentationScore = entropyScores.excessiveDocumentationScore,
            llmRepetitionIntensity = repetitionIntensity,
            astBasedSimilarityEnabled = true
        )
    }

    private fun collectMethodNodes(node: TSNode): List<TSNode> {
        val results = mutableListOf<TSNode>()
        // Java/Kotlin method types
        if (node.getType() == "method_declaration" || node.getType() == "constructor_declaration") {
            results.add(node)
        }
        // Scala method types
        if (node.getType() == "function_definition" ||
            node.getType() == "template_definition" ||
            node.getType() == "object_definition") {
            results.add(node)
        }
        for (index in 0 until node.getChildCount()) {
            val child = node.getChild(index)
            if (child != null && !child.isNull()) {
                results += collectMethodNodes(child)
            }
        }
        return results
    }

    private fun extractMethodName(node: TSNode, source: String): String {
        node.getChildByFieldName("name")?.takeIf { !it.isNull() }?.let {
            return extractText(it, source).trim()
        }

        val signature = extractText(node, source).lines().firstOrNull() ?: ""
        return METHOD_NAME_REGEX.find(signature)?.groupValues?.getOrNull(1)?.trim() ?: node.getType()
    }

    private fun normalizeNodeTokens(node: TSNode, source: String): List<String> {
        val childrenCount = node.getNamedChildCount()
        return if (childrenCount == 0) {
            listOf(normalizeLeafToken(node, source))
        } else {
            listOf(node.getType()) + (0 until childrenCount).flatMap { normalizeNodeTokens(node.getNamedChild(it), source) }
        }
    }

    private fun normalizeLeafToken(node: TSNode, source: String): String {
        return when (node.getType()) {
            "identifier" -> "IDENTIFIER"
            "type_identifier" -> "TYPE"
            "string_literal", "char_literal" -> "STRING_LITERAL"
            "decimal_integer_literal", "decimal_float_literal", "hex_integer_literal", "octal_integer_literal", "binary_integer_literal" -> "NUMERIC_LITERAL"
            "true", "false", "null" -> node.getType().uppercase()
            else -> node.getType()
        }
    }

    private fun createEnhancedFingerprint(node: TSNode, source: String): EnhancedMethodFingerprint {
        val name = extractMethodName(node, source)
        val tokens = normalizeNodeTokens(node, source)
        val shingles = shingleBuilder.buildMultiGranularShingles(tokens)
        val methodLength = calculateMethodLength(node, source)
        val complexity = calculateMethodComplexity(node, source)

        return EnhancedMethodFingerprint(
            name = name,
            shingles = shingles,
            methodLength = methodLength,
            complexity = complexity,
            tokenCount = tokens.size,
            uniqueTokenCount = tokens.toSet().size
        )
    }

    private fun createASTFingerprint(node: TSNode, source: String): ASTSubtreeFingerprint {
        val name = extractMethodName(node, source)
        val subtreeHash = astComparator.extractSubtreeHash(node, source)
        val methodLength = calculateMethodLength(node, source)
        val complexity = calculateMethodComplexity(node, source)
        val tokens = normalizeNodeTokens(node, source)

        return ASTSubtreeFingerprint(
            name = name,
            subtreeHash = subtreeHash,
            methodLength = methodLength,
            complexity = complexity,
            tokenCount = tokens.size
        )
    }

    private fun calculateMethodLength(node: TSNode, source: String): Int {
        val methodText = extractText(node, source)
        return methodText.lines().count { it.isNotBlank() }
    }

    private fun calculateMethodComplexity(node: TSNode, source: String): Int {
        val methodText = extractText(node, source)
        var complexity = 1

        complexity += countOccurrences(methodText, "if")
        complexity += countOccurrences(methodText, "for")
        complexity += countOccurrences(methodText, "while")
        complexity += countOccurrences(methodText, "catch")
        complexity += countOccurrences(methodText, "case")
        complexity += countOccurrences(methodText, "&&")
        complexity += countOccurrences(methodText, "||")
        complexity += countOccurrences(methodText, "?")

        return complexity
    }

    private fun countOccurrences(text: String, pattern: String): Int {
        return text.split(pattern).size - 1
    }

    private fun createShingleBreakdown(
        fp1: EnhancedMethodFingerprint,
        fp2: EnhancedMethodFingerprint
    ): ShingleBreakdown {
        val breakdown = mutableMapOf<Int, Double>()
        val shingleSizes = listOf(2, 4, 6, 8)

        for (size in shingleSizes) {
            val similarity = jaccardSimilarity(
                fp1.shingles[size] ?: emptySet(),
                fp2.shingles[size] ?: emptySet()
            )
            breakdown[size] = similarity
        }

        return ShingleBreakdown(breakdown)
    }

    private fun jaccardSimilarity(first: Set<String>, second: Set<String>): Double {
        if (first.isEmpty() && second.isEmpty()) return 1.0
        val intersection = first intersect second
        val union = first union second
        return if (union.isEmpty()) 0.0 else intersection.size.toDouble() / union.size.toDouble()
    }

    private fun extractText(node: TSNode, source: String): String {
        val start = node.startByte.coerceAtLeast(0)
        val end = node.endByte.coerceAtMost(source.length)
        return source.substring(start, end)
    }

    companion object {
        private val METHOD_NAME_REGEX = Regex("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")
    }
}
