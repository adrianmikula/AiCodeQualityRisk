package com.aicodequalityrisk.plugin.analysis

data class CorruptedSourceMetrics(
    val parseFailed: Boolean = false,
    val markdownTokenCount: Int = 0,
    val xmlFragmentCount: Int = 0,
    val unbalancedBraceCount: Int = 0,
    val mixedLanguageDensity: Double = 0.0,
    val hasCorruptedContent: Boolean = false
)
