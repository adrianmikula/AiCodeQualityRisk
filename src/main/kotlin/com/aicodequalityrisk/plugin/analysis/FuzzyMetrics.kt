package com.aicodequalityrisk.plugin.analysis

data class MethodSimilarityPair(
    val firstMethod: String,
    val secondMethod: String,
    val similarity: Double
)

data class FuzzyMetrics(
    val duplicateMethodCount: Int = 0,
    val maxSimilarityScore: Double = 0.0,
    val duplicateMethodPairs: List<MethodSimilarityPair> = emptyList()
) {
    val hasDuplicateMethodBodies: Boolean = duplicateMethodCount > 0
}
