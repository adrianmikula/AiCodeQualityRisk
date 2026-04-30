package com.aicodequalityrisk.plugin.analysis

/**
 * Calculates "LLM repetition intensity" - a metric measuring how much AI-generated
 * code exhibits repetitive patterns across the codebase.
 *
 * This is not about perfect clone detection, but about measuring the intensity
 * of structural repetition that indicates AI-generated boilerplate.
 */
class LLMRepetitionIntensity {
    
    /**
     * Calculates repetition intensity from similarity pairs.
     *
     * @param similarityPairs List of method similarity pairs with their scores
     * @param totalMethods Total number of methods in the codebase
     * @param threshold Minimum similarity to consider (default 0.5)
     * @return Intensity score from 0.0 (no repetition) to 100.0 (extreme repetition)
     */
    fun calculateIntensity(
        similarityPairs: List<MethodSimilarityPair>,
        totalMethods: Int,
        threshold: Double = 0.5
    ): Double {
        if (totalMethods < 2 || similarityPairs.isEmpty()) return 0.0
        
        // Filter pairs above threshold
        val significantPairs = similarityPairs.filter { it.similarity >= threshold }
        if (significantPairs.isEmpty()) return 0.0
        
        // Calculate metrics
        val repetitionCoverage = calculateRepetitionCoverage(significantPairs, totalMethods)
        val averageSimilarity = significantPairs.map { it.similarity }.average()
        val pairDensity = significantPairs.size.toDouble() / (totalMethods * (totalMethods - 1) / 2.0)
        
        // Weighted intensity score
        val intensity = (repetitionCoverage * 0.4) + (averageSimilarity * 0.4) + (pairDensity * 0.2)
        
        // Scale to 0-100
        return (intensity * 100).coerceIn(0.0, 100.0)
    }
    
    /**
     * Calculates the percentage of methods that are similar to at least N other methods.
     *
     * @param similarityPairs List of similarity pairs
     * @param totalMethods Total number of methods
     * @param minSimilarMethods Minimum number of similar methods to count (default 2)
     * @return Percentage 0.0-100.0
     */
    fun calculateRepetitionCoverage(
        similarityPairs: List<MethodSimilarityPair>,
        totalMethods: Int,
        minSimilarMethods: Int = 2
    ): Double {
        if (totalMethods == 0) return 0.0
        
        // Count how many methods each method is similar to
        val methodSimilarityCount = mutableMapOf<String, Int>()
        
        for (pair in similarityPairs) {
            methodSimilarityCount[pair.firstMethod] = (methodSimilarityCount[pair.firstMethod] ?: 0) + 1
            methodSimilarityCount[pair.secondMethod] = (methodSimilarityCount[pair.secondMethod] ?: 0) + 1
        }
        
        // Count methods with at least minSimilarMethods similar methods
        val repetitiveMethods = methodSimilarityCount.values.count { it >= minSimilarMethods }
        
        return (repetitiveMethods.toDouble() / totalMethods.toDouble()) * 100.0
    }
    
    /**
     * Calculates repetition patterns across classes/files.
     *
     * @param similarityPairs List of similarity pairs with method names
     * @param methodToClassMap Mapping from method name to class name
     * @return Map of class names to their repetition intensity
     */
    fun calculateClassLevelIntensity(
        similarityPairs: List<MethodSimilarityPair>,
        methodToClassMap: Map<String, String>
    ): Map<String, Double> {
        val classPairs = mutableMapOf<String, MutableList<MethodSimilarityPair>>()
        
        // Group pairs by class
        for (pair in similarityPairs) {
            val class1 = methodToClassMap[pair.firstMethod] ?: "unknown"
            val class2 = methodToClassMap[pair.secondMethod] ?: "unknown"
            
            // Add to both classes' pair lists
            classPairs.getOrPut(class1) { mutableListOf() }.add(pair)
            classPairs.getOrPut(class2) { mutableListOf() }.add(pair)
        }
        
        // Calculate intensity for each class
        val result = mutableMapOf<String, Double>()
        for ((className, pairs) in classPairs) {
            val methodsInClass = methodToClassMap.values.count { it == className }
            result[className] = calculateIntensity(pairs, methodsInClass)
        }
        
        return result
    }
    
    /**
     * Identifies repetition hotspots - methods that are most frequently duplicated.
     *
     * @param similarityPairs List of similarity pairs
     * @return List of method names sorted by repetition count (descending)
     */
    fun identifyRepetitionHotspots(
        similarityPairs: List<MethodSimilarityPair>
    ): List<RepetitionHotspot> {
        val methodSimilarityCount = mutableMapOf<String, Int>()
        val methodSimilaritySum = mutableMapOf<String, Double>()
        
        for (pair in similarityPairs) {
            methodSimilarityCount[pair.firstMethod] = (methodSimilarityCount[pair.firstMethod] ?: 0) + 1
            methodSimilarityCount[pair.secondMethod] = (methodSimilarityCount[pair.secondMethod] ?: 0) + 1
            
            methodSimilaritySum[pair.firstMethod] = (methodSimilaritySum[pair.firstMethod] ?: 0.0) + pair.similarity
            methodSimilaritySum[pair.secondMethod] = (methodSimilaritySum[pair.secondMethod] ?: 0.0) + pair.similarity
        }
        
        return methodSimilarityCount.map { (method, count) ->
            RepetitionHotspot(
                methodName = method,
                repetitionCount = count,
                averageSimilarity = (methodSimilaritySum[method] ?: 0.0) / count
            )
        }.sortedByDescending { it.repetitionCount }
    }
    
    /**
     * Calculates the intensity trend across iterations (for tracking degradation).
     *
     * @param intensityHistory List of intensity scores over time
     * @return Trend direction (INCREASING, DECREASING, STABLE) and rate of change
     */
    fun calculateIntensityTrend(intensityHistory: List<Double>): IntensityTrend {
        if (intensityHistory.size < 2) {
            return IntensityTrend(IntensityDirection.STABLE, 0.0)
        }
        
        val recent = intensityHistory.takeLast(5).average()
        val earlier = intensityHistory.dropLast(5).takeLast(5).ifEmpty { listOf(intensityHistory.first()) }.average()
        
        val change = recent - earlier
        val direction = when {
            change > 5.0 -> IntensityDirection.INCREASING
            change < -5.0 -> IntensityDirection.DECREASING
            else -> IntensityDirection.STABLE
        }
        
        return IntensityTrend(direction, change)
    }
}

data class RepetitionHotspot(
    val methodName: String,
    val repetitionCount: Int,
    val averageSimilarity: Double
)

enum class IntensityDirection {
    INCREASING,
    DECREASING,
    STABLE
}

data class IntensityTrend(
    val direction: IntensityDirection,
    val changeRate: Double
)
