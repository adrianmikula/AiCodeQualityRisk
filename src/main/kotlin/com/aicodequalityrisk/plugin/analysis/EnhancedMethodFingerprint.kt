package com.aicodequalityrisk.plugin.analysis

data class EnhancedMethodFingerprint(
    val name: String,
    val shingles: Map<Int, Set<String>>,
    val methodLength: Int,
    val complexity: Int,
    val tokenCount: Int,
    val uniqueTokenCount: Int
) {
    val density: Double = if (tokenCount > 0) uniqueTokenCount.toDouble() / tokenCount else 0.0

    fun getSimilarityScore(other: EnhancedMethodFingerprint): Double {
        return MultiGranularShingleBuilder().calculateWeightedSimilarity(
            this.shingles, other.shingles
        )
    }
}

data class ShingleStats(
    val totalShingles: Int,
    val uniqueShingles: Int,
    val shingleDensity: Double,
    val dominantShingleSize: Int
)

data class ShingleBreakdown(
    val similarities: Map<Int, Double>
) {
    fun getWeightedScore(): Double {
        val weights = mapOf(2 to 0.2, 4 to 0.4, 6 to 0.3, 8 to 0.1)
        return similarities.map { (size, similarity) ->
            similarity * (weights[size] ?: 0.0)
        }.sum()
    }

    fun getDominantPattern(): Int {
        return similarities.maxByOrNull { it.value }?.key ?: 4
    }
}
