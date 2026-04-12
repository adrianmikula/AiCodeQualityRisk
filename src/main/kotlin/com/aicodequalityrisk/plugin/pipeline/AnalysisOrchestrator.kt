package com.aicodequalityrisk.plugin.pipeline

import com.aicodequalityrisk.plugin.analysis.LocalMockAnalyzerClient
import com.aicodequalityrisk.plugin.capture.DiffCaptureService
import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.aicodequalityrisk.plugin.model.RiskResult
import com.aicodequalityrisk.plugin.model.TriggerType
import com.aicodequalityrisk.plugin.state.AnalysisStateStore
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class AnalysisOrchestrator private constructor(
    private val captureFn: (TriggerType) -> AnalysisInput?,
    private val analyzeFn: (AnalysisInput) -> RiskResult,
    private val updateFn: (AnalysisViewState) -> Unit,
    private val runnerRef: TaskRunner
) : Disposable {

    private val logger = Logger.getInstance(AnalysisOrchestrator::class.java)
    constructor(project: Project) : this(
        captureFn = { trigger -> project.service<DiffCaptureService>().captureCurrentContext(project, trigger) },
        analyzeFn = { input -> project.service<LocalMockAnalyzerClient>().analyze(input) },
        updateFn = { state -> project.service<AnalysisStateStore>().update(state) },
        runnerRef = LatestOnlyRunner()
    )

    fun trigger(triggerType: TriggerType) {
        logger.info("Analysis trigger received: $triggerType")
        runnerRef.submit {
            updateFn(AnalysisViewState.Loading)
            try {
                val input = captureFn(triggerType)
                if (input == null) {
                    logger.debug("No analysis input available for trigger=$triggerType")
                    updateFn(AnalysisViewState.Idle)
                    return@submit
                }
                val result = analyzeFn(input)
                logger.info("Analysis ready for file=${input.filePath} score=${result.score}")
                updateFn(AnalysisViewState.Ready(result))
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                logger.warn("Analysis failed for trigger=$triggerType", e)
                updateFn(AnalysisViewState.Error(e.message ?: "Analysis failed"))
            }
        }
    }

    override fun dispose() {
        runnerRef.shutdown()
    }

    companion object {
        internal fun forTests(
            capture: (TriggerType) -> AnalysisInput?,
            analyze: (AnalysisInput) -> RiskResult,
            updateState: (AnalysisViewState) -> Unit,
            runner: TaskRunner
        ): AnalysisOrchestrator = AnalysisOrchestrator(capture, analyze, updateState, runner)
    }
}
