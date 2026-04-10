package com.aicodequalityrisk.plugin.pipeline

interface TaskRunner {
    fun submit(task: () -> Unit)
    fun shutdown()
}
