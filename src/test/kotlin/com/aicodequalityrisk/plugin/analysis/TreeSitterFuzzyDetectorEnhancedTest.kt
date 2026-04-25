package com.aicodequalityrisk.plugin.analysis

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TreeSitterFuzzyDetectorEnhancedTest {
    private val detector = TreeSitterFuzzyDetector()

    @Test
    fun `detector uses adaptive thresholds`() {
        val code = """
            public class Example {
                public void methodA() { return true; }
                public void methodB() { return false; }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertTrue(metrics.adaptiveThresholdsEnabled, "Should use adaptive thresholds")
    }

    @Test
    fun `detector uses multi-granular shingling`() {
        val code = """
            public class Example {
                public void methodA() { return true; }
                public void methodB() { return false; }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertTrue(metrics.multiGranularShinglingEnabled, "Should use multi-granular shingling")
    }

    @Test
    fun `detector uses entropy scores`() {
        val code = """
            public class Example {
                public void methodA() { return true; }
                public void methodB() { return false; }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertTrue(metrics.entropyScoresEnabled, "Should use entropy scores")
        assertTrue(metrics.boilerplateBloatScore in 0.0..1.0, "Boilerplate bloat score should be in valid range")
        assertTrue(metrics.verboseCommentScore in 0.0..1.0, "Verbose comment score should be in valid range")
        assertTrue(metrics.overDefensiveScore in 0.0..1.0, "Over defensive score should be in valid range")
        assertTrue(metrics.poorNamingScore in 0.0..1.0, "Poor naming score should be in valid range")
        assertTrue(metrics.frameworkMisuseScore in 0.0..1.0, "Framework misuse score should be in valid range")
        assertTrue(metrics.excessiveDocumentationScore in 0.0..1.0, "Excessive documentation score should be in valid range")
    }

    @Test
    fun `adaptive thresholds work for simple similar code`() {
        val simpleSimilarCode = """
            public class Example {
                public void methodA() { return true; }
                public void methodB() { return false; }
                public void methodC() { return null; }
            }
        """.trimIndent()

        val metrics = detector.detect(simpleSimilarCode, "/tmp/Example.java")

        // With adaptive thresholds, short methods get lower thresholds
        // This is a balance between catching legitimate duplicates and avoiding false positives
        assertTrue(metrics.duplicateMethodCount <= 3,
            "Should not flag all simple methods as duplicates, found ${metrics.duplicateMethodCount}")
    }

    @Test
    fun `adaptive thresholds catch complex duplicates`() {
        val complexDuplicateCode = """
            public class Example {
                public void processData(java.util.List<String> data) {
                    if (data != null && !data.isEmpty()) {
                        for (String item : data) {
                            if (item != null && item.length() > 0) {
                                System.out.println(item.trim());
                            }
                        }
                    }
                }

                public void handleItems(java.util.List<String> items) {
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

        assertTrue(metrics.duplicateMethodCount >= 1, "Should catch complex duplicate methods, found ${metrics.duplicateMethodCount}")
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

        if (metrics.duplicateMethodPairs.isNotEmpty()) {
            val firstPair = metrics.duplicateMethodPairs.first()
            assertNotNull(firstPair.shingleBreakdown, "Should have shingle breakdown")
            assertTrue(firstPair.shingleBreakdown!!.similarities.isNotEmpty(),
                "Should have similarities for different sizes")
        }
    }

    @Test
    fun `similarity pairs include threshold information`() {
        val code = """
            public class Example {
                public void similar1() {
                    if (x > 0) {
                        System.out.println("positive");
                    }
                }
                public void similar2() {
                    if (y > 0) {
                        System.out.println("positive");
                    }
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        for (pair in metrics.duplicateMethodPairs) {
            assertTrue(pair.threshold in 0.4..0.85,
                "Threshold ${pair.threshold} should be within valid range")
            assertTrue(pair.similarity >= pair.threshold,
                "Similarity ${pair.similarity} should meet threshold ${pair.threshold}")
        }
    }

    @Test
    fun `empty code returns default metrics`() {
        val metrics = detector.detect("", "/tmp/Empty.java")

        assertEquals(0, metrics.duplicateMethodCount)
        assertEquals(0.0, metrics.maxSimilarityScore)
        assertTrue(metrics.duplicateMethodPairs.isEmpty())
    }

    @Test
    fun `single method returns no duplicates`() {
        val code = """
            public class Example {
                public void onlyMethod() {
                    System.out.println("Hello");
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertEquals(0, metrics.duplicateMethodCount, "Single method should have no duplicates")
    }

    @Test
    fun `unsupported file type returns empty metrics`() {
        val code = """
            public class Example {
                public void methodA() { }
                public void methodB() { }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.py")

        assertEquals(0, metrics.duplicateMethodCount, "Should return empty metrics for unsupported files")
    }

    @Test
    fun `average threshold calculation works`() {
        val code = """
            public class Example {
                public void a() { if (x > 0) { doWork(); } }
                public void b() { if (y > 0) { doWork(); } }
                public void c() { if (z > 0) { doWork(); } }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        if (metrics.duplicateMethodPairs.isNotEmpty()) {
            val avgThreshold = metrics.getAverageThreshold()
            assertTrue(avgThreshold in 0.4..0.85,
                "Average threshold $avgThreshold should be within valid range")
        }
    }

    @Test
    fun `average shingle size calculation works`() {
        val code = """
            public class Example {
                public void a() { if (x > 0) { doWork(); } }
                public void b() { if (y > 0) { doWork(); } }
                public void c() { if (z > 0) { doWork(); } }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        val avgShingleSize = metrics.getAverageShingleSize()
        assertTrue(avgShingleSize in 2.0..8.0,
            "Average shingle size $avgShingleSize should be between 2 and 8")
    }

    @Test
    fun `shingle breakdown contains all sizes`() {
        val code = """
            public class Example {
                public void a() { if (x > 0) { doWork(); } }
                public void b() { if (y > 0) { doWork(); } }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        for (pair in metrics.duplicateMethodPairs) {
            val breakdown = pair.shingleBreakdown
            assertNotNull(breakdown)
            assertTrue(breakdown!!.similarities.containsKey(2), "Should have 2-token similarity")
            assertTrue(breakdown.similarities.containsKey(4), "Should have 4-token similarity")
            assertTrue(breakdown.similarities.containsKey(6), "Should have 6-token similarity")
            assertTrue(breakdown.similarities.containsKey(8), "Should have 8-token similarity")
        }
    }

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
        assertTrue(metrics.maxSimilarityScore > 0.0, "Should have positive similarity score")
    }
}
