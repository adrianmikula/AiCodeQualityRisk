package com.aicodequalityrisk.plugin.ui

import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.aicodequalityrisk.plugin.model.Finding
import com.aicodequalityrisk.plugin.state.AnalysisStateStore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

class RiskToolWindowPanel(project: Project) : JPanel(BorderLayout()) {
    private val scoreLabel = JLabel("Risk score: --")
    private val findingsLabel = JLabel("Findings")
    private val explanationLabel = JLabel("Explanations")
    private val findingsModel = DefaultListModel<String>()
    private val findingsList = JList(findingsModel)
    private val explanationArea = JTextArea()

    init {
        explanationArea.isEditable = false
        explanationArea.lineWrap = true
        explanationArea.wrapStyleWord = true

        val centerPanel = JPanel(BorderLayout())
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.preferredSize = Dimension(0, 120)

        centerPanel.add(findingsLabel, BorderLayout.NORTH)
        centerPanel.add(JScrollPane(findingsList), BorderLayout.CENTER)
        bottomPanel.add(explanationLabel, BorderLayout.NORTH)
        bottomPanel.add(JScrollPane(explanationArea), BorderLayout.CENTER)

        add(scoreLabel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)

        val store = AnalysisStateStore.getInstance(project)
        store.subscribe { render(it) }
    }

    private fun render(state: AnalysisViewState) {
        ApplicationManager.getApplication().invokeLater {
            when (state) {
                AnalysisViewState.Idle -> {
                    scoreLabel.text = "Risk score: --"
                    findingsModel.clear()
                    explanationArea.text = "No analysis yet."
                }

                AnalysisViewState.Loading -> {
                    scoreLabel.text = "Risk score: analyzing..."
                    findingsModel.clear()
                    explanationArea.text = "Running local risk analysis..."
                }

                is AnalysisViewState.Error -> {
                    scoreLabel.text = "Risk score: error"
                    findingsModel.clear()
                    explanationArea.text = state.message
                }

                is AnalysisViewState.Ready -> {
                    val result = state.result
                    scoreLabel.text = "Risk score: ${result.score}/100"
                    renderFindings(result.findings)
                    explanationArea.text = result.explanations.joinToString("\n\n")
                }
            }
        }
    }

    private fun renderFindings(findings: List<Finding>) {
        findingsModel.clear()
        findings.forEach { finding ->
            findingsModel.addElement("[${finding.severity}] ${finding.title}: ${finding.detail}")
        }
    }
}
