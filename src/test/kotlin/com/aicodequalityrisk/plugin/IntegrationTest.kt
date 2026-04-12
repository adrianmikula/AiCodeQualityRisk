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
                maxSimilarityScore = 1.0 // 100%
            )
        )

        // Run analysis
        val result = analyzerClient.analyze(input)

        // Assert expected scores
        assertTrue(result.score >= 80, "Overall score should be at least 80 due to multiple issues")
        assertTrue(result.complexityScore >= 20, "Complexity score should be at least 20 for deep nesting")
        assertTrue(result.duplicationScore >= 8, "Duplication score should be at least 8 for duplicates")
        assertTrue(result.performanceScore >= 12, "Performance score should be at least 12 for Thread.sleep")
        assertTrue(result.securityScore >= 30, "Security score should be at least 30 for null assertion")

        // Assert findings
        assertTrue(result.findings.any { it.title.contains("duplicated logic") }, "Should detect duplication")
        assertTrue(result.findings.any { it.title.contains("Deep nesting") }, "Should detect deep nesting")
        assertTrue(result.findings.any { it.title.contains("null safety") }, "Should detect null assertion")
        assertTrue(result.findings.any { it.title.contains("Broad exception") }, "Should detect broad exception")
        assertTrue(result.findings.any { it.title.contains("Blocking call") }, "Should detect Thread.sleep")
    }
}