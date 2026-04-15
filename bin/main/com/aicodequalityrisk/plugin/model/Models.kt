package com.aicodequalityrisk.plugin.model

import com.aicodequalityrisk.plugin.analysis.ASTMetrics
import com.aicodequalityrisk.plugin.analysis.Category
import com.aicodequalityrisk.plugin.analysis.FuzzyMetrics

data class AnalysisInput(
    val projectPath: String,
    val filePath: String?,
    val trigger: TriggerType,
    val diffText: String,
    val fileSnapshot: String,
    val astMetrics: ASTMetrics = ASTMetrics(),
    val fuzzyMetrics: FuzzyMetrics = FuzzyMetrics()
)

enum class TriggerType {
    EDIT,
    SAVE,
    FOCUS,
    MANUAL
}

data class RiskResult(
    val score: Int,
    val complexityScore: Int = 0,
    val duplicationScore: Int = 0,
    val performanceScore: Int = 0,
    val securityScore: Int = 0,
    val boilerplateBloatScore: Int = 0,
    val verboseCommentSpamScore: Int = 0,
    val overDefensiveProgrammingScore: Int = 0,
    val magicNumbersScore: Int = 0,
    val complexBooleanLogicScore: Int = 0,
    val deepNestingScore: Int = 0,
    val verboseLoggingScore: Int = 0,
    val poorNamingScore: Int = 0,
    val frameworkMisuseScore: Int = 0,
    val excessiveDocumentationScore: Int = 0,
    val findings: List<Finding> = emptyList(),
    val explanations: List<String> = emptyList(),
    val sourceFilePath: String? = null
)

data class Finding(
    val title: String,
    val detail: String,
    val severity: Severity,
    val category: Category = Category.COMPLEXITY,
    val filePath: String? = null,
    val lineNumber: Int? = null
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
