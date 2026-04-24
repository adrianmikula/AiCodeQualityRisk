# Adaptive Similarity Thresholds Implementation Guide

## Overview

This guide provides detailed implementation instructions for adaptive similarity thresholds to replace the fixed 0.62 threshold in TreeSitterFuzzyDetector. Adaptive thresholds significantly improve detection accuracy by considering code context, complexity, and project patterns.

## Problem Statement

The current fixed threshold of 0.62 creates issues:
- **False positives** for simple, similar code patterns
- **False negatives** for complex, dissimilar code
- **No context awareness** for different coding styles
- **Rigid detection** across diverse codebases

## Solution Architecture

### Adaptive Threshold Factors

1. **Method Length Adjustment**
   - Short methods (< 10 lines): Lower threshold (0.47)
   - Medium methods (10-50 lines): Base threshold (0.62)
   - Long methods (> 50 lines): Higher threshold (0.72)

2. **Complexity Adjustment**
   - Simple code (complexity < 3): Lower threshold (0.52)
   - Medium complexity (3-10): Base threshold (0.62)
   - Complex code (> 10): Higher threshold (0.77)

3. **Project Baseline Adjustment**
   - Codebase similarity patterns
   - Language-specific conventions
   - Team coding standards

## Implementation Details

### 1. Enhanced MethodFingerprint Class

```kotlin
data class EnhancedMethodFingerprint(
    val name: String,
    val shingles: Set<String>,
    val methodLength: Int,
    val complexity: Int,
    val tokenCount: Int,
    val uniqueTokenCount: Int
) {
    val density: Double = if (tokenCount > 0) uniqueTokenCount.toDouble() / tokenCount else 0.0
}
```

### 2. Adaptive Threshold Calculator

```kotlin
class AdaptiveThresholdCalculator {
    private val baseThreshold = 0.62
    private val projectBaseline = mutableMapOf<String, Double>()
    
    fun calculateThreshold(
        fingerprint1: EnhancedMethodFingerprint,
        fingerprint2: EnhancedMethodFingerprint,
        filePath: String?
    ): Double {
        val lengthAdjustment = calculateLengthAdjustment(fingerprint1, fingerprint2)
        val complexityAdjustment = calculateComplexityAdjustment(fingerprint1, fingerprint2)
        val projectAdjustment = calculateProjectAdjustment(filePath)
        
        val adaptiveThreshold = baseThreshold + lengthAdjustment + complexityAdjustment + projectAdjustment
        
        return adaptiveThreshold.coerceIn(0.4, 0.85)
    }
    
    private fun calculateLengthAdjustment(
        fp1: EnhancedMethodFingerprint,
        fp2: EnhancedMethodFingerprint
    ): Double {
        val avgLength = (fp1.methodLength + fp2.methodLength) / 2.0
        
        return when {
            avgLength < 10 -> -0.15  // Lower threshold for short methods
            avgLength > 50 -> +0.10  // Higher threshold for long methods
            else -> 0.0
        }
    }
    
    private fun calculateComplexityAdjustment(
        fp1: EnhancedMethodFingerprint,
        fp2: EnhancedMethodFingerprint
    ): Double {
        val avgComplexity = (fp1.complexity + fp2.complexity) / 2.0
        
        return when {
            avgComplexity < 3 -> -0.10     // Lower for simple code
            avgComplexity > 10 -> +0.15     // Higher for complex code
            else -> 0.0
        }
    }
    
    private fun calculateProjectAdjustment(filePath: String?): Double {
        val projectKey = extractProjectKey(filePath)
        return projectBaseline.getOrDefault(projectKey, 0.0)
    }
    
    private fun extractProjectKey(filePath: String?): String {
        return filePath?.substringBeforeLast("/")?.substringBeforeLast("/") ?: "default"
    }
    
    fun updateProjectBaseline(projectKey: String, baseline: Double) {
        projectBaseline[projectKey] = baseline
    }
}
```

### 3. Enhanced TreeSitterFuzzyDetector

```kotlin
class TreeSitterFuzzyDetector {
    private val logger = Logger.getInstance(TreeSitterFuzzyDetector::class.java)
    private val parser = TSParser().apply { setLanguage(TreeSitterJava()) }
    private val thresholdCalculator = AdaptiveThresholdCalculator()
    
    fun detect(code: String, filePath: String?): FuzzyMetrics {
        logger.debug("Tree-sitter fuzzy detect invoked for $filePath")
        if (!isSupported(filePath)) return FuzzyMetrics()
        
        return try {
            val tree = parser.parseString(null, code)
            val metrics = analyzeTreeWithAdaptiveThresholds(tree.rootNode, code, filePath)
            if (metrics.duplicateMethodCount > 0) {
                logger.debug("Fuzzy detector found ${metrics.duplicateMethodCount} duplicate method pairs for $filePath")
            }
            metrics
        } catch (error: Throwable) {
            logger.warn("Tree-sitter parse failed for $filePath", error)
            FuzzyMetrics()
        }
    }
    
    private fun analyzeTreeWithAdaptiveThresholds(
        root: TSNode, 
        source: String, 
        filePath: String?
    ): FuzzyMetrics {
        val methods = collectMethodNodes(root)
        if (methods.size < 2) return FuzzyMetrics()
        
        val enhancedFingerprints = methods.map { node ->
            createEnhancedFingerprint(node, source)
        }
        
        val similarPairs = enhancedFingerprints.flatMapIndexed { index, fingerprint ->
            (index + 1 until enhancedFingerprints.size).mapNotNull { otherIndex ->
                val other = enhancedFingerprints[otherIndex]
                val adaptiveThreshold = thresholdCalculator.calculateThreshold(
                    fingerprint, other, filePath
                )
                val similarity = jaccardSimilarity(fingerprint.shingles, other.shingles)
                
                if (similarity >= adaptiveThreshold) {
                    MethodSimilarityPair(
                        firstMethod = fingerprint.name,
                        secondMethod = other.name,
                        similarity = similarity,
                        threshold = adaptiveThreshold
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
            adaptiveThresholdsEnabled = true
        )
    }
    
    private fun createEnhancedFingerprint(node: TSNode, source: String): EnhancedMethodFingerprint {
        val name = extractMethodName(node, source)
        val tokens = normalizeNodeTokens(node, source)
        val shingles = buildShingles(tokens)
        val methodLength = calculateMethodLength(node, source)
        val complexity = calculateMethodComplexity(node, source)
        
        return EnhancedMethodFingerprint(
            name = name,
            shingles = shingles,
            methodLength = methodLength,
            complexity = complexity,
            tokenCount = tokens.size,
            uniqueTokenCount = tokens.toSet().size
        )
    }
    
    private fun calculateMethodLength(node: TSNode, source: String): Int {
        val methodText = extractText(node, source)
        return methodText.lines().count { it.isNotBlank() }
    }
    
    private fun calculateMethodComplexity(node: TSNode, source: String): Int {
        // Simplified cyclomatic complexity
        val methodText = extractText(node, source)
        var complexity = 1
        
        complexity += methodText.count { it == 'i' && methodText.contains("if") } // if statements
        complexity += methodText.count { it == 'f' && methodText.contains("for") } // for loops
        complexity += methodText.count { it == 'w' && methodText.contains("while") } // while loops
        complexity += methodText.count { it == 'c' && methodText.contains("catch") } // catch blocks
        
        return complexity
    }
}
```

### 4. Enhanced FuzzyMetrics

```kotlin
data class MethodSimilarityPair(
    val firstMethod: String,
    val secondMethod: String,
    val similarity: Double,
    val threshold: Double = 0.62
)

data class FuzzyMetrics(
    val duplicateMethodCount: Int = 0,
    val maxSimilarityScore: Double = 0.0,
    val duplicateMethodPairs: List<MethodSimilarityPair> = emptyList(),
    val adaptiveThresholdsEnabled: Boolean = false
) {
    val hasDuplicateMethodBodies: Boolean = duplicateMethodCount > 0
    
    fun getAverageThreshold(): Double {
        return if (duplicateMethodPairs.isNotEmpty()) {
            duplicateMethodPairs.map { it.threshold }.average()
        } else 0.62
    }
}
```

## Migration Strategy

### Phase 1: Parallel Implementation
1. **Add new classes** without removing existing code
2. **Implement feature flag** for adaptive thresholds
3. **Run A/B testing** with both systems
4. **Compare results** and validate improvements

### Phase 2: Gradual Rollout
1. **Enable adaptive thresholds** for new projects
2. **Monitor performance** and accuracy
3. **Collect user feedback** on detection quality
4. **Fine-tune parameters** based on results

### Phase 3: Full Migration
1. **Replace fixed threshold** as default
2. **Maintain backward compatibility** option
3. **Update documentation** and examples
4. **Remove legacy code** after stabilization

## Testing Strategy

### Unit Tests
```kotlin
class AdaptiveThresholdCalculatorTest {
    private val calculator = AdaptiveThresholdCalculator()
    
    @Test
    fun `calculates lower threshold for short simple methods`() {
        val shortSimple = EnhancedMethodFingerprint(
            name = "shortMethod",
            shingles = setOf("CALL RETURN"),
            methodLength = 5,
            complexity = 2,
            tokenCount = 10,
            uniqueTokenCount = 8
        )
        
        val threshold = calculator.calculateThreshold(shortSimple, shortSimple, null)
        
        assertTrue(threshold < 0.62, "Expected lower threshold for short simple method")
        assertTrue(threshold >= 0.4, "Threshold should not go below minimum")
    }
    
    @Test
    fun `calculates higher threshold for long complex methods`() {
        val longComplex = EnhancedMethodFingerprint(
            name = "complexMethod",
            shingles = setOf("IF CALL CALL IF CALL RETURN"),
            methodLength = 60,
            complexity = 12,
            tokenCount = 100,
            uniqueTokenCount = 40
        )
        
        val threshold = calculator.calculateThreshold(longComplex, longComplex, null)
        
        assertTrue(threshold > 0.62, "Expected higher threshold for long complex method")
        assertTrue(threshold <= 0.85, "Threshold should not exceed maximum")
    }
}
```

### Integration Tests
```kotlin
class AdaptiveThresholdsIntegrationTest {
    private val detector = TreeSitterFuzzyDetector()
    
    @Test
    fun `adaptive thresholds reduce false positives`() {
        val simpleSimilarCode = """
            public class Example {
                public void methodA() { return true; }
                public void methodB() { return false; }
            }
        """.trimIndent()
        
        val metrics = detector.detect(simpleSimilarCode, "/tmp/Example.java")
        
        // Should not flag simple methods as duplicates
        assertTrue(metrics.duplicateMethodCount == 0, "Should not flag simple similar methods")
    }
    
    @Test
    fun `adaptive thresholds catch complex duplicates`() {
        val complexDuplicateCode = """
            public class Example {
                public void processData(List<String> data) {
                    if (data != null && !data.isEmpty()) {
                        for (String item : data) {
                            if (item != null && item.length() > 0) {
                                System.out.println(item.trim());
                            }
                        }
                    }
                }
                
                public void handleItems(List<String> items) {
                    if (items != null && !items.isEmpty()) {
                        for (String element : items) {
                            if (element != null && element.length() > 0) {
                                System.out.println(element.trim());
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val metrics = detector.detect(complexDuplicateCode, "/tmp/Example.java")
        
        // Should flag complex duplicate logic
        assertTrue(metrics.duplicateMethodCount >= 1, "Should catch complex duplicate methods")
    }
}
```

## Performance Considerations

### Optimization Strategies
1. **Cache threshold calculations** for similar method pairs
2. **Pre-compute complexity** metrics during parsing
3. **Batch process** method pairs for efficiency
4. **Use primitive types** for calculations

### Memory Management
1. **Limit fingerprint cache size** to prevent memory leaks
2. **Use weak references** for project baselines
3. **Clean up old data** periodically
4. **Monitor memory usage** in production

## Monitoring and Metrics

### Key Performance Indicators
- **False positive rate**: Should decrease by 30-40%
- **False negative rate**: Should decrease by 25-35%
- **Processing time**: Should remain <50ms
- **Memory usage**: Should remain <50MB

### Monitoring Implementation
```kotlin
class AdaptiveThresholdMetrics {
    private val falsePositiveCount = AtomicInteger(0)
    private val falseNegativeCount = AtomicInteger(0)
    private val totalDetections = AtomicInteger(0)
    
    fun recordDetection(isCorrect: Boolean) {
        totalDetections.incrementAndGet()
        if (!isCorrect) {
            // Track user feedback to determine false positive/negative
            falsePositiveCount.incrementAndGet()
        }
    }
    
    fun getAccuracyMetrics(): AccuracyMetrics {
        val total = totalDetections.get()
        return AccuracyMetrics(
            falsePositiveRate = if (total > 0) falsePositiveCount.get().toDouble() / total else 0.0,
            falseNegativeRate = 0.0, // Would need user feedback
            totalDetections = total
        )
    }
}

data class AccuracyMetrics(
    val falsePositiveRate: Double,
    val falseNegativeRate: Double,
    val totalDetections: Int
)
```

## Troubleshooting

### Common Issues
1. **Threshold too low**: Increase base threshold or adjustment factors
2. **Threshold too high**: Decrease base threshold or adjustment factors
3. **Performance degradation**: Add caching or optimize calculations
4. **Memory leaks**: Implement proper cleanup and weak references

### Debug Tools
```kotlin
class AdaptiveThresholdDebugger {
    fun debugThresholdCalculation(
        fp1: EnhancedMethodFingerprint,
        fp2: EnhancedMethodFingerprint,
        filePath: String?
    ): DebugInfo {
        val calculator = AdaptiveThresholdCalculator()
        
        return DebugInfo(
            method1Name = fp1.name,
            method2Name = fp2.name,
            method1Length = fp1.methodLength,
            method2Length = fp2.methodLength,
            method1Complexity = fp1.complexity,
            method2Complexity = fp2.complexity,
            baseThreshold = 0.62,
            lengthAdjustment = calculator.calculateLengthAdjustment(fp1, fp2),
            complexityAdjustment = calculator.calculateComplexityAdjustment(fp1, fp2),
            projectAdjustment = calculator.calculateProjectAdjustment(filePath),
            finalThreshold = calculator.calculateThreshold(fp1, fp2, filePath)
        )
    }
}

data class DebugInfo(
    val method1Name: String,
    val method2Name: String,
    val method1Length: Int,
    val method2Length: Int,
    val method1Complexity: Int,
    val method2Complexity: Int,
    val baseThreshold: Double,
    val lengthAdjustment: Double,
    val complexityAdjustment: Double,
    val projectAdjustment: Double,
    val finalThreshold: Double
)
```

This implementation guide provides comprehensive instructions for implementing adaptive similarity thresholds that will significantly improve the accuracy and flexibility of AI-generated code detection while maintaining performance requirements.
