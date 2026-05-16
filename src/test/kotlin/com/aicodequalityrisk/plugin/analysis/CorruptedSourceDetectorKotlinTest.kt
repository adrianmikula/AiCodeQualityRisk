package com.aicodequalityrisk.plugin.analysis

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CorruptedSourceDetectorKotlinTest {
    private val detector = CorruptedSourceDetector()

    @Test
    fun `detects markdown code fences in kotlin files`() {
        val code = """
            class Test {
                ```kotlin
                fun method() {
                    println("hello")
                }
                ```
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.kt")
        
        // This test will fail until Kotlin support is added to CorruptedSourceDetector
        assertTrue(metrics.markdownTokenCount > 0, "Expected to detect markdown in Kotlin files")
        assertTrue(metrics.hasCorruptedContent, "Expected to flag corrupted content in Kotlin files")
    }

    @Test
    fun `detects unbalanced braces in kotlin files`() {
        val code = """
            class Test {
                fun method() {
                    if (true) {
                        println("hello")
                    // Missing closing brace
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.kt")
        
        // This test will fail until Kotlin support is added to CorruptedSourceDetector
        assertTrue(metrics.unbalancedBraceCount > 0, "Expected to detect unbalanced braces in Kotlin files")
        assertTrue(metrics.hasCorruptedContent, "Expected to flag corrupted content in Kotlin files")
    }

    @Test
    fun `detects xml fragments in kotlin files`() {
        val code = """
            class Test {
                fun method() {
                    <dependency>
                        <groupId>org.example</groupId>
                    </dependency>
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.kt")
        
        // This test will fail until Kotlin support is added to CorruptedSourceDetector
        assertTrue(metrics.xmlFragmentCount > 0, "Expected to detect XML fragments in Kotlin files")
        assertTrue(metrics.hasCorruptedContent, "Expected to flag corrupted content in Kotlin files")
    }

    @Test
    fun `clean kotlin code has no corrupted content`() {
        val code = """
            class Test {
                private var value: Int = 0
                
                fun setValue(value: Int) {
                    this.value = value
                }
                
                fun getValue(): Int {
                    return value
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.kt")
        
        // This test will fail until Kotlin support is added to CorruptedSourceDetector
        assertFalse(metrics.hasCorruptedContent, "Clean Kotlin code should not be flagged as corrupted")
    }
}
