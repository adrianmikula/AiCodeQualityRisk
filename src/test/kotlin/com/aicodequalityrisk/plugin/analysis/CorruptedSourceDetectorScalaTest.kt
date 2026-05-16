package com.aicodequalityrisk.plugin.analysis

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CorruptedSourceDetectorScalaTest {
    private val detector = CorruptedSourceDetector()

    @Test
    fun `detects markdown code fences in scala files`() {
        val code = """
            object Test {
                ```scala
                def method(): Unit = {
                    println("hello")
                }
                ```
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.scala")
        
        // This test will fail until Scala support is added to CorruptedSourceDetector
        assertTrue(metrics.markdownTokenCount > 0, "Expected to detect markdown in Scala files")
        assertTrue(metrics.hasCorruptedContent, "Expected to flag corrupted content in Scala files")
    }

    @Test
    fun `detects unbalanced braces in scala files`() {
        val code = """
            object Test {
                def method(): Unit = {
                    if (true) {
                        println("hello")
                    // Missing closing brace
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.scala")
        
        // This test will fail until Scala support is added to CorruptedSourceDetector
        assertTrue(metrics.unbalancedBraceCount > 0, "Expected to detect unbalanced braces in Scala files")
        assertTrue(metrics.hasCorruptedContent, "Expected to flag corrupted content in Scala files")
    }

    @Test
    fun `detects xml fragments in scala files`() {
        val code = """
            object Test {
                def method(): Unit = {
                    <dependency>
                        <groupId>org.example</groupId>
                    </dependency>
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.scala")
        
        // This test will fail until Scala support is added to CorruptedSourceDetector
        assertTrue(metrics.xmlFragmentCount > 0, "Expected to detect XML fragments in Scala files")
        assertTrue(metrics.hasCorruptedContent, "Expected to flag corrupted content in Scala files")
    }

    @Test
    fun `clean scala code has no corrupted content`() {
        val code = """
            object Test {
                private var value: Int = 0
                
                def setValue(value: Int): Unit = {
                    this.value = value
                }
                
                def getValue(): Int = {
                    value
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "Test.scala")
        
        // This test will fail until Scala support is added to CorruptedSourceDetector
        assertFalse(metrics.hasCorruptedContent, "Clean Scala code should not be flagged as corrupted")
    }
}
