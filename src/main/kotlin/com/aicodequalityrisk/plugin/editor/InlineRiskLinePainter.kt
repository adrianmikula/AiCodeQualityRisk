package com.aicodequalityrisk.plugin.editor

import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.aicodequalityrisk.plugin.state.AnalysisStateStore
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import java.awt.Font

class InlineRiskLinePainter : com.intellij.openapi.editor.EditorLinePainter() {
    override fun getLineExtensions(
        project: Project,
        file: com.intellij.openapi.vfs.VirtualFile,
        lineNumber: Int
    ): MutableCollection<LineExtensionInfo>? {
        if (lineNumber != 0) return null
        val state = AnalysisStateStore.getInstance(project).currentState()
        if (state !is AnalysisViewState.Ready) return null
        if (state.result.sourceFilePath != file.path) return null

        val attrs = TextAttributes().apply {
            foregroundColor = colorForScore(state.result.score)
            fontType = Font.BOLD
        }
        val text = "   [AI Risk ${state.result.score}/100]"
        return mutableListOf(LineExtensionInfo(text, attrs))
    }

    private fun colorForScore(score: Int) = when {
        score >= 70 -> JBColor.RED
        score >= 40 -> JBColor.ORANGE
        else -> JBColor.GREEN
    }
}
