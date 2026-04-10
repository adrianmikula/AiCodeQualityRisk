package com.aicodequalityrisk.plugin.analysis

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
                fileSnapshot = "val x = maybeNull!!"
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
                fileSnapshot = "val x = 1"
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
                """.trimIndent() + "\n" + "x".repeat(2200)
            )
        )

        assertTrue(result.findings.size >= 3)
        assertTrue(result.findings.any { it.severity == Severity.HIGH })
    }
}
