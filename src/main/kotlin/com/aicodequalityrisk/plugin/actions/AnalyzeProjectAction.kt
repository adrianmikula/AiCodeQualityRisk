package com.aicodequalityrisk.plugin.actions

import com.aicodequalityrisk.plugin.model.TriggerType
import com.aicodequalityrisk.plugin.pipeline.AnalysisOrchestrator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

/**
 * Placeholder action for project-wide analysis.
 * TODO: implement proper project-level analysis trigger distinguish from file-level.
 */
class AnalyzeProjectAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        project.service<AnalysisOrchestrator>().trigger(TriggerType.MANUAL)
    }
}
