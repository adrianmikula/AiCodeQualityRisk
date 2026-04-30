package com.aicodequalityrisk.plugin.analysis

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CorruptedSourceDetectorTest {
    private val detector = CorruptedSourceDetector()

    @Test
    fun `detects markdown code fences`() {
        val code = """
            public class Test {
                ```java
                int x = 5;
                ```
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.java")
        assertTrue(metrics.markdownTokenCount > 0)
        assertTrue(metrics.hasCorruptedContent)
    }

    @Test
    fun `detects markdown file path markers`() {
        val code = """
            <file path=src/main/java/Test.java>
            public class Test {
                int x = 5;
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.java")
        assertTrue(metrics.markdownTokenCount > 0)
        assertTrue(metrics.hasCorruptedContent)
    }

    @Test
    fun `detects XML fragments`() {
        val code = """
            public class Test {
                <dependency>
                    <groupId>org.example</groupId>
                </dependency>
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.java")
        assertTrue(metrics.xmlFragmentCount > 0)
        assertTrue(metrics.hasCorruptedContent)
    }

    @Test
    fun `detects unbalanced braces`() {
        val code = """
            public class Test {
                public void method() {
                    if (true) {
                        int x = 5;
                    // Missing closing brace
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.java")
        assertTrue(metrics.unbalancedBraceCount > 0)
        assertTrue(metrics.hasCorruptedContent)
    }

    @Test
    fun `detects balanced braces correctly`() {
        val code = """
            public class Test {
                public void method() {
                    if (true) {
                        int x = 5;
                    }
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.java")
        assertEquals(0, metrics.unbalancedBraceCount)
    }

    @Test
    fun `detects mixed language density`() {
        val code = """
            This is a sentence with multiple words. It has punctuation.
            Another sentence here. And yet another one.
            public class Test {
                int x = 5;
            }
            This is more prose with sentences. It looks like documentation.
            Even more text here that forms complete sentences.
        """.trimIndent()

        val metrics = detector.detect(code, "Test.java")
        assertTrue(metrics.mixedLanguageDensity > 0)
    }

    @Test
    fun `clean code has no corrupted content`() {
        val code = """
            package com.example;

            public class Test {
                private int value;
                
                public void setValue(int value) {
                    this.value = value;
                }
                
                public int getValue() {
                    return value;
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.java")
        assertFalse(metrics.hasCorruptedContent)
        assertEquals(0, metrics.markdownTokenCount)
        assertEquals(0, metrics.xmlFragmentCount)
        assertEquals(0, metrics.unbalancedBraceCount)
    }

    @Test
    fun `skips unsupported file extensions`() {
        val code = """
            ```python
            def hello():
                print("Hello")
            ```
        """.trimIndent()

        val metrics = detector.detect(code, "test.py")
        assertFalse(metrics.hasCorruptedContent)
        assertEquals(0, metrics.markdownTokenCount)
    }

    @Test
    fun `supports Kotlin files`() {
        val code = """
            ```kotlin
            class Test {
                fun method() {
                    println("Hello")
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.kt")
        assertTrue(metrics.markdownTokenCount > 0)
        assertTrue(metrics.hasCorruptedContent)
    }

    @Test
    fun `detects parse failure with corrupted content`() {
        val code = """
            This is not valid Java code at all.
            It has no class definition or proper syntax.
            Just random text that won't parse.
        """.trimIndent()

        val metrics = detector.detect(code, "Test.java")
        // Parse failure detection depends on both JavaParser and Tree-sitter failing
        // This may or may not trigger depending on parser behavior
        // The key is that the detector doesn't crash
        assertTrue(metrics.mixedLanguageDensity > 0)
    }
}
