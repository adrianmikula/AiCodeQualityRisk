package com.aicodequalityrisk.plugin.analysis

data class MethodSimilarityPair(
    val firstMethod: String,
    val secondMethod: String,
    val similarity: Double,
    val threshold: Double = 0.62,
    val shingleBreakdown: ShingleBreakdown? = null,
    val structuralSimilarity: Double = 0.0,
    val treeEditDistance: Double = 0.0
)

data class FuzzyMetrics(
    val duplicateMethodCount: Int = 0,
    val maxSimilarityScore: Double = 0.0,
    val duplicateMethodPairs: List<MethodSimilarityPair> = emptyList(),
    val adaptiveThresholdsEnabled: Boolean = false,
    val multiGranularShinglingEnabled: Boolean = false,
    val entropyScoresEnabled: Boolean = false,
    val boilerplateBloatScore: Double = 0.0,
    val verboseCommentScore: Double = 0.0,
    val overDefensiveScore: Double = 0.0,
    val poorNamingScore: Double = 0.0,
    val frameworkMisuseScore: Double = 0.0,
    val excessiveDocumentationScore: Double = 0.0,
    val llmRepetitionIntensity: Double = 0.0,
    val astBasedSimilarityEnabled: Boolean = false
) {
    val hasDuplicateMethodBodies: Boolean = duplicateMethodCount > 0

    fun getAverageThreshold(): Double {
        return if (duplicateMethodPairs.isNotEmpty()) {
            duplicateMethodPairs.map { it.threshold }.average()
        } else 0.62
    }

    fun getAverageShingleSize(): Double {
        return if (duplicateMethodPairs.isNotEmpty()) {
            duplicateMethodPairs
                .mapNotNull { it.shingleBreakdown?.getDominantPattern() }
                .average()
        } else 4.0
    }
}
