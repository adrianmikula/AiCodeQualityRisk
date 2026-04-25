package com.aicodequalityrisk.plugin.analysis

class MultiGranularShingleBuilder {
    private val shingleSizes = listOf(2, 4, 6, 8)
    private val weights = mapOf(2 to 0.2, 4 to 0.4, 6 to 0.3, 8 to 0.1)

    fun buildMultiGranularShingles(tokens: List<String>): Map<Int, Set<String>> {
        return shingleSizes.associateWith { size ->
            buildShingles(tokens, size)
        }
    }

    fun buildShingles(tokens: List<String>, size: Int): Set<String> {
        if (tokens.size < size) return tokens.toSet()
        return tokens.windowed(size) { it.joinToString(" ") }.toSet()
    }

    fun calculateWeightedSimilarity(
        firstShingles: Map<Int, Set<String>>,
        secondShingles: Map<Int, Set<String>>
    ): Double {
        return weights.map { (size, weight) ->
            val similarity = jaccardSimilarity(
                firstShingles[size] ?: emptySet(),
                secondShingles[size] ?: emptySet()
            )
            similarity * weight
        }.sum()
    }

    private fun jaccardSimilarity(first: Set<String>, second: Set<String>): Double {
        if (first.isEmpty() && second.isEmpty()) return 1.0
        val intersection = first intersect second
        val union = first union second
        return if (union.isEmpty()) 0.0 else intersection.size.toDouble() / union.size.toDouble()
    }

    fun getWeights(): Map<Int, Double> = weights
}
