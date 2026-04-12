package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.Finding
import com.aicodequalityrisk.plugin.model.RiskResult
import com.aicodequalityrisk.plugin.model.Severity
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger

@Service(Service.Level.PROJECT)
class LocalMockAnalyzerClient : AnalyzerClient {

    private val logger = Logger.getInstance(LocalMockAnalyzerClient::class.java)
    private val configLoader = AnalysisConfigLoader()
    private val ruleFactory = RuleFactory()
    private val rules: List<Rule> by lazy {
        val config = configLoader.loadConfig()
        config.rules.map { ruleFactory.createRule(it) }
    }

    override fun analyze(input: AnalysisInput): RiskResult {
        logger.debug("Analyzing file=${input.filePath} trigger=${input.trigger}")
        var totalScore = 8
        var complexityScore = 0
        var duplicationScore = 0
        var performanceScore = 0
        var securityScore = 0

        val findings = rules.filter { it.matches(input) }.map { rule ->
            totalScore += rule.scoreDelta
            when (rule.category) {
                Category.COMPLEXITY -> complexityScore += rule.scoreDelta
                Category.DUPLICATION -> duplicationScore += rule.scoreDelta
                Category.PERFORMANCE -> performanceScore += rule.scoreDelta
                Category.SECURITY -> securityScore += rule.scoreDelta
            }
            val location = estimateLineNumber(input, rule.pattern)
            rule.finding.copy(filePath = input.filePath, lineNumber = location)
        }.toMutableList()

        input.fuzzyMetrics.takeIf { it.duplicateMethodCount > 0 }?.let { fuzzy ->
            logger.info("Detected fuzzy duplicate method bodies count=${fuzzy.duplicateMethodCount} for ${input.filePath}")
            totalScore += 8
            duplicationScore += 8
            findings += Finding(
                title = "Possible duplicated logic detected",
                detail = "Detected ${fuzzy.duplicateMethodCount} similar method body pair(s) with highest similarity ${"%.0f".format(fuzzy.maxSimilarityScore * 100)}%.",
                severity = Severity.MEDIUM,
                category = Category.DUPLICATION,
                filePath = input.filePath
            )
        }

        if (findings.isEmpty()) {
            findings += Finding(
                title = "No high-risk patterns detected",
                detail = "Current heuristics found no obvious risk hotspots in this change set.",
                severity = Severity.LOW
            )
            totalScore += 5
        }

        val boundedScore = totalScore.coerceIn(0, 100)
        val boundedComplexity = complexityScore.coerceIn(0, 100)
        val boundedDuplication = duplicationScore.coerceIn(0, 100)
        val boundedPerformance = performanceScore.coerceIn(0, 100)
        val boundedSecurity = securityScore.coerceIn(0, 100)

        val explanations = listOf(
            "Risk score combines lightweight syntax heuristics and diff footprint signals.",
            "Use this score as triage guidance; prioritize HIGH severity findings first."
        )

        val result = RiskResult(
            score = boundedScore,
            complexityScore = boundedComplexity,
            duplicationScore = boundedDuplication,
            performanceScore = boundedPerformance,
            securityScore = boundedSecurity,
            findings = findings.take(7),
            explanations = explanations,
            sourceFilePath = input.filePath
        )
        logger.info("Computed RiskResult(score=${result.score}, findings=${result.findings.size}) for file=${input.filePath}")
        return result
    }

    private fun estimateLineNumber(input: AnalysisInput, pattern: PatternConfig): Int? {
        return when (pattern.type) {
            "contains" -> pattern.value?.let { lineContaining(input.fileSnapshot, it) }
            "regex" -> pattern.value?.let { lineMatching(input.fileSnapshot, it) }
            "complex" -> pattern.conditions?.mapNotNull { condition -> estimateLineNumberForCondition(input, condition) }?.firstOrNull()
            else -> null
        }
    }

    private fun estimateLineNumberForCondition(input: AnalysisInput, condition: ConditionConfig): Int? {
        return when (condition.type) {
            "contains" -> (condition.value as? String)?.let { lineContaining(input.fileSnapshot, it) }
            "regex" -> (condition.value as? String)?.let { lineMatching(input.fileSnapshot, it) }
            else -> null
        }
    }

    private fun lineContaining(text: String, search: String): Int? {
        return text.lines().indexOfFirst { it.contains(search) }.takeIf { it >= 0 }?.plus(1)
    }

    private fun lineMatching(text: String, regexValue: String): Int? {
        return Regex(regexValue).find(text)?.let { match ->
            text.substring(0, match.range.first).count { it == '\n' } + 1
        }
    }
}
