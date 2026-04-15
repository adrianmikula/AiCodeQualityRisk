package com.aicodequalityrisk.plugin.analysis

import com.intellij.openapi.diagnostic.Logger
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterJava

class TreeSitterFuzzyDetector {
    private val logger = Logger.getInstance(TreeSitterFuzzyDetector::class.java)
    private val parser = TSParser().apply {
        setLanguage(TreeSitterJava())
    }

    fun detect(code: String, filePath: String?): FuzzyMetrics {
        logger.debug("Tree-sitter fuzzy detect invoked for $filePath")
        if (!isSupported(filePath)) return FuzzyMetrics()

        return try {
            val tree = parser.parseString(null, code)
            val metrics = analyzeTree(tree.rootNode, code)
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
        return extension == "java" || extension == "kt"
    }


    private fun analyzeTree(root: TSNode, source: String): FuzzyMetrics {
        val methods = collectMethodNodes(root)
        if (methods.size < 2) return FuzzyMetrics()

        val fingerprints = methods.map { node ->
            MethodFingerprint(
                name = extractMethodName(node, source),
                shingles = buildShingles(normalizeNodeTokens(node, source))
            )
        }

        val similarPairs = fingerprints.flatMapIndexed { index, fingerprint ->
            (index + 1 until fingerprints.size).mapNotNull { otherIndex ->
                val other = fingerprints[otherIndex]
                val similarity = jaccardSimilarity(fingerprint.shingles, other.shingles)
                if (similarity >= DUPLICATE_THRESHOLD) {
                    MethodSimilarityPair(
                        firstMethod = fingerprint.name,
                        secondMethod = other.name,
                        similarity = similarity
                    )
                } else {
                    null
                }
            }
        }

        return FuzzyMetrics(
            duplicateMethodCount = similarPairs.size,
            maxSimilarityScore = similarPairs.maxOfOrNull { it.similarity } ?: 0.0,
            duplicateMethodPairs = similarPairs
        )
    }

    private fun collectMethodNodes(node: TSNode): List<TSNode> {
        val results = mutableListOf<TSNode>()
        if (node.getType() == "method_declaration" || node.getType() == "constructor_declaration") {
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

    private fun buildShingles(tokens: List<String>, size: Int = 4): Set<String> {
        if (tokens.size < size) return tokens.toSet()
        return tokens.windowed(size) { it.joinToString(" ") }.toSet()
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

    private data class MethodFingerprint(
        val name: String,
        val shingles: Set<String>
    )

    companion object {
        private const val DUPLICATE_THRESHOLD = 0.62
        private val METHOD_NAME_REGEX = Regex("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")
    }
}
