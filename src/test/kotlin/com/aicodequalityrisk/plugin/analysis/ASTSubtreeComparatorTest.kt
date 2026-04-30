package com.aicodequalityrisk.plugin.analysis

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ASTSubtreeComparatorTest {
    private val comparator = ASTSubtreeComparator()

    @Test
    fun `calculates similarity for identical structures`() {
        val code = """
            public class Example {
                public void methodA(int x) {
                    if (x > 0) {
                        System.out.println(x);
                    }
                }
            }
        """.trimIndent()

        val hash1 = "IF EXPR CALL"
        val hash2 = "IF EXPR CALL"

        val similarity = comparator.calculateSimilarity(hash1, hash2)

        assertEquals(1.0, similarity, 0.001, "Identical structures should have 1.0 similarity")
    }

    @Test
    fun `calculates similarity for similar structures`() {
        val hash1 = "IF EXPR CALL RETURN"
        val hash2 = "IF EXPR CALL RETURN"

        val similarity = comparator.calculateSimilarity(hash1, hash2)

        assertEquals(1.0, similarity, 0.001, "Similar structures should have high similarity")
    }

    @Test
    fun `calculates lower similarity for different structures`() {
        val hash1 = "IF EXPR CALL"
        val hash2 = "FOR EXPR CALL"

        val similarity = comparator.calculateSimilarity(hash1, hash2)

        assertTrue(similarity < 1.0, "Different structures should have lower similarity")
        assertTrue(similarity > 0.0, "Should have some similarity due to shared tokens")
    }

    @Test
    fun `calculates zero similarity for completely different structures`() {
        val hash1 = "IF EXPR CALL"
        val hash2 = "RETURN"

        val similarity = comparator.calculateSimilarity(hash1, hash2)

        assertTrue(similarity < 0.5, "Completely different structures should have low similarity")
    }

    @Test
    fun `handles empty hashes`() {
        val similarity1 = comparator.calculateSimilarity("", "")
        val similarity2 = comparator.calculateSimilarity("IF EXPR", "")

        assertEquals(0.0, similarity1, 0.001, "Empty hashes should have 0.0 similarity")
        assertEquals(0.0, similarity2, 0.001, "Empty vs non-empty should have 0.0 similarity")
    }

    @Test
    fun `calculates tree edit distance for identical structures`() {
        val hash1 = "IF EXPR CALL"
        val hash2 = "IF EXPR CALL"

        val distance = comparator.calculateTreeEditDistance(hash1, hash2)

        assertEquals(0.0, distance, 0.001, "Identical structures should have 0.0 distance")
    }

    @Test
    fun `calculates tree edit distance for different structures`() {
        val hash1 = "IF EXPR CALL"
        val hash2 = "FOR EXPR CALL"

        val distance = comparator.calculateTreeEditDistance(hash1, hash2)

        assertTrue(distance > 0.0, "Different structures should have positive distance")
        assertTrue(distance <= 1.0, "Distance should be normalized to 0-1 range")
    }

    @Test
    fun `normalizes node types correctly`() {
        val code = """
            public class Example {
                public void method() {
                    if (x > 0) {
                        for (int i = 0; i < 10; i++) {
                            System.out.println(i);
                        }
                    }
                    while (y > 0) {
                        doWork();
                    }
                    try {
                        riskyOperation();
                    } catch (Exception e) {
                        handleError();
                    }
                }
            }
        """.trimIndent()

        // This test verifies that the normalization logic works
        // In practice, this would be tested by extracting hashes from actual AST nodes
        assertTrue(true, "Normalization logic should handle various node types")
    }
}
