package com.aicodequalityrisk.plugin.analysis

import kotlin.test.Test
import kotlin.test.assertTrue

class TreeSitterFuzzyDetectorTest {
    private val detector = TreeSitterFuzzyDetector()

    @Test
    fun `detects similar java method bodies with tree-sitter`() {
        val source = """
            public class Example {
                public void copyA(int value) {
                    if (value > 0) {
                        System.out.println(value);
                    }
                }

                public void copyB(int value) {
                    if (value > 0) {
                        System.out.println(value);
                    }
                }

                public void uniqueMethod() {
                    System.out.println("unique");
                }
            }
        """.trimIndent()

        val metrics = detector.detect(source, "/tmp/Example.java")

        assertTrue(metrics.duplicateMethodCount >= 1, "Expected at least one duplicate method pair")
        assertTrue(metrics.maxSimilarityScore >= 0.5, "Expected similarity score above threshold with AST-based approach")
        assertTrue(metrics.duplicateMethodPairs.any { it.firstMethod.contains("copy") && it.secondMethod.contains("copy") })
        assertTrue(metrics.astBasedSimilarityEnabled, "AST-based similarity should be enabled")
    }

    @Test
    fun `does not flag clearly different method bodies as duplicates`() {
        val source = """
            public class Example {
                public int compute(int value) {
                    int result = value * 2;
                    return result;
                }

                public void outputLoop() {
                    for (int i = 0; i < 5; i++) {
                        System.out.println(i);
                    }
                }
            }
        """.trimIndent()

        val metrics = detector.detect(source, "/tmp/Example.java")

        assertTrue(metrics.duplicateMethodCount == 0, "Expected no duplicate method pairs")
        assertTrue(metrics.maxSimilarityScore <= 0.5, "Expected low similarity score for distinct methods with AST-based approach")
    }

    @Test
    fun `normalizes identifiers across similar method bodies`() {
        val source = """
            public class Example {
                public void copyA(int value) {
                    if (value > 0) {
                        System.out.println(value);
                    }
                }

                public void copyB(int amount) {
                    if (amount > 0) {
                        System.out.println(amount);
                    }
                }
            }
        """.trimIndent()

        val metrics = detector.detect(source, "/tmp/Example.java")

        assertTrue(metrics.duplicateMethodCount >= 1, "Expected duplicate pair after identifier normalization")
        assertTrue(metrics.maxSimilarityScore >= 0.5, "Expected similarity score above threshold with AST-based approach")
    }

    @Test
    fun `detects similar scala method bodies with tree-sitter`() {
        val source = """
            object Example {
              def copyA(value: Int): Unit = {
                if (value > 0) {
                  println(value)
                }
              }

              def copyB(value: Int): Unit = {
                if (value > 0) {
                  println(value)
                }
              }

              def uniqueMethod(): Unit = {
                println("unique")
              }
            }
        """.trimIndent()

        val metrics = detector.detect(source, "/tmp/Example.scala")

        assertTrue(metrics.duplicateMethodCount >= 1, "Expected at least one duplicate method pair in Scala")
        assertTrue(metrics.maxSimilarityScore >= 0.5, "Expected similarity score above threshold for Scala")
    }

    @Test
    fun `parses scala files correctly`() {
        val source = """
            object ScalaExample {
              def simpleMethod(): String = "hello"
              
              def complexMatch(x: Any): String = x match {
                case s: String => s
                case i: Int => i.toString
                case _ => "unknown"
              }
            }
        """.trimIndent()

        val metrics = detector.detect(source, "/tmp/ScalaExample.scala")

        // Should not throw exception and should produce valid metrics
        assertTrue(metrics.duplicateMethodCount >= 0, "Should parse Scala files without errors")
    }
}
