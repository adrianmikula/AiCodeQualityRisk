package com.aicodequalityrisk.plugin.analysis

import org.treesitter.TSNode

class EntropyScoreCalculator {

    fun calculateEntropyScores(
        methods: List<TSNode>,
        root: TSNode,
        source: String
    ): EntropyScores {
        return EntropyScores(
            boilerplateBloatScore = calculateBoilerplateBloatScore(methods, source),
            verboseCommentScore = calculateVerboseCommentScore(root, source),
            overDefensiveScore = calculateOverDefensiveScore(methods, source),
            poorNamingScore = calculatePoorNamingScore(methods, source),
            frameworkMisuseScore = calculateFrameworkMisuseScore(methods, source),
            excessiveDocumentationScore = calculateExcessiveDocumentationScore(root, source, methods.size)
        )
    }

    private fun calculateBoilerplateBloatScore(methods: List<TSNode>, source: String): Double {
        if (methods.isEmpty()) return 0.0

        val getterSetterCount = methods.count { isGetterOrSetter(it, source) }
        val totalMethods = methods.size
        val getterSetterRatio = getterSetterCount.toDouble() / totalMethods

        val avgMethodLength = if (totalMethods > 0) {
            methods.map { calculateMethodLength(it, source) }.average()
        } else 0.0

        return when {
            getterSetterRatio > 0.7 && avgMethodLength < 5 -> 0.9
            getterSetterRatio > 0.5 && avgMethodLength < 8 -> 0.7
            getterSetterRatio > 0.3 -> 0.4
            else -> 0.0
        }
    }

    private fun isGetterOrSetter(node: TSNode, source: String): Boolean {
        val methodText = extractText(node, source)
        val methodName = extractMethodName(node, source)

        val isGetter = methodName.startsWith("get") || methodName.startsWith("is")
        val isSetter = methodName.startsWith("set")

        if (!isGetter && !isSetter) return false

        val lines = methodText.lines().count { it.isNotBlank() }
        return lines <= 5
    }

    private fun calculateVerboseCommentScore(root: TSNode, source: String): Double {
        val sourceLines = source.lines()
        val totalLines = sourceLines.size
        if (totalLines == 0) return 0.0

        val commentLines = sourceLines.count { it.trim().startsWith("//") || it.trim().startsWith("/*") || it.trim().startsWith("*") }
        val codeLines = sourceLines.count { it.isNotBlank() && !it.trim().startsWith("//") && !it.trim().startsWith("/*") && !it.trim().startsWith("*") }

        val commentRatio = if (codeLines > 0) commentLines.toDouble() / codeLines else 0.0

        return when {
            commentRatio > 0.5 -> 0.9
            commentRatio > 0.3 -> 0.6
            commentRatio > 0.2 -> 0.3
            else -> 0.0
        }
    }

    private fun calculateOverDefensiveScore(methods: List<TSNode>, source: String): Double {
        if (methods.isEmpty()) return 0.0

        var nullCheckCount = 0
        var totalMethodLength = 0

        methods.forEach { method ->
            val methodText = extractText(method, source)
            totalMethodLength += methodText.lines().count { it.isNotBlank() }

            nullCheckCount += countOccurrences(methodText, "!= null")
            nullCheckCount += countOccurrences(methodText, "== null")
            nullCheckCount += countOccurrences(methodText, "?.let")
            nullCheckCount += countOccurrences(methodText, "?.takeIf")
        }

        val nullCheckRatio = if (totalMethodLength > 0) nullCheckCount.toDouble() / totalMethodLength else 0.0

        return when {
            nullCheckRatio > 0.3 -> 0.9
            nullCheckRatio > 0.2 -> 0.6
            nullCheckRatio > 0.1 -> 0.3
            else -> 0.0
        }
    }

    private fun calculatePoorNamingScore(methods: List<TSNode>, source: String): Double {
        if (methods.isEmpty()) return 0.0

        var singleLetterVarCount = 0
        var totalIdentifierCount = 0

        methods.forEach { method ->
            collectIdentifiers(method, source).forEach { identifier ->
                totalIdentifierCount++
                if (identifier.length == 1 && identifier[0].isLetter()) {
                    singleLetterVarCount++
                }
            }
        }

        val poorNamingRatio = if (totalIdentifierCount > 0) singleLetterVarCount.toDouble() / totalIdentifierCount else 0.0

        return when {
            poorNamingRatio > 0.3 -> 0.9
            poorNamingRatio > 0.2 -> 0.6
            poorNamingRatio > 0.1 -> 0.3
            else -> 0.0
        }
    }

    private fun collectIdentifiers(node: TSNode, source: String): List<String> {
        val identifiers = mutableListOf<String>()
        collectIdentifiersRecursive(node, source, identifiers)
        return identifiers
    }

    private fun collectIdentifiersRecursive(node: TSNode, source: String, identifiers: MutableList<String>) {
        if (node.getType() == "identifier") {
            val text = extractText(node, source).trim()
            if (text.isNotEmpty() && text[0].isLetter()) {
                identifiers.add(text)
            }
        }

        for (i in 0 until node.getChildCount()) {
            val child = node.getChild(i)
            if (child != null && !child.isNull()) {
                collectIdentifiersRecursive(child, source, identifiers)
            }
        }
    }

    private fun calculateFrameworkMisuseScore(methods: List<TSNode>, source: String): Double {
        var misuseCount = 0
        var totalMethods = methods.size

        methods.forEach { method ->
            val methodText = extractText(method, source)

            val swallowingExceptions = countOccurrences(methodText, "catch (Exception")
            val swallowingExceptions2 = countOccurrences(methodText, "catch (Throwable")
            val emptyCatches = countOccurrences(methodText, "catch") - countOccurrences(methodText, "{")

            misuseCount += swallowingExceptions + swallowingExceptions2 + maxOf(0, emptyCatches)
        }

        val misuseRatio = if (totalMethods > 0) misuseCount.toDouble() / totalMethods else 0.0

        return when {
            misuseRatio > 0.5 -> 0.9
            misuseRatio > 0.3 -> 0.6
            misuseRatio > 0.1 -> 0.3
            else -> 0.0
        }
    }

    private fun calculateExcessiveDocumentationScore(root: TSNode, source: String, methodCount: Int): Double {
        if (methodCount == 0) return 0.0

        val javadocPattern = Regex("/\\*\\*")
        val javadocCount = javadocPattern.findAll(source).count()

        val javadocRatio = javadocCount.toDouble() / methodCount

        val totalLines = source.lines().count { it.isNotBlank() }
        val javadocLines = source.lines().count { it.trim().startsWith("*") || it.trim().startsWith("/**") }
        val javadocLineRatio = if (totalLines > 0) javadocLines.toDouble() / totalLines else 0.0

        return when {
            javadocRatio > 1.0 && javadocLineRatio > 0.3 -> 0.9
            javadocRatio > 0.8 && javadocLineRatio > 0.2 -> 0.6
            javadocRatio > 0.5 && javadocLineRatio > 0.15 -> 0.3
            else -> 0.0
        }
    }

    private fun extractMethodName(node: TSNode, source: String): String {
        node.getChildByFieldName("name")?.takeIf { !it.isNull() }?.let {
            return extractText(it, source).trim()
        }
        return ""
    }

    private fun calculateMethodLength(node: TSNode, source: String): Int {
        val methodText = extractText(node, source)
        return methodText.lines().count { it.isNotBlank() }
    }

    private fun countOccurrences(text: String, pattern: String): Int {
        return text.split(pattern).size - 1
    }

    private fun extractText(node: TSNode, source: String): String {
        val start = node.startByte.coerceAtLeast(0)
        val end = node.endByte.coerceAtMost(source.length)
        return if (start < end && end <= source.length) {
            source.substring(start, end)
        } else {
            ""
        }
    }
}

data class EntropyScores(
    val boilerplateBloatScore: Double = 0.0,
    val verboseCommentScore: Double = 0.0,
    val overDefensiveScore: Double = 0.0,
    val poorNamingScore: Double = 0.0,
    val frameworkMisuseScore: Double = 0.0,
    val excessiveDocumentationScore: Double = 0.0
)
