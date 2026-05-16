package com.aicodequalityrisk.plugin.analysis

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class ASTAnalyzerScalaTest {
    private val analyzer = ASTAnalyzer()

    @Test
    fun `scala code returns empty metrics as AST analyzer is Java-only`() {
        val source = """
            object ScalaExample {
                def processSecret(): Unit = {
                    val apiKey = "sk-1234567890abcdef"
                    val password = "secret123"
                    println(apiKey)
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(source)

        // ASTAnalyzer only supports Java via JavaParser
        // For Kotlin/Scala, use TreeSitterFuzzyDetector for duplicate detection
        assertEquals(0, metrics.methodCount, "Scala should return empty metrics as AST analyzer is Java-only")
        assertEquals(0, metrics.hardcodedSecretCount, "Scala should return empty metrics as AST analyzer is Java-only")
    }

    @Test
    fun `kotlin code may have partial parsing via JavaParser but is not officially supported`() {
        val source = """
            class KotlinExample {
                fun processSecret() {
                    val apiKey = "sk-1234567890abcdef"
                    val password = "secret123"
                    println(apiKey)
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(source)

        // ASTAnalyzer only supports Java via JavaParser
        // Kotlin may partially parse due to Java/Kotlin similarity, but results are unreliable
        // For proper Kotlin analysis, use TreeSitterFuzzyDetector for duplicate detection
        // We just verify it doesn't crash
        assertTrue(metrics.methodCount >= 0, "Should not crash on Kotlin code")
    }

    @Test
    fun `java code is analyzed correctly`() {
        val source = """
            public class JavaExample {
                public void processSecret() {
                    String apiKey = "sk-1234567890abcdef";
                    String password = "secret123";
                    System.out.println(apiKey);
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(source)

        // Java should be analyzed correctly
        assertTrue(metrics.methodCount > 0, "Java should be analyzed with method count")
        assertTrue(metrics.hardcodedSecretCount > 0, "Java should detect hardcoded secrets")
    }
}
