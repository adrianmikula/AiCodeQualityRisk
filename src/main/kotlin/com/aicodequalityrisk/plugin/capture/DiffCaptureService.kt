package com.aicodequalityrisk.plugin.capture

import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.TriggerType
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class DiffCaptureService(
    private val gitDiffRunner: (projectPath: String, args: List<String>) -> String = ::defaultRunGitDiff
) {

    fun captureCurrentContext(project: Project, triggerType: TriggerType): AnalysisInput? {
        val selectedEditor = EditorFactory.getInstance().allEditors
            .firstOrNull { it.project == project } ?: return null

        val document = selectedEditor.document
        val virtualFile = FileDocumentManager.getInstance().getFile(document)
        val filePath = virtualFile?.path
        val snapshot = document.text
        val projectPath = project.basePath ?: ""
        val diff = buildDiff(projectPath, filePath, snapshot)

        return AnalysisInput(
            projectPath = projectPath,
            filePath = filePath,
            trigger = triggerType,
            diffText = diff,
            fileSnapshot = snapshot
        )
    }

    internal fun buildDiff(projectPath: String, filePath: String?, snapshot: String): String {
        if (projectPath.isBlank() || filePath.isNullOrBlank()) {
            return syntheticDiff(filePath, snapshot)
        }

        val relativePath = relativeToProject(projectPath, filePath) ?: return syntheticDiff(filePath, snapshot)
        val staged = runGitDiff(projectPath, listOf("--staged", relativePath))
        if (staged.isNotBlank()) return staged

        val unstaged = runGitDiff(projectPath, listOf(relativePath))
        if (unstaged.isNotBlank()) return unstaged

        return syntheticDiff(filePath, snapshot)
    }

    private fun relativeToProject(projectPath: String, filePath: String): String? {
        return try {
            File(projectPath).toPath().relativize(File(filePath).toPath()).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun runGitDiff(projectPath: String, args: List<String>): String = gitDiffRunner(projectPath, args)

    internal fun syntheticDiff(filePath: String?, snapshot: String): String {
        val name = filePath ?: "unknown.file"
        return buildString {
            appendLine("diff --git a/$name b/$name")
            appendLine("--- a/$name")
            appendLine("+++ b/$name")
            snapshot.lines().forEach { appendLine("+$it") }
        }
    }

    companion object {
        private fun defaultRunGitDiff(projectPath: String, args: List<String>): String {
            return try {
                val command = mutableListOf("git", "diff")
                command.addAll(args)
                val process = ProcessBuilder(command)
                    .directory(File(projectPath))
                    .redirectErrorStream(true)
                    .start()

                val completed = process.waitFor(1200, TimeUnit.MILLISECONDS)
                if (!completed || process.exitValue() != 0) return ""
                process.inputStream.bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                ""
            }
        }
    }
}
