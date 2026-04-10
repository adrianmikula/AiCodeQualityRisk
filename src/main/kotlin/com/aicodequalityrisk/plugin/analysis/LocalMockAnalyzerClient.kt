package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.Finding
import com.aicodequalityrisk.plugin.model.RiskResult
import com.aicodequalityrisk.plugin.model.Severity
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class LocalMockAnalyzerClient : AnalyzerClient {

    override fun analyze(input: AnalysisInput): RiskResult {
        val findings = mutableListOf<Finding>()
        val diff = input.diffText
        val snapshot = input.fileSnapshot

        var score = 8

        if (diff.contains("TODO") || snapshot.contains("TODO")) {
            findings += Finding(
                title = "Deferred work marker detected",
                detail = "TODO markers often indicate incomplete behavior in changed code.",
                severity = Severity.LOW
            )
            score += 8
        }

        if (snapshot.contains("!!")) {
            findings += Finding(
                title = "Potential null safety issue",
                detail = "Non-null assertion detected (`!!`), which may throw at runtime.",
                severity = Severity.HIGH
            )
            score += 30
        }

        if (Regex("""catch\s*\(\s*Exception""").containsMatchIn(snapshot)) {
            findings += Finding(
                title = "Broad exception catch",
                detail = "Catching broad Exception can hide root causes and reduce recoverability.",
                severity = Severity.MEDIUM
            )
            score += 18
        }

        if (Regex("""Thread\.sleep\(""").containsMatchIn(snapshot)) {
            findings += Finding(
                title = "Blocking call in code path",
                detail = "Thread.sleep usage can create responsiveness and timing issues.",
                severity = Severity.MEDIUM
            )
            score += 12
        }

        if (Regex("""(?m)^\+.*(if|for|while)\s*\(""").containsMatchIn(diff) && diff.length > 2000) {
            findings += Finding(
                title = "Complex change footprint",
                detail = "Large control-flow-heavy diff raises review and regression risk.",
                severity = Severity.MEDIUM
            )
            score += 14
        }

        if (findings.isEmpty()) {
            findings += Finding(
                title = "No high-risk patterns detected",
                detail = "Current heuristics found no obvious risk hotspots in this change set.",
                severity = Severity.LOW
            )
            score += 5
        }

        val boundedScore = score.coerceIn(0, 100)
        val explanations = listOf(
            "Risk score combines lightweight syntax heuristics and diff footprint signals.",
            "Use this score as triage guidance; prioritize HIGH severity findings first."
        )

        return RiskResult(
            score = boundedScore,
            findings = findings.take(7),
            explanations = explanations,
            sourceFilePath = input.filePath
        )
    }
}
