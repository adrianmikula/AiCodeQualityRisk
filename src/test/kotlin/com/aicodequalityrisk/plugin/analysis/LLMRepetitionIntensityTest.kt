package com.aicodequalityrisk.plugin.analysis

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LLMRepetitionIntensityTest {
    private val calculator = LLMRepetitionIntensity()

    @Test
    fun `calculates zero intensity with no pairs`() {
        val intensity = calculator.calculateIntensity(emptyList(), 10)

        assertEquals(0.0, intensity, 0.001, "No pairs should result in zero intensity")
    }

    @Test
    fun `calculates zero intensity with single method`() {
        val pairs = listOf(
            MethodSimilarityPair("method1", "method2", 0.8)
        )
        val intensity = calculator.calculateIntensity(pairs, 1)

        assertEquals(0.0, intensity, 0.001, "Single method should result in zero intensity")
    }

    @Test
    fun `calculates low intensity with few similar pairs`() {
        val pairs = listOf(
            MethodSimilarityPair("method1", "method2", 0.6)
        )
        val intensity = calculator.calculateIntensity(pairs, 10)

        assertTrue(intensity > 0.0, "Should have some intensity")
        assertTrue(intensity < 40.0, "Low repetition should have low intensity")
    }

    @Test
    fun `calculates high intensity with many similar pairs`() {
        val pairs = listOf(
            MethodSimilarityPair("method1", "method2", 0.8),
            MethodSimilarityPair("method1", "method3", 0.8),
            MethodSimilarityPair("method2", "method3", 0.8),
            MethodSimilarityPair("method4", "method5", 0.8),
            MethodSimilarityPair("method4", "method6", 0.8)
        )
        val intensity = calculator.calculateIntensity(pairs, 6)

        assertTrue(intensity > 30.0, "High repetition should have high intensity")
    }

    @Test
    fun `calculates repetition coverage correctly`() {
        val pairs = listOf(
            MethodSimilarityPair("method1", "method2", 0.8),
            MethodSimilarityPair("method1", "method3", 0.8),
            MethodSimilarityPair("method4", "method5", 0.6)
        )
        
        val coverage = calculator.calculateRepetitionCoverage(pairs, 5, minSimilarMethods = 2)

        assertTrue(coverage > 0.0, "Should have some coverage")
        assertTrue(coverage <= 100.0, "Coverage should not exceed 100%")
    }

    @Test
    fun `identifies repetition hotspots correctly`() {
        val pairs = listOf(
            MethodSimilarityPair("method1", "method2", 0.8),
            MethodSimilarityPair("method1", "method3", 0.8),
            MethodSimilarityPair("method1", "method4", 0.8),
            MethodSimilarityPair("method2", "method5", 0.6)
        )
        
        val hotspots = calculator.identifyRepetitionHotspots(pairs)

        assertTrue(hotspots.isNotEmpty(), "Should identify hotspots")
        assertEquals("method1", hotspots[0].methodName, "method1 should be top hotspot")
        assertTrue(hotspots[0].repetitionCount >= 3, "method1 should have high repetition count")
    }

    @Test
    fun `calculates class level intensity correctly`() {
        val pairs = listOf(
            MethodSimilarityPair("method1", "method2", 0.8),
            MethodSimilarityPair("method3", "method4", 0.7)
        )
        val methodToClassMap = mapOf(
            "method1" to "ClassA",
            "method2" to "ClassA",
            "method3" to "ClassB",
            "method4" to "ClassB"
        )
        
        val classIntensity = calculator.calculateClassLevelIntensity(pairs, methodToClassMap)

        assertTrue(classIntensity.containsKey("ClassA"), "Should have intensity for ClassA")
        assertTrue(classIntensity.containsKey("ClassB"), "Should have intensity for ClassB")
    }

    @Test
    fun `calculates intensity trend for increasing repetition`() {
        val history = listOf(10.0, 20.0, 35.0, 50.0, 70.0)
        
        val trend = calculator.calculateIntensityTrend(history)

        assertEquals(IntensityDirection.INCREASING, trend.direction, "Should detect increasing trend")
        assertTrue(trend.changeRate > 0, "Change rate should be positive")
    }

    @Test
    fun `calculates intensity trend for decreasing repetition`() {
        val history = listOf(70.0, 50.0, 35.0, 20.0, 10.0)
        
        val trend = calculator.calculateIntensityTrend(history)

        assertEquals(IntensityDirection.DECREASING, trend.direction, "Should detect decreasing trend")
        assertTrue(trend.changeRate < 0, "Change rate should be negative")
    }

    @Test
    fun `calculates intensity trend for stable repetition`() {
        val history = listOf(40.0, 42.0, 38.0, 41.0, 39.0)
        
        val trend = calculator.calculateIntensityTrend(history)

        assertEquals(IntensityDirection.STABLE, trend.direction, "Should detect stable trend")
    }

    @Test
    fun `handles insufficient history for trend`() {
        val history = listOf(40.0)
        
        val trend = calculator.calculateIntensityTrend(history)

        assertEquals(IntensityDirection.STABLE, trend.direction, "Should be stable with insufficient data")
        assertEquals(0.0, trend.changeRate, 0.001, "Change rate should be zero")
    }
}
