package com.aicodequalityrisk.plugin.state

import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.aicodequalityrisk.plugin.model.RiskResult
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
class AnalysisStateStore(private val project: Project) {
    private val state = AtomicReference<AnalysisViewState>(AnalysisViewState.Idle)
    private val listeners = CopyOnWriteArrayList<(AnalysisViewState) -> Unit>()

    fun currentState(): AnalysisViewState = state.get()

    fun update(newState: AnalysisViewState) {
        state.set(newState)
        listeners.forEach { it(newState) }
        if (newState is AnalysisViewState.Ready) {
            persistToFile(newState.result)
        }
    }

    fun subscribe(listener: (AnalysisViewState) -> Unit): () -> Unit {
        listeners += listener
        listener(state.get())
        return { listeners.remove(listener) }
    }

    private fun persistToFile(result: RiskResult) {
        try {
            val baseDir = project.basePath ?: return
            val dataDir = File(baseDir, ".aicodequalityrisk")
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }
            val jsonFile = File(dataDir, "latest-scan.json")
            val json = com.aicodequalityrisk.plugin.model.AnalysisViewState.toJson(result)
            jsonFile.writeText(json)
        } catch (e: Exception) {
            System.err.println("Failed to persist scan results: ${e.message}")
        }
    }

    fun loadFromFile(): RiskResult? {
        try {
            val baseDir = project.basePath ?: return null
            val jsonFile = File(baseDir, ".aicodequalityrisk/latest-scan.json")
            if (!jsonFile.exists()) return null
            return com.aicodequalityrisk.plugin.model.AnalysisViewState.fromJson(jsonFile.readText())
        } catch (e: Exception) {
            System.err.println("Failed to load scan results: ${e.message}")
            return null
        }
    }

    fun getDataDir(): File? {
        val baseDir = project.basePath ?: return null
        return File(baseDir, ".aicodequalityrisk")
    }

    companion object {
        fun getInstance(project: Project): AnalysisStateStore = project.service()
    }
}