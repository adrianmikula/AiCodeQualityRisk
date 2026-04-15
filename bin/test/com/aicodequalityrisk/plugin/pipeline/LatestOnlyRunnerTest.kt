package com.aicodequalityrisk.plugin.pipeline

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LatestOnlyRunnerTest {
    @Test
    fun `only latest submitted task executes`() {
        val runner = LatestOnlyRunner(debounceMs = 20)
        val executed = CopyOnWriteArrayList<Int>()
        val latch = CountDownLatch(1)

        runner.submit { executed += 1 }
        runner.submit {
            executed += 2
            latch.countDown()
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(listOf(2), executed.toList())
        runner.shutdown()
    }
}
