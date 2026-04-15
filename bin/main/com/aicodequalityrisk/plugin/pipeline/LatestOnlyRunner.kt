package com.aicodequalityrisk.plugin.pipeline

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class LatestOnlyRunner(
    private val debounceMs: Long = 350
) : TaskRunner {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val sequence = AtomicLong(0)

    @Volatile
    private var pending: ScheduledFuture<*>? = null

    @Volatile
    private var running: Future<*>? = null

    override fun submit(task: () -> Unit) {
        val id = sequence.incrementAndGet()
        pending?.cancel(false)
        running?.cancel(true)
        pending = scheduler.schedule({
            running = scheduler.submit {
                if (id == sequence.get()) {
                    task()
                }
            }
        }, debounceMs, TimeUnit.MILLISECONDS)
    }

    override fun shutdown() {
        pending?.cancel(true)
        running?.cancel(true)
        scheduler.shutdownNow()
    }
}
