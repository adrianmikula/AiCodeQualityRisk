package com.aicodequalityrisk.plugin.capture

import com.aicodequalityrisk.plugin.analysis.ASTAnalyzer
import com.aicodequalityrisk.plugin.analysis.FuzzyMetrics
import com.aicodequalityrisk.plugin.analysis.TreeSitterFuzzyDetector
import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.TriggerType
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class DiffCaptureService(
    private val gitDiffRunner: (projectPath: String, args: List<String>) -> String = ::defaultRunGitDiff,
    private val astAnalyzer: ASTAnalyzer = ASTAnalyzer(),
    private val fuzzyDetector: TreeSitterFuzzyDetector = TreeSitterFuzzyDetector()
) {

    private val logger = Logger.getInstance(DiffCaptureService::class.java)

    fun captureCurrentContext(project: Project, triggerType: TriggerType): AnalysisInput? {
        logger.info("Capture requested for trigger=$triggerType")
        val selectedEditor = FileEditorManager.getInstance(project).selectedTextEditor
            ?: EditorFactory.getInstance().allEditors.firstOrNull { it.project == project }
            ?: return null

        val document = selectedEditor.document
        val virtualFile = FileDocumentManager.getInstance().getFile(document)
        val filePath = virtualFile?.path
        if (shouldSkipAnalysis(filePath)) {
            logger.info("Skipping analysis for unsupported or excluded file: $filePath")
            return null
        }

        val snapshot = document.text
        val projectPath = project.basePath ?: ""
        val diff = buildDiff(projectPath, filePath, snapshot)
        val astMetrics = astAnalyzer.analyzeCode(snapshot)
        val fuzzyMetrics = fuzzyDetector.detect(snapshot, filePath)

        val input = AnalysisInput(
            projectPath = projectPath,
            filePath = filePath,
            trigger = triggerType,
            diffText = diff,
            fileSnapshot = snapshot,
            astMetrics = astMetrics,
            fuzzyMetrics = fuzzyMetrics
        )
        logger.info("Capture produced analysis input for file=$filePath, trigger=$triggerType")
        return input
    }

    internal fun shouldSkipAnalysis(filePath: String?): Boolean {
        val extension = filePath?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()
        return !extension.isNullOrBlank() && excludedFileExtensions.contains(extension)
    }

    fun buildDiff(projectPath: String, filePath: String?, snapshot: String): String {
        if (projectPath.isBlank() || filePath.isNullOrBlank()) {
            logger.debug("Using synthetic diff because projectPath or filePath is blank.")
            return syntheticDiff(filePath, snapshot)
        }

        val relativePath = relativeToProject(projectPath, filePath)
        if (relativePath == null) {
            logger.debug("Relative path could not be resolved for $filePath. Using synthetic diff.")
            return syntheticDiff(filePath, snapshot)
        }
        val staged = runGitDiff(projectPath, listOf("--staged", relativePath))
        if (staged.isNotBlank()) {
            logger.debug("Using staged diff for path=$relativePath")
            return staged
        }

        val unstaged = runGitDiff(projectPath, listOf(relativePath))
        if (unstaged.isNotBlank()) {
            logger.debug("Using unstaged diff for path=$relativePath")
            return unstaged
        }

        logger.debug("Using synthetic diff for path=$relativePath")
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
        private val excludedFileExtensions = setOf("md", "class", "tst", "log", "jar")

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
