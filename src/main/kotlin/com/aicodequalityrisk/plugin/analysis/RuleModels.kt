package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.Finding

data class Rule(
    val matches: (AnalysisInput) -> Boolean,
    val scoreDelta: Int,
    val category: Category,
    val finding: Finding
)

enum class Category {
    COMPLEXITY, DUPLICATION, PERFORMANCE, SECURITY
}