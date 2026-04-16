package com.aicodequalityrisk.plugin.pipeline

import com.aicodequalityrisk.plugin.analysis.LocalMockAnalyzerClient
import com.aicodequalityrisk.plugin.capture.DiffCaptureService
import com.aicodequalityrisk.plugin.mcp.McpServerService
import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.aicodequalityrisk.plugin.model.RiskResult
import com.aicodequalityrisk.plugin.model.TriggerType
import com.aicodequalityrisk.plugin.service.LicenseService
import com.aicodequalityrisk.plugin.state.AnalysisStateStore
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class AnalysisOrchestrator private constructor(
    private val captureFn: (TriggerType) -> AnalysisInput?,
    private val analyzeFn: (AnalysisInput) -> RiskResult,
    private val updateFn: (AnalysisViewState) -> Unit,
    private val runnerRef: TaskRunner,
    private val licenseCheck: () -> Boolean,
    private val saveScanFn: (RiskResult) -> Unit
) : Disposable {

    private val logger = Logger.getInstance(AnalysisOrchestrator::class.java)
    private val scanCooldowns = ConcurrentHashMap<String, Long>()
    private val cooldownMs = 30_000L
    constructor(project: Project) : this(
        captureFn = { trigger -> project.service<DiffCaptureService>().captureCurrentContext(project, trigger) },
        analyzeFn = { input -> project.service<LocalMockAnalyzerClient>().analyze(input) },
        updateFn = { state -> project.service<AnalysisStateStore>().update(state) },
        runnerRef = LatestOnlyRunner(),
        licenseCheck = { project.service<LicenseService>().isTrialExpired() },
        saveScanFn = { result ->
            project.service<McpServerService>().saveLatestScan(result)
            project.service<McpServerService>().saveScanForFile(result)
        }
    )

    fun trigger(triggerType: TriggerType) {
        logger.info("Analysis trigger received: $triggerType")
        if (licenseCheck()) {
            logger.info("Analysis blocked: trial expired")
            updateFn(AnalysisViewState.Error("Trial expired. Please upgrade to continue using the plugin."))
            return
        }
        logger.info("Analysis proceeding - license check passed")
        runnerRef.submit {
            updateFn(AnalysisViewState.Loading)
            logger.info("Analysis state set to Loading")
            val input = captureFn(triggerType)
            if (input == null) {
                logger.debug("No analysis input available for trigger=$triggerType")
                updateFn(AnalysisViewState.Idle)
                logger.info("Analysis state set to Idle (no input)")
                return@submit
            }
            val filePath = input.filePath
            if (triggerType == TriggerType.EDIT && filePath != null) {
                val now = System.currentTimeMillis()
                val lastScan = scanCooldowns[filePath]
                if (lastScan != null && now - lastScan < cooldownMs) {
                    logger.info("Analysis skipped: cooldown active for file=$filePath (${(now - lastScan)/1000}s since last scan)")
                    return@submit
                }
            }
            logger.info("Analysis state set to Loading")
            try {
                logger.info("Calling analyze function for file=${input.filePath}")
                val result = analyzeFn(input)
                logger.info("Analysis complete: score=${result.score}, complexity=${result.complexityScore}")
                logger.info("Analysis ready for file=${input.filePath} score=${result.score}")
                if (filePath != null) {
                    scanCooldowns[filePath] = System.currentTimeMillis()
                }
                saveScanFn(result)
                updateFn(AnalysisViewState.Ready(result))
                logger.info("Analysis state set to Ready")
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                logger.warn("Analysis failed for trigger=$triggerType", e)
                updateFn(AnalysisViewState.Error(e.message ?: "Analysis failed"))
                logger.info("Analysis state set to Error: ${e.message}")
            }
        }
    }

    override fun dispose() {
        scanCooldowns.clear()
        runnerRef.shutdown()
    }

    companion object {
        internal fun forTests(
            capture: (TriggerType) -> AnalysisInput?,
            analyze: (AnalysisInput) -> RiskResult,
            updateState: (AnalysisViewState) -> Unit,
            runner: TaskRunner,
            isTrialExpired: Boolean = false,
            saveScan: (RiskResult) -> Unit = {}
        ): AnalysisOrchestrator = AnalysisOrchestrator(
            capture, analyze, updateState, runner,
            { isTrialExpired }, saveScan
        )
    }
}