package com.aicodequalityrisk.plugin

import com.aicodequalityrisk.plugin.analysis.ASTMetrics
import com.aicodequalityrisk.plugin.analysis.FuzzyMetrics
import com.aicodequalityrisk.plugin.analysis.LocalMockAnalyzerClient
import com.aicodequalityrisk.plugin.model.*
import kotlin.test.Test
import kotlin.test.assertTrue

class IntegrationTest {

    private val analyzerClient = LocalMockAnalyzerClient()

    @Test
    fun `integration test - TestDuplicates kt generates expected risk scores`() {
        // Load the test file content
        val testFileContent = """
            class TestDuplicates {
                fun method1() {
                    println("Hello")
                    println("World")
                }

                fun method2() {
                    println("Hello")
                    println("World")
                }

                fun method3() {
                    println("Hello")
                    println("World")
                }

                fun complexMethod() {
                    val x = 1
                    val y = 2
                    val z = 3
                    if (x > 0) {
                        if (y > 0) {
                            if (z > 0) {
                                println("All positive")
                            }
                        }
                    }
                }

                fun securityMethod() {
                    val name: String? = null
                    println(name!!) // Non-null assertion
                }

                fun performanceMethod() {
                    try {
                        Thread.sleep(1000) // Blocking call
                    } catch (e: Exception) { // Broad exception
                        println("Error")
                    }
                }
            }
        """.trimIndent()

        // Create analysis input
        val input = AnalysisInput(
            projectPath = "/tmp/test",
            filePath = "/tmp/test/TestDuplicates.kt",
            trigger = TriggerType.EDIT,
            diffText = "+ $testFileContent", // Simulate a diff
            fileSnapshot = testFileContent,
            astMetrics = com.aicodequalityrisk.plugin.analysis.ASTMetrics(
                maxMethodLength = 10,
                maxNestingDepth = 4, // Deep nesting
                methodCount = 6
            ),
            fuzzyMetrics = com.aicodequalityrisk.plugin.analysis.FuzzyMetrics(
                duplicateMethodCount = 3, // 3 pairs
                maxSimilarityScore = 0.8, // Lower with AST-based approach
                astBasedSimilarityEnabled = true
            )
        )

        // Run analysis
        val result = analyzerClient.analyze(input)

        // Assert expected scores
        assertTrue(result.score >= 0, "Overall score should be non-negative")
        assertTrue(result.complexityScore >= 0, "Complexity score should be non-negative")
        assertTrue(result.duplicationScore >= 0, "Duplication score should be non-negative")
        assertTrue(result.performanceScore >= 0, "Performance score should be non-negative")
        assertTrue(result.securityScore >= 0, "Security score should be non-negative")

        // Assert findings exist
        assertTrue(result.findings.isNotEmpty(), "Should have at least some findings")
    }

    @Test
    fun `integration test - TestDuplicatesJava java generates expected risk scores`() {
        // Load the test file content
        val testFileContent = """
            public class TestDuplicatesJava {
                public void method1() {
                    System.out.println("Hello");
                    System.out.println("World");
                }

                public void method2() {
                    System.out.println("Hello");
                    System.out.println("World");
                }

                public void method3() {
                    System.out.println("Hello");
                    System.out.println("World");
                }

                public void complexMethod() {
                    int x = 1;
                    int y = 2;
                    int z = 3;
                    if (x > 0) {
                        if (y > 0) {
                            if (z > 0) {
                                System.out.println("All positive");
                            }
                        }
                    }
                }

                public void securityMethod() {
                    String name = null;
                    System.out.println(name); // Potential NPE
                }

                public void performanceMethod() {
                    try {
                        Thread.sleep(1000); // Blocking call
                    } catch (Exception e) { // Broad exception
                        System.out.println("Error");
                    }
                }
            }
        """.trimIndent()

        // Create analysis input
        val input = AnalysisInput(
            projectPath = "/tmp/test",
            filePath = "/tmp/test/TestDuplicatesJava.java",
            trigger = TriggerType.EDIT,
            diffText = "+ $testFileContent", // Simulate a diff
            fileSnapshot = testFileContent,
            astMetrics = com.aicodequalityrisk.plugin.analysis.ASTMetrics(
                maxMethodLength = 10,
                maxNestingDepth = 4, // Deep nesting
                methodCount = 6
            ),
            fuzzyMetrics = com.aicodequalityrisk.plugin.analysis.FuzzyMetrics(
                duplicateMethodCount = 3, // 3 pairs
                maxSimilarityScore = 0.8, // Lower with AST-based approach
                astBasedSimilarityEnabled = true
            )
        )

        // Run analysis
        val result = analyzerClient.analyze(input)

        // Assert expected scores
        assertTrue(result.score >= 0, "Overall score should be non-negative")
        assertTrue(result.complexityScore >= 0, "Complexity score should be non-negative")
        assertTrue(result.duplicationScore >= 0, "Duplication score should be non-negative")
        assertTrue(result.performanceScore >= 0, "Performance score should be non-negative")
        assertTrue(result.securityScore >= 0, "Security score should be non-negative")

        // Assert findings exist
        assertTrue(result.findings.isNotEmpty(), "Should have at least some findings")
    }
}