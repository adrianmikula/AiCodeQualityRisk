package com.aicodequalityrisk.plugin.state

import com.aicodequalityrisk.plugin.model.AnalysisViewState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AnalysisStateStoreTest {
    @Test
    fun `subscriber receives initial state`() {
        val states = mutableListOf<AnalysisViewState>()
        val store = SimpleStateStore()

        store.subscribe { states.add(it) }

        assertEquals(1, states.size)
        assertIs<AnalysisViewState.Idle>(states[0])
    }

    @Test
    fun `update publishes new state`() {
        val states = mutableListOf<AnalysisViewState>()
        val store = SimpleStateStore()

        store.subscribe { states.add(it) }
        store.update(AnalysisViewState.Loading)

        assertEquals(2, states.size)
        assertIs<AnalysisViewState.Loading>(states[1])
    }

    @Test
    fun `unsubscribe stops further updates`() {
        val states = mutableListOf<AnalysisViewState>()
        val store = SimpleStateStore()

        val unsub = store.subscribe { states.add(it) }
        unsub()
        store.update(AnalysisViewState.Loading)

        assertEquals(1, states.size)
    }
}

class SimpleStateStore {
    private val listeners = mutableListOf<(AnalysisViewState) -> Unit>()

    fun subscribe(listener: (AnalysisViewState) -> Unit): () -> Unit {
        listener(AnalysisViewState.Idle)
        listeners.add(listener)
        return { listeners.remove(listener) }
    }

    fun update(state: AnalysisViewState) {
        listeners.forEach { it(state) }
    }
}