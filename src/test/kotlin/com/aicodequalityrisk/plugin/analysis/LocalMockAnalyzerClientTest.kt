package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.analysis.FuzzyMetrics
import com.aicodequalityrisk.plugin.analysis.MethodSimilarityPair
import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.Severity
import com.aicodequalityrisk.plugin.model.TriggerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalMockAnalyzerClientTest {
    private val client = LocalMockAnalyzerClient()

    @Test
    fun `non-null assertion should produce high-severity finding`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+val x = maybeNull!!",
                fileSnapshot = "val x = maybeNull!!",
                astMetrics = ASTMetrics()
            )
        )

        assertTrue(result.score >= 30)
        assertTrue(result.findings.any { it.severity == Severity.HIGH })
    }

    @Test
    fun `empty low-risk input should return fallback finding`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.MANUAL,
                diffText = "+val x = 1",
                fileSnapshot = "val x = 1",
                astMetrics = ASTMetrics()
            )
        )

        assertEquals(1, result.findings.size)
        assertTrue(result.findings.first().title.contains("No high-risk"))
        assertTrue(result.score > 0)
    }

    @Test
    fun `multiple rules should accumulate findings`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+// TODO fix\n+if (x) { }",
                fileSnapshot = """
                    fun run() {
                        // TODO fix
                        val x = maybeNull!!
                        try { Thread.sleep(1) } catch (Exception: Exception) {}
                    }
                """.trimIndent() + "\n" + "x".repeat(2200),
                astMetrics = ASTMetrics()
            )
        )

        assertTrue(result.findings.size >= 3)
        assertTrue(result.findings.any { it.severity == Severity.HIGH })
    }

    @Test
    fun `includes fuzzy duplicate method finding when fuzzy metrics are present`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.java",
                trigger = TriggerType.EDIT,
                diffText = "+public void copyA() {}\n+public void copyB() {}",
                fileSnapshot = "public class Example { public void copyA() {} public void copyB() {} }",
                astMetrics = ASTMetrics(),
                fuzzyMetrics = FuzzyMetrics(
                    duplicateMethodCount = 1,
                    maxSimilarityScore = 0.72,
                    duplicateMethodPairs = listOf(MethodSimilarityPair("copyA", "copyB", 0.72))
                )
            )
        )

        assertTrue(result.findings.any { it.title.contains("Possible duplicated logic detected") })
        assertTrue(result.findings.any { it.category == Category.DUPLICATION })
    }
}
