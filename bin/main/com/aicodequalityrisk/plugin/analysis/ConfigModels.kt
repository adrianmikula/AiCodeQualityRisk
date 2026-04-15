package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.model.Finding
import com.aicodequalityrisk.plugin.model.Severity

data class AnalysisConfig(
    val rules: List<RuleConfig>
)

data class RuleConfig(
    val name: String,
    val pattern: PatternConfig,
    val scoreDelta: Int,
    val category: String,
    val finding: FindingConfig
)

data class PatternConfig(
    val type: String,
    val target: String? = null,
    val value: String? = null,
    val conditions: List<ConditionConfig>? = null
)

data class ConditionConfig(
    val type: String,
    val target: String,
    val value: Any? = null,
    val operator: String? = null
)

data class FindingConfig(
    val title: String,
    val detail: String,
    val severity: String
)

fun FindingConfig.toFinding(): Finding {
    val severityEnum = when (severity.uppercase()) {
        "LOW" -> Severity.LOW
        "MEDIUM" -> Severity.MEDIUM
        "HIGH" -> Severity.HIGH
        else -> Severity.LOW
    }
    return Finding(title, detail, severityEnum)
}