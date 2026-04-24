# Multi-Granular Shingling Implementation Guide

## Overview

This guide provides detailed implementation instructions for multi-granular shingling to replace the fixed 4-token shingle approach in TreeSitterFuzzyDetector. Multi-granular shingling significantly improves pattern detection by analyzing code similarity at different scales.

## Problem Statement

The current fixed 4-token shingle approach has limitations:
- **Misses small patterns** that are shorter than 4 tokens
- **Misses large structural patterns** that span more than 4 tokens
- **Rigid granularity** doesn't adapt to different code contexts
- **Limited pattern coverage** for various code styles

## Solution Architecture

### Multi-Granular Approach

1. **Fine-grained shingles** (2 tokens): Catch small, similar patterns
2. **Standard shingles** (4 tokens): Maintain current baseline
3. **Coarse-grained shingles** (6 tokens): Capture medium patterns
4. **Structural shingles** (8 tokens): Detect large structural similarities

### Weighted Similarity Scoring

- **2-token shingles**: 20% weight (fine patterns)
- **4-token shingles**: 40% weight (standard patterns)
- **6-token shingles**: 30% weight (medium patterns)
- **8-token shingles**: 10% weight (structural patterns)

## Implementation Details

### 1. Enhanced Shingle Builder

```kotlin
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
}
```

### 2. Enhanced Method Fingerprint

```kotlin
data class MultiGranularMethodFingerprint(
    val name: String,
    val shingles: Map<Int, Set<String>>,
    val tokenCount: Int,
    val uniqueTokenCount: Int,
    val shingleStats: ShingleStats
) {
    val density: Double = if (tokenCount > 0) uniqueTokenCount.toDouble() / tokenCount else 0.0
    
    fun getSimilarityScore(other: MultiGranularMethodFingerprint): Double {
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
```

### 3. Enhanced TreeSitterFuzzyDetector

```kotlin
class TreeSitterFuzzyDetector {
    private val logger = Logger.getInstance(TreeSitterFuzzyDetector::class.java)
    private val parser = TSParser().apply { setLanguage(TreeSitterJava()) }
    private val shingleBuilder = MultiGranularShingleBuilder()
    
    fun detect(code: String, filePath: String?): FuzzyMetrics {
        logger.debug("Tree-sitter fuzzy detect invoked for $filePath")
        if (!isSupported(filePath)) return FuzzyMetrics()
        
        return try {
            val tree = parser.parseString(null, code)
            val metrics = analyzeTreeWithMultiGranularShingling(tree.rootNode, code, filePath)
            if (metrics.duplicateMethodCount > 0) {
                logger.debug("Fuzzy detector found ${metrics.duplicateMethodCount} duplicate method pairs for $filePath")
            }
            metrics
        } catch (error: Throwable) {
            logger.warn("Tree-sitter parse failed for $filePath", error)
            FuzzyMetrics()
        }
    }
    
    private fun analyzeTreeWithMultiGranularShingling(
        root: TSNode, 
        source: String, 
        filePath: String?
    ): FuzzyMetrics {
        val methods = collectMethodNodes(root)
        if (methods.size < 2) return FuzzyMetrics()
        
        val multiGranularFingerprints = methods.map { node ->
            createMultiGranularFingerprint(node, source)
        }
        
        val similarPairs = multiGranularFingerprints.flatMapIndexed { index, fingerprint ->
            (index + 1 until multiGranularFingerprints.size).mapNotNull { otherIndex ->
                val other = multiGranularFingerprints[otherIndex]
                val similarity = fingerprint.getSimilarityScore(other)
                
                if (similarity >= DUPLICATE_THRESHOLD) {
                    MethodSimilarityPair(
                        firstMethod = fingerprint.name,
                        secondMethod = other.name,
                        similarity = similarity,
                        shingleBreakdown = createShingleBreakdown(fingerprint, other)
                    )
                } else {
                    null
                }
            }
        }
        
        return FuzzyMetrics(
            duplicateMethodCount = similarPairs.size,
            maxSimilarityScore = similarPairs.maxOfOrNull { it.similarity } ?: 0.0,
            duplicateMethodPairs = similarPairs,
            multiGranularShinglingEnabled = true
        )
    }
    
    private fun createMultiGranularFingerprint(node: TSNode, source: String): MultiGranularMethodFingerprint {
        val name = extractMethodName(node, source)
        val tokens = normalizeNodeTokens(node, source)
        val shingles = shingleBuilder.buildMultiGranularShingles(tokens)
        val shingleStats = calculateShingleStats(shingles)
        
        return MultiGranularMethodFingerprint(
            name = name,
            shingles = shingles,
            tokenCount = tokens.size,
            uniqueTokenCount = tokens.toSet().size,
            shingleStats = shingleStats
        )
    }
    
    private fun calculateShingleStats(shingles: Map<Int, Set<String>>): ShingleStats {
        val totalShingles = shingles.values.sumOf { it.size }
        val uniqueShingles = shingles.values.flatten().toSet().size
        val shingleDensity = if (totalShingles > 0) uniqueShingles.toDouble() / totalShingles else 0.0
        val dominantShingleSize = shingles.maxByOrNull { it.value.size }?.key ?: 4
        
        return ShingleStats(
            totalShingles = totalShingles,
            uniqueShingles = uniqueShingles,
            shingleDensity = shingleDensity,
            dominantShingleSize = dominantShingleSize
        )
    }
    
    private fun createShingleBreakdown(
        fp1: MultiGranularMethodFingerprint,
        fp2: MultiGranularMethodFingerprint
    ): ShingleBreakdown {
        val breakdown = mutableMapOf<Int, Double>()
        
        for (size in listOf(2, 4, 6, 8)) {
            val similarity = jaccardSimilarity(
                fp1.shingles[size] ?: emptySet(),
                fp2.shingles[size] ?: emptySet()
            )
            breakdown[size] = similarity
        }
        
        return ShingleBreakdown(breakdown)
    }
}
```

### 4. Enhanced Data Classes

```kotlin
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

data class MethodSimilarityPair(
    val firstMethod: String,
    val secondMethod: String,
    val similarity: Double,
    val threshold: Double = 0.62,
    val shingleBreakdown: ShingleBreakdown? = null
)

data class FuzzyMetrics(
    val duplicateMethodCount: Int = 0,
    val maxSimilarityScore: Double = 0.0,
    val duplicateMethodPairs: List<MethodSimilarityPair> = emptyList(),
    val multiGranularShinglingEnabled: Boolean = false
) {
    val hasDuplicateMethodBodies: Boolean = duplicateMethodCount > 0
    
    fun getAverageShingleSize(): Double {
        return if (duplicateMethodPairs.isNotEmpty()) {
            duplicateMethodPairs
                .mapNotNull { it.shingleBreakdown?.getDominantPattern() }
                .average()
        } else 4.0
    }
}
```

## Advanced Features

### 1. Adaptive Shingle Weights

```kotlin
class AdaptiveShingleWeights {
    private val baseWeights = mapOf(2 to 0.2, 4 to 0.4, 6 to 0.3, 8 to 0.1)
    private val contextWeights = mutableMapOf<String, Map<Int, Double>>()
    
    fun getWeights(context: String? = null): Map<Int, Double> {
        return context?.let { contextWeights[it] } ?: baseWeights
    }
    
    fun updateWeights(context: String, weights: Map<Int, Double>) {
        contextWeights[context] = weights
    }
    
    fun adaptWeightsBasedOnFeedback(feedback: List<ShingleFeedback>) {
        val performanceBySize = feedback.groupBy { it.shingleSize }
            .mapValues { (_, entries) ->
                entries.map { it.accuracy }.average()
            }
        
        val totalPerformance = performanceBySize.values.average()
        val adaptedWeights = performanceBySize.mapValues { (size, performance) ->
            val baseWeight = baseWeights[size] ?: 0.0
            val adjustment = (performance - totalPerformance) * 0.1
            (baseWeight + adjustment).coerceIn(0.05, 0.6)
        }
        
        // Normalize weights to sum to 1.0
        val totalWeight = adaptedWeights.values.sum()
        return adaptedWeights.mapValues { (_, weight) -> weight / totalWeight }
    }
}

data class ShingleFeedback(
    val shingleSize: Int,
    val accuracy: Double,
    val context: String
)
```

### 2. Shingle Quality Assessment

```kotlin
class ShingleQualityAssessor {
    fun assessShingleQuality(shingles: Map<Int, Set<String>>): ShingleQuality {
        val qualityMetrics = mutableMapOf<Int, ShingleQualityMetric>()
        
        for ((size, shingleSet) in shingles) {
            val metric = assessShingleSet(shingleSet, size)
            qualityMetrics[size] = metric
        }
        
        return ShingleQuality(
            overallScore = qualityMetrics.values.map { it.score }.average(),
            metricsBySize = qualityMetrics,
            recommendedWeights = calculateRecommendedWeights(qualityMetrics)
        )
    }
    
    private fun assessShingleSet(shingles: Set<String>, size: Int): ShingleQualityMetric {
        val avgShingleLength = shingles.map { it.split(" ").size }.average()
        val uniqueTokens = shingles.flatMap { it.split(" ") }.toSet().size
        val redundancy = 1.0 - (uniqueTokens.toDouble() / (shingles.size * size))
        
        val score = when {
            redundancy < 0.1 -> 1.0  // Low redundancy is good
            redundancy < 0.3 -> 0.8
            redundancy < 0.5 -> 0.6
            else -> 0.4  // High redundancy is bad
        }
        
        return ShingleQualityMetric(
            size = size,
            score = score,
            redundancy = redundancy,
            uniqueTokenRatio = uniqueTokens.toDouble() / (shingles.size * size)
        )
    }
    
    private fun calculateRecommendedWeights(metrics: Map<Int, ShingleQualityMetric>): Map<Int, Double> {
        val totalScore = metrics.values.map { it.score }.sum()
        return metrics.mapValues { (_, metric) ->
            metric.score / totalScore
        }
    }
}

data class ShingleQuality(
    val overallScore: Double,
    val metricsBySize: Map<Int, ShingleQualityMetric>,
    val recommendedWeights: Map<Int, Double>
)

data class ShingleQualityMetric(
    val size: Int,
    val score: Double,
    val redundancy: Double,
    val uniqueTokenRatio: Double
)
```

## Testing Strategy

### Unit Tests

```kotlin
class MultiGranularShingleBuilderTest {
    private val builder = MultiGranularShingleBuilder()
    
    @Test
    fun `builds shingles for all sizes`() {
        val tokens = listOf("IF", "CALL", "RETURN", "ELSE", "CALL", "RETURN")
        val shingles = builder.buildMultiGranularShingles(tokens)
        
        assertEquals(4, shingles.size, "Should create shingles for 4 sizes")
        assertTrue(shingles.containsKey(2), "Should contain 2-token shingles")
        assertTrue(shingles.containsKey(4), "Should contain 4-token shingles")
        assertTrue(shingles.containsKey(6), "Should contain 6-token shingles")
        assertTrue(shingles.containsKey(8), "Should contain 8-token shingles")
    }
    
    @Test
    fun `calculates weighted similarity correctly`() {
        val shingles1 = mapOf(
            2 to setOf("IF CALL", "CALL RETURN"),
            4 to setOf("IF CALL RETURN ELSE")
        )
        val shingles2 = mapOf(
            2 to setOf("IF CALL", "CALL RETURN"),
            4 to setOf("IF CALL RETURN THEN")
        )
        
        val similarity = builder.calculateWeightedSimilarity(shingles1, shingles2)
        
        assertTrue(similarity > 0.0, "Should detect some similarity")
        assertTrue(similarity < 1.0, "Should not be identical")
    }
    
    @Test
    fun `handles short token lists`() {
        val tokens = listOf("CALL", "RETURN")
        val shingles = builder.buildMultiGranularShingles(tokens)
        
        assertEquals(2, shingles[2]?.size, "2-token shingles should work")
        assertTrue(shingles[4]?.size ?: 0 <= 2, "4-token shingles should be limited")
        assertTrue(shingles[6]?.size ?: 0 <= 2, "6-token shingles should be limited")
        assertTrue(shingles[8]?.size ?: 0 <= 2, "8-token shingles should be limited")
    }
}
```

### Integration Tests

```kotlin
class MultiGranularShinglingIntegrationTest {
    private val detector = TreeSitterFuzzyDetector()
    
    @Test
    fun `detects similar patterns at different granularities`() {
        val code = """
            public class Example {
                public void methodA() {
                    if (x > 0) {
                        System.out.println("positive");
                    }
                }
                
                public void methodB() {
                    if (y > 0) {
                        System.out.println("positive");
                    }
                }
            }
        """.trimIndent()
        
        val metrics = detector.detect(code, "/tmp/Example.java")
        
        assertTrue(metrics.duplicateMethodCount >= 1, "Should detect similar methods")
        assertTrue(metrics.multiGranularShinglingEnabled, "Should use multi-granular shingling")
    }
    
    @Test
    fun `provides detailed shingle breakdown`() {
        val code = """
            public class Example {
                public void similar1() { if (x > 0) { doWork(); } }
                public void similar2() { if (y > 0) { doWork(); } }
            }
        """.trimIndent()
        
        val metrics = detector.detect(code, "/tmp/Example.java")
        val firstPair = metrics.duplicateMethodPairs.firstOrNull()
        
        assertNotNull(firstPair, "Should have at least one similar pair")
        assertNotNull(firstPair?.shingleBreakdown, "Should have shingle breakdown")
        
        val breakdown = firstPair!!.shingleBreakdown!!
        assertTrue(breakdown.similarities.isNotEmpty(), "Should have similarities for different sizes")
    }
}
```

## Performance Optimization

### 1. Caching Strategy

```kotlin
class ShingleCache {
    private val cache = mutableMapOf<String, Map<Int, Set<String>>>()
    private val maxSize = 1000
    
    fun getOrCompute(tokens: List<String>, compute: () -> Map<Int, Set<String>>): Map<Int, Set<String>> {
        val key = tokens.joinToString("|")
        return cache.getOrPut(key) {
            if (cache.size >= maxSize) {
                cache.clear() // Simple LRU replacement
            }
            compute()
        }
    }
    
    fun clear() {
        cache.clear()
    }
}
```

### 2. Parallel Processing

```kotlin
class ParallelShingleProcessor {
    private val executor = ForkJoinPool.commonPool()
    
    fun processFingerprintsParallel(
        fingerprints: List<MultiGranularMethodFingerprint>
    ): List<MethodSimilarityPair> {
        val pairs = mutableListOf<MethodSimilarityPair>()
        
        // Process pairs in parallel
        val futures = (0 until fingerprints.size).flatMap { i ->
            (i + 1 until fingerprints.size).map { j ->
                executor.submit {
                    calculateSimilarity(fingerprints[i], fingerprints[j])
                }
            }
        }
        
        // Collect results
        futures.forEach { future ->
            future.get()?.let { pairs.add(it) }
        }
        
        return pairs
    }
    
    private fun calculateSimilarity(
        fp1: MultiGranularMethodFingerprint,
        fp2: MultiGranularMethodFingerprint
    ): MethodSimilarityPair? {
        val similarity = fp1.getSimilarityScore(fp2)
        return if (similarity >= DUPLICATE_THRESHOLD) {
            MethodSimilarityPair(
                firstMethod = fp1.name,
                secondMethod = fp2.name,
                similarity = similarity,
                shingleBreakdown = createShingleBreakdown(fp1, fp2)
            )
        } else null
    }
}
```

## Monitoring and Metrics

### Performance Metrics

```kotlin
class MultiGranularShingleMetrics {
    private val shingleProcessingTime = AtomicLong(0)
    private val similarityCalculationTime = AtomicLong(0)
    private val totalShinglesProcessed = AtomicLong(0)
    
    fun recordShingleProcessing(timeMs: Long, shingleCount: Int) {
        shingleProcessingTime.addAndGet(timeMs)
        totalShinglesProcessed.addAndGet(shingleCount.toLong())
    }
    
    fun recordSimilarityCalculation(timeMs: Long) {
        similarityCalculationTime.addAndGet(timeMs)
    }
    
    fun getMetrics(): ShinglePerformanceMetrics {
        return ShinglePerformanceMetrics(
            avgShingleProcessingTime = if (totalShinglesProcessed.get() > 0) {
                shingleProcessingTime.get().toDouble() / totalShinglesProcessed.get()
            } else 0.0,
            avgSimilarityCalculationTime = similarityCalculationTime.get().toDouble(),
            totalShinglesProcessed = totalShinglesProcessed.get()
        )
    }
}

data class ShinglePerformanceMetrics(
    val avgShingleProcessingTime: Double,
    val avgSimilarityCalculationTime: Double,
    val totalShinglesProcessed: Long
)
```

This implementation guide provides comprehensive instructions for implementing multi-granular shingling that will significantly improve pattern detection accuracy while maintaining performance requirements.
