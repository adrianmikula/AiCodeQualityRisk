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
                    fileSnapshot = "x",
                    astMetrics = com.aicodequalityrisk.plugin.analysis.ASTMetrics()
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

    @Test
    fun `edit trigger within cooldown period is skipped`() {
        val states = mutableListOf<AnalysisViewState>()
        var analyzeCallCount = 0
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
                analyzeCallCount++
                RiskResult(
                    score = 25,
                    sourceFilePath = it.filePath
                )
            },
            updateState = { states += it },
            runner = CooldownTestRunner()
        )

        orchestrator.trigger(TriggerType.EDIT)
        orchestrator.trigger(TriggerType.EDIT)

        assertEquals(1, analyzeCallCount)
    }

    @Test
    fun `edit trigger on different file bypasses cooldown`() {
        val states = mutableListOf<AnalysisViewState>()
        var analyzeCallCount = 0
        val orchestrator = AnalysisOrchestrator.forTests(
            capture = { trigger ->
                val filePath = if (states.size < 2) "/tmp/Test1.kt" else "/tmp/Test2.kt"
                AnalysisInput(
                    projectPath = "/tmp",
                    filePath = filePath,
                    trigger = trigger,
                    diffText = "+val a = 1",
                    fileSnapshot = "val a = 1",
                    astMetrics = com.aicodequalityrisk.plugin.analysis.ASTMetrics()
                )
            },
            analyze = {
                analyzeCallCount++
                RiskResult(
                    score = 25,
                    sourceFilePath = it.filePath
                )
            },
            updateState = { states += it },
            runner = CooldownTestRunner()
        )

        orchestrator.trigger(TriggerType.EDIT)
        orchestrator.trigger(TriggerType.EDIT)

        assertEquals(2, analyzeCallCount)
    }

    @Test
    fun `save trigger bypasses cooldown`() {
        val states = mutableListOf<AnalysisViewState>()
        var analyzeCallCount = 0
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
                analyzeCallCount++
                RiskResult(
                    score = 25,
                    sourceFilePath = it.filePath
                )
            },
            updateState = { states += it },
            runner = CooldownTestRunner()
        )

        orchestrator.trigger(TriggerType.SAVE)
        orchestrator.trigger(TriggerType.SAVE)

        assertEquals(2, analyzeCallCount)
    }

    @Test
    fun `focus trigger bypasses cooldown`() {
        val states = mutableListOf<AnalysisViewState>()
        var analyzeCallCount = 0
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
                analyzeCallCount++
                RiskResult(
                    score = 25,
                    sourceFilePath = it.filePath
                )
            },
            updateState = { states += it },
            runner = CooldownTestRunner()
        )

        orchestrator.trigger(TriggerType.FOCUS)
        orchestrator.trigger(TriggerType.FOCUS)

        assertEquals(2, analyzeCallCount)
    }

    @Test
    fun `manual trigger bypasses cooldown`() {
        val states = mutableListOf<AnalysisViewState>()
        var analyzeCallCount = 0
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
                analyzeCallCount++
                RiskResult(
                    score = 25,
                    sourceFilePath = it.filePath
                )
            },
            updateState = { states += it },
            runner = CooldownTestRunner()
        )

        orchestrator.trigger(TriggerType.MANUAL)
        orchestrator.trigger(TriggerType.MANUAL)

        assertEquals(2, analyzeCallCount)
    }

    private class ImmediateRunner : TaskRunner {
        override fun submit(task: () -> Unit) = task()
        override fun shutdown() = Unit
    }

    private class CooldownTestRunner : TaskRunner {
        override fun submit(task: () -> Unit) {
            Thread(task).start()
            Thread.sleep(50)
        }
        override fun shutdown() = Unit
    }
}
