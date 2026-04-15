package com.aicodequalityrisk.plugin.state

import com.aicodequalityrisk.plugin.model.AnalysisViewState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AnalysisStateStoreTest {
    @Test
    fun `subscriber receives initial and subsequent states`() {
        val store = AnalysisStateStore()
        val events = mutableListOf<AnalysisViewState>()

        val unsubscribe = store.subscribe { events += it }
        store.update(AnalysisViewState.Loading)

        assertEquals(2, events.size)
        assertIs<AnalysisViewState.Idle>(events[0])
        assertIs<AnalysisViewState.Loading>(events[1])
        unsubscribe()
    }

    @Test
    fun `unsubscribe stops further updates`() {
        val store = AnalysisStateStore()
        var count = 0

        val unsubscribe = store.subscribe { count += 1 }
        unsubscribe()
        store.update(AnalysisViewState.Loading)

        assertEquals(1, count)
    }
}
