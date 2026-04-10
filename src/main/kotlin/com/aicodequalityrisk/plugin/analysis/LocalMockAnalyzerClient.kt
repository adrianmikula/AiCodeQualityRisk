package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.Finding
import com.aicodequalityrisk.plugin.model.RiskResult
import com.aicodequalityrisk.plugin.model.Severity
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class LocalMockAnalyzerClient : AnalyzerClient {

    private val configLoader = AnalysisConfigLoader()
    private val ruleFactory = RuleFactory()
    private val rules: List<Rule> by lazy {
        val config = configLoader.loadConfig()
        config.rules.map { ruleFactory.createRule(it) }
    }

    override fun analyze(input: AnalysisInput): RiskResult {
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
            rule.finding
        }.toMutableList()

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

        return RiskResult(
            score = boundedScore,
            complexityScore = boundedComplexity,
            duplicationScore = boundedDuplication,
            performanceScore = boundedPerformance,
            securityScore = boundedSecurity,
            findings = findings.take(7),
            explanations = explanations,
            sourceFilePath = input.filePath
        )
    }
}
