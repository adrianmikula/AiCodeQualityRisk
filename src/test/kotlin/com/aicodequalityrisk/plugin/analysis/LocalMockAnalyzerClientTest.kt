package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.Severity
import com.aicodequalityrisk.plugin.model.TriggerType
import kotlin.test.Test
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
}
