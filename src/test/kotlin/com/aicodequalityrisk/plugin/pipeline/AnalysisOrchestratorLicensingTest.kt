package com.aicodequalityrisk.plugin.pipeline

import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.aicodequalityrisk.plugin.model.RiskResult
import com.aicodequalityrisk.plugin.model.Severity
import com.aicodequalityrisk.plugin.model.TriggerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalysisOrchestratorLicensingTest {

    @Test
    fun `analysis proceeds when trial is active`() {
        val states = mutableListOf<AnalysisViewState>()
        val orchestrator = AnalysisOrchestrator.forTests(
            capture = {
                AnalysisInput(
                    projectPath = "/tmp",
                    filePath = "/tmp/Test.kt",
                    trigger = it,
                    diffText = "+val a = 1",
                    fileSnapshot = "val a = 1",
                    astMetrics = com.aicodequalityrisk.plugin.analysis.ASTMetrics()
                )
            },
            analyze = {
                RiskResult(
                    score = 25,
                    complexityScore = 10,
                    duplicationScore = 5,
                    performanceScore = 5,
                    securityScore = 5,
                    findings = listOf(),
                    explanations = listOf("ok"),
                    sourceFilePath = it.filePath
                )
            },
            updateState = { states += it },
            runner = ImmediateRunner(),
            isTrialExpired = false
        )

        orchestrator.trigger(TriggerType.MANUAL)

        assertEquals(2, states.size)
        assertEquals(AnalysisViewState.Loading::class, states[0]::class)
        assertEquals(AnalysisViewState.Ready::class, states[1]::class)
    }

    @Test
    fun `analysis blocked when trial is expired`() {
        val states = mutableListOf<AnalysisViewState>()
        val orchestrator = AnalysisOrchestrator.forTests(
            capture = {
                AnalysisInput(
                    projectPath = "/tmp",
                    filePath = "/tmp/Test.kt",
                    trigger = it,
                    diffText = "+val a = 1",
                    fileSnapshot = "val a = 1",
                    astMetrics = com.aicodequalityrisk.plugin.analysis.ASTMetrics()
                )
            },
            analyze = { error("analyze should not run") },
            updateState = { states += it },
            runner = ImmediateRunner(),
            isTrialExpired = true
        )

        orchestrator.trigger(TriggerType.MANUAL)

        assertEquals(1, states.size)
        val error = states[0] as AnalysisViewState.Error
        assertTrue(error.message!!.contains("Trial expired"))
    }

    @Test
    fun `analysis error message contains upgrade prompt`() {
        val states = mutableListOf<AnalysisViewState>()
        val orchestrator = AnalysisOrchestrator.forTests(
            capture = {
                AnalysisInput(
                    projectPath = "/tmp",
                    filePath = "/tmp/Test.kt",
                    trigger = it,
                    diffText = "+val a = 1",
                    fileSnapshot = "val a = 1",
                    astMetrics = com.aicodequalityrisk.plugin.analysis.ASTMetrics()
                )
            },
            analyze = { error("analyze should not run") },
            updateState = { states += it },
            runner = ImmediateRunner(),
            isTrialExpired = true
        )

        orchestrator.trigger(TriggerType.EDIT)

        assertEquals(1, states.size)
        val error = states[0] as AnalysisViewState.Error
        assertTrue(error.message!!.contains("Please upgrade"))
    }

    private class ImmediateRunner : TaskRunner {
        override fun submit(task: () -> Unit) = task()
        override fun shutdown() = Unit
    }
}
