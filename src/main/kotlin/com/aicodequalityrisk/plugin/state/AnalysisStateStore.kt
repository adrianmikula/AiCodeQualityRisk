package com.aicodequalityrisk.plugin.state

import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
class AnalysisStateStore {
    private val state = AtomicReference<AnalysisViewState>(AnalysisViewState.Idle)
    private val listeners = CopyOnWriteArrayList<(AnalysisViewState) -> Unit>()

    fun currentState(): AnalysisViewState = state.get()

    fun update(newState: AnalysisViewState) {
        state.set(newState)
        listeners.forEach { it(newState) }
    }

    fun subscribe(listener: (AnalysisViewState) -> Unit): () -> Unit {
        listeners += listener
        listener(state.get())
        return { listeners.remove(listener) }
    }

    companion object {
        fun getInstance(project: Project): AnalysisStateStore = project.service()
    }
}
