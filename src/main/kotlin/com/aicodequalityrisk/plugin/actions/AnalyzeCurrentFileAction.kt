package com.aicodequalityrisk.plugin.actions

import com.aicodequalityrisk.plugin.model.TriggerType
import com.aicodequalityrisk.plugin.pipeline.AnalysisOrchestrator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class AnalyzeCurrentFileAction(
    private val manualTriggerHandler: (AnActionEvent) -> Unit = { event ->
        val project = event.project
        if (project != null) {
            project.service<AnalysisOrchestrator>().trigger(TriggerType.MANUAL)
        }
    }
) : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        manualTriggerHandler(event)
    }
}
