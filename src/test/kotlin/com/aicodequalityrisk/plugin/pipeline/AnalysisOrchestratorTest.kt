package com.aicodequalityrisk.plugin.pipeline

import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.aicodequalityrisk.plugin.model.Finding
import com.aicodequalityrisk.plugin.model.RiskResult
import com.aicodequalityrisk.plugin.model.Severity
import com.aicodequalityrisk.plugin.model.TriggerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AnalysisOrchestratorTest {

    @Test
    fun `trigger publishes loading then ready`() {
        val states = mutableListOf<AnalysisViewState>()
        val orchestrator = AnalysisOrchestrator.forTests(
            capture = {
                AnalysisInput(
                    projectPath = "/tmp",
                    filePath = "/tmp/Test.kt",
                    trigger = it,
                    diffText = "+val a = 1",
                    fileSnapshot = "val a = 1"
                )
            },
            analyze = {
                RiskResult(
                    score = 25,
                    findings = listOf(Finding("Test", "Detail", Severity.LOW)),
                    explanations = listOf("ok"),
                    sourceFilePath = it.filePath
                )
            },
            updateState = { states += it },
            runner = ImmediateRunner()
        )

        orchestrator.trigger(TriggerType.MANUAL)

        assertEquals(2, states.size)
        assertIs<AnalysisViewState.Loading>(states[0])
        assertIs<AnalysisViewState.Ready>(states[1])
    }

    @Test
    fun `trigger publishes idle when capture has no input`() {
        val states = mutableListOf<AnalysisViewState>()
        val orchestrator = AnalysisOrchestrator.forTests(
            capture = { null },
            analyze = { error("analyze should not run") },
            updateState = { states += it },
            runner = ImmediateRunner()
        )

        orchestrator.trigger(TriggerType.SAVE)

        assertEquals(2, states.size)
        assertIs<AnalysisViewState.Loading>(states[0])
        assertIs<AnalysisViewState.Idle>(states[1])
    }

    @Test
    fun `trigger publishes error on analysis exception`() {
        val states = mutableListOf<AnalysisViewState>()
        val orchestrator = AnalysisOrchestrator.forTests(
            capture = {
                AnalysisInput(
                    projectPath = "/tmp",
                    filePath = "/tmp/Test.kt",
                    trigger = it,
                    diffText = "+x",
                    fileSnapshot = "x"
                )
            },
            analyze = { throw IllegalStateException("boom") },
            updateState = { states += it },
            runner = ImmediateRunner()
        )

        orchestrator.trigger(TriggerType.EDIT)

        assertEquals(2, states.size)
        assertIs<AnalysisViewState.Loading>(states[0])
        val error = assertIs<AnalysisViewState.Error>(states[1])
        assertEquals("boom", error.message)
    }

    private class ImmediateRunner : TaskRunner {
        override fun submit(task: () -> Unit) = task()
        override fun shutdown() = Unit
    }
}
