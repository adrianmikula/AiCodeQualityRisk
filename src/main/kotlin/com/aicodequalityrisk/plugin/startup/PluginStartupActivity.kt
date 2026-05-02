package com.aicodequalityrisk.plugin.startup

import com.aicodequalityrisk.plugin.analysis.Category
import com.aicodequalityrisk.plugin.model.TriggerType
import com.aicodequalityrisk.plugin.pipeline.AnalysisOrchestrator
import com.aicodequalityrisk.plugin.ui.RiskToolWindowPanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.ui.content.ContentFactory

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

        if (FileEditorManager.getInstance(project).selectedTextEditor != null) {
            logger.info("Project startup detected active editor; triggering initial risk analysis.")
            fire(TriggerType.FOCUS)
        }

        // Register the "AI Code Risk" tool window programmatically without using ToolWindowFactory
        ApplicationManager.getApplication().invokeLater {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val existingToolWindow = toolWindowManager.getToolWindow("AI Code Risk")
            if (existingToolWindow == null) {
                // Create tool window using the non-deprecated RegisterToolWindowTask API
                val toolWindow = toolWindowManager.registerToolWindow(
                    RegisterToolWindowTask.notClosable(
                        id = "AI Code Risk",
                        anchor = ToolWindowAnchor.RIGHT,
                        icon = com.intellij.openapi.util.IconLoader.getIcon("/icons/expui/pluginIcon@20x20.svg", javaClass)
                    )
                )
                // Add content directly to the tool window
                val content = ContentFactory.getInstance().createContent(RiskToolWindowPanel(project), "", false)
                toolWindow.contentManager.addContent(content)
            }
        }
    }

    companion object {
        private val logger = Logger.getInstance(PluginStartupActivity::class.java)
    }
}
