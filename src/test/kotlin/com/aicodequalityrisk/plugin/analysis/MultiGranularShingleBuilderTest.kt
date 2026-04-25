package com.aicodequalityrisk.plugin.analysis

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            4 to setOf("IF CALL RETURN ELSE"),
            6 to setOf("IF CALL RETURN ELSE CALL"),
            8 to setOf("IF CALL RETURN ELSE CALL RETURN")
        )
        val shingles2 = mapOf(
            2 to setOf("IF CALL", "CALL RETURN"),
            4 to setOf("IF CALL RETURN THEN"),
            6 to setOf("IF CALL RETURN THEN CALL"),
            8 to setOf("IF CALL RETURN THEN CALL RETURN")
        )

        val similarity = builder.calculateWeightedSimilarity(shingles1, shingles2)

        assertTrue(similarity > 0.0, "Should detect some similarity")
        assertTrue(similarity < 1.0, "Should not be identical")
        assertTrue(similarity >= 0.2, "2-token shingles should contribute at least 20% (got $similarity)")
    }

    @Test
    fun `handles short token lists`() {
        val tokens = listOf("CALL", "RETURN")
        val shingles = builder.buildMultiGranularShingles(tokens)

        assertEquals(1, shingles[2]?.size, "2-token shingles for 2 tokens should have 1 window")
        assertTrue(shingles[4]?.size ?: 0 <= 2, "4-token shingles should be limited")
        assertTrue(shingles[6]?.size ?: 0 <= 2, "6-token shingles should be limited")
        assertTrue(shingles[8]?.size ?: 0 <= 2, "8-token shingles should be limited")
    }

    @Test
    fun `identical shingles have perfect similarity`() {
        val tokens = listOf("IF", "CALL", "RETURN", "ELSE", "CALL", "RETURN")
        val shingles = builder.buildMultiGranularShingles(tokens)

        val similarity = builder.calculateWeightedSimilarity(shingles, shingles)

        assertEquals(1.0, similarity, 0.001, "Identical shingles should have 1.0 similarity")
    }

    @Test
    fun `completely different shingles have zero similarity`() {
        val shingles1 = mapOf(
            2 to setOf("A B", "C D"),
            4 to setOf("A B C D"),
            6 to setOf("A B C D E F"),
            8 to setOf("A B C D E F G H")
        )
        val shingles2 = mapOf(
            2 to setOf("X Y", "Z W"),
            4 to setOf("X Y Z W"),
            6 to setOf("X Y Z W Q R"),
            8 to setOf("X Y Z W Q R S T")
        )

        val similarity = builder.calculateWeightedSimilarity(shingles1, shingles2)

        assertEquals(0.0, similarity, 0.001, "Completely different shingles should have 0.0 similarity")
    }

    @Test
    fun `weights sum to 1`() {
        val weights = builder.getWeights()
        val totalWeight = weights.values.sum()

        assertEquals(1.0, totalWeight, 0.001, "Weights should sum to 1.0")
    }

    @Test
    fun `4-token shingles have highest weight`() {
        val weights = builder.getWeights()

        assertEquals(0.4, weights[4], "4-token shingles should have 40% weight")
        assertTrue(weights[4]!! > weights[2]!!, "4-token weight should exceed 2-token")
        assertTrue(weights[4]!! > weights[6]!!, "4-token weight should exceed 6-token")
        assertTrue(weights[4]!! > weights[8]!!, "4-token weight should exceed 8-token")
    }

    @Test
    fun `empty shingles handled correctly`() {
        val emptyShingles = mapOf(2 to emptySet<String>(), 4 to emptySet(), 6 to emptySet(), 8 to emptySet())
        val nonEmptyShingles = mapOf(
            2 to setOf("A B"),
            4 to setOf("A B C D"),
            6 to setOf("A B C D E F"),
            8 to setOf("A B C D E F G H")
        )

        val emptyVsEmpty = builder.calculateWeightedSimilarity(emptyShingles, emptyShingles)
        val emptyVsNonEmpty = builder.calculateWeightedSimilarity(emptyShingles, nonEmptyShingles)

        assertEquals(1.0, emptyVsEmpty, 0.001, "Two empty sets should have 1.0 similarity")
        assertEquals(0.0, emptyVsNonEmpty, 0.001, "Empty vs non-empty should have 0.0 similarity")
    }
}
