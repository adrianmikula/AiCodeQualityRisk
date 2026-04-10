package com.aicodequalityrisk.plugin.model

data class AnalysisInput(
    val projectPath: String,
    val filePath: String?,
    val trigger: TriggerType,
    val diffText: String,
    val fileSnapshot: String
)

enum class TriggerType {
    EDIT,
    SAVE,
    FOCUS,
    MANUAL
}

data class RiskResult(
    val score: Int,
    val complexityScore: Int,
    val duplicationScore: Int,
    val performanceScore: Int,
    val securityScore: Int,
    val findings: List<Finding>,
    val explanations: List<String>,
    val sourceFilePath: String?
)

data class Finding(
    val title: String,
    val detail: String,
    val severity: Severity
)

enum class Severity {
    LOW,
    MEDIUM,
    HIGH
}

sealed class AnalysisViewState {
    data object Idle : AnalysisViewState()
    data object Loading : AnalysisViewState()
    data class Ready(val result: RiskResult) : AnalysisViewState()
    data class Error(val message: String) : AnalysisViewState()
}
