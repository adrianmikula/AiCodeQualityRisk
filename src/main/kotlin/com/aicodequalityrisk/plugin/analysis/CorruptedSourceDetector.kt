package com.aicodequalityrisk.plugin.analysis

import com.intellij.openapi.diagnostic.Logger
import com.github.javaparser.JavaParser
import org.treesitter.TSParser
import org.treesitter.TreeSitterJava

class CorruptedSourceDetector {
    private val logger = Logger.getInstance(CorruptedSourceDetector::class.java)
    private val javaParser = JavaParser()
    private val treeSitterParser = TSParser().apply {
        setLanguage(TreeSitterJava())
    }

    fun detect(code: String, filePath: String?): CorruptedSourceMetrics {
        logger.debug("Corrupted source detection invoked for $filePath")
        if (!isSupported(filePath)) return CorruptedSourceMetrics()

        val parseFailed = checkParseFailure(code, filePath)
        val markdownTokenCount = countMarkdownTokens(code)
        val xmlFragmentCount = countXmlFragments(code)
        val unbalancedBraceCount = countUnbalancedBraces(code)
        val mixedLanguageDensity = calculateMixedLanguageDensity(code)

        val hasCorruptedContent = parseFailed ||
            markdownTokenCount > 0 ||
            xmlFragmentCount > 0 ||
            unbalancedBraceCount > 0 ||
            mixedLanguageDensity > 0.3

        return CorruptedSourceMetrics(
            parseFailed = parseFailed,
            markdownTokenCount = markdownTokenCount,
            xmlFragmentCount = xmlFragmentCount,
            unbalancedBraceCount = unbalancedBraceCount,
            mixedLanguageDensity = mixedLanguageDensity,
            hasCorruptedContent = hasCorruptedContent
        )
    }

    private fun isSupported(filePath: String?): Boolean {
        val extension = filePath?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()
        return extension == "java" || extension == "kt"
    }

    private fun checkParseFailure(code: String, filePath: String?): Boolean {
        val javaParserFailed = try {
            val result = javaParser.parse(code)
            !result.isSuccessful
        } catch (e: Exception) {
            true
        }

        val treeSitterFailed = try {
            treeSitterParser.parseString(null, code) == null
        } catch (e: Exception) {
            true
        }

        // Only mark as parse failure if BOTH parsers fail
        // (single parser failure might be due to version-specific syntax)
        return javaParserFailed && treeSitterFailed
    }

    private fun countMarkdownTokens(code: String): Int {
        var count = 0
        // Markdown code fence markers
        count += code.countOccurrences("```")
        // Markdown file path markers
        count += code.countOccurrences("<file path=")
        count += code.countOccurrences("```java")
        count += code.countOccurrences("```kotlin")
        count += code.countOccurrences("```kt")
        return count
    }

    private fun countXmlFragments(code: String): Int {
        var count = 0
        // XML-like tags
        count += code.countOccurrences("<[^!/][^>]*>")
        count += code.countOccurrences("</[^>]*>")
        count += code.countOccurrences("<[!?][^>]*>")
        return count
    }

    private fun countUnbalancedBraces(code: String): Int {
        val braces = mapOf(
            '{' to '}',
            '(' to ')',
            '[' to ']'
        )

        var totalUnbalanced = 0
        for ((open, close) in braces) {
            val openCount = code.count { it == open }
            val closeCount = code.count { it == close }
            totalUnbalanced += kotlin.math.abs(openCount - closeCount)
        }
        return totalUnbalanced
    }

    private fun calculateMixedLanguageDensity(code: String): Double {
        // Count prose-like patterns (sentences with spaces and punctuation)
        val proseLines = code.lines().count { line ->
            line.isNotBlank() &&
            !line.trimStart().startsWith("//") &&
            !line.trimStart().startsWith("/*") &&
            !line.trimStart().startsWith("*") &&
            line.contains(Regex("[.!?]")) &&
            line.split(" ").size > 3 &&
            !line.contains(Regex("\\b(class|public|private|protected|void|int|String|return|if|else|for|while|import|package)\\b"))
        }

        val totalLines = code.lines().count { it.isNotBlank() }
        return if (totalLines > 0) proseLines.toDouble() / totalLines else 0.0
    }

    private fun String.countOccurrences(pattern: String): Int {
        return if (pattern.startsWith("<") && pattern.contains(">")) {
            // Regex pattern for XML tags
            Regex(pattern).findAll(this).count()
        } else {
            // Simple string count
            this.split(pattern).size - 1
        }
    }
}
