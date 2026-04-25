package com.aicodequalityrisk.plugin.analysis

import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class AdaptiveThresholdCalculatorTest {
    private val calculator = AdaptiveThresholdCalculator()

    @Test
    fun `calculates lower threshold for short simple methods`() {
        val shortSimple = EnhancedMethodFingerprint(
            name = "shortMethod",
            shingles = mapOf(4 to setOf("CALL RETURN")),
            methodLength = 5,
            complexity = 2,
            tokenCount = 10,
            uniqueTokenCount = 8
        )

        val threshold = calculator.calculateThreshold(shortSimple, shortSimple, null)

        assertTrue(threshold < 0.62, "Expected lower threshold for short simple method, got $threshold")
        assertTrue(threshold >= 0.4, "Threshold should not go below minimum, got $threshold")
    }

    @Test
    fun `calculates higher threshold for long complex methods`() {
        val longComplex = EnhancedMethodFingerprint(
            name = "complexMethod",
            shingles = mapOf(4 to setOf("IF CALL CALL IF CALL RETURN")),
            methodLength = 60,
            complexity = 12,
            tokenCount = 100,
            uniqueTokenCount = 40
        )

        val threshold = calculator.calculateThreshold(longComplex, longComplex, null)

        assertTrue(threshold > 0.62, "Expected higher threshold for long complex method, got $threshold")
        assertTrue(threshold <= 0.85, "Threshold should not exceed maximum, got $threshold")
    }

    @Test
    fun `calculates base threshold for medium methods`() {
        val mediumMethod = EnhancedMethodFingerprint(
            name = "mediumMethod",
            shingles = mapOf(4 to setOf("IF CALL RETURN")),
            methodLength = 25,
            complexity = 5,
            tokenCount = 30,
            uniqueTokenCount = 20
        )

        val threshold = calculator.calculateThreshold(mediumMethod, mediumMethod, null)

        assertEquals(0.62, threshold, 0.001, "Expected base threshold for medium method")
    }

    @Test
    fun `combines length and complexity adjustments`() {
        val shortComplex = EnhancedMethodFingerprint(
            name = "shortComplex",
            shingles = mapOf(4 to setOf("IF IF CALL RETURN")),
            methodLength = 5,
            complexity = 12,
            tokenCount = 20,
            uniqueTokenCount = 15
        )

        val threshold = calculator.calculateThreshold(shortComplex, shortComplex, null)

        assertTrue(threshold >= 0.4, "Threshold should respect minimum bound")
        assertTrue(threshold <= 0.85, "Threshold should respect maximum bound")
    }

    @Test
    fun `threshold is within valid range`() {
        val testCases = listOf(
            EnhancedMethodFingerprint("a", mapOf(), 1, 1, 5, 5),
            EnhancedMethodFingerprint("b", mapOf(), 100, 20, 200, 50),
            EnhancedMethodFingerprint("c", mapOf(), 25, 5, 50, 25),
            EnhancedMethodFingerprint("d", mapOf(), 5, 15, 10, 8)
        )

        for (fp1 in testCases) {
            for (fp2 in testCases) {
                val threshold = calculator.calculateThreshold(fp1, fp2, "/test/file.java")
                assertTrue(threshold in 0.4..0.85,
                    "Threshold $threshold out of bounds for ${fp1.name} vs ${fp2.name}")
            }
        }
    }

    @Test
    fun `project baseline can be updated and used`() {
        val method = EnhancedMethodFingerprint(
            name = "testMethod",
            shingles = mapOf(4 to setOf("CALL RETURN")),
            methodLength = 20,
            complexity = 5,
            tokenCount = 20,
            uniqueTokenCount = 15
        )

        val baselineBefore = calculator.calculateThreshold(method, method, "/project1/file.java")

        calculator.updateProjectBaseline("", 0.05)

        val baselineAfter = calculator.calculateThreshold(method, method, "/project1/file.java")

        assertTrue(baselineAfter > baselineBefore,
            "Project baseline adjustment should increase threshold")
    }
}
