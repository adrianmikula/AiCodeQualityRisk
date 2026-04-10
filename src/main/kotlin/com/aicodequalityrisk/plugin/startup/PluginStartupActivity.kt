package com.aicodequalityrisk.plugin.startup

import com.aicodequalityrisk.plugin.model.TriggerType
import com.aicodequalityrisk.plugin.pipeline.AnalysisOrchestrator
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class PluginStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val orchestrator = project.service<AnalysisOrchestrator>()
        val connection = project.messageBus.connect(project)
        val fire: (TriggerType) -> Unit = { orchestrator.trigger(it) }

        val listener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) = fire(TriggerType.EDIT)
        }

        val multicaster = com.intellij.openapi.editor.EditorFactory.getInstance().eventMulticaster
        multicaster.addDocumentListener(listener, project)
        com.intellij.openapi.editor.EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) = fire(TriggerType.FOCUS)
            },
            project
        )

        connection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) = fire(TriggerType.FOCUS)
            }
        )

        connection.subscribe(
            FileDocumentManagerListener.TOPIC,
            object : FileDocumentManagerListener {
                override fun beforeDocumentSaving(document: com.intellij.openapi.editor.Document) = fire(TriggerType.SAVE)
            }
        )
    }
}
