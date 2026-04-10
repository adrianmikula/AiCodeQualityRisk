package com.aicodequalityrisk.plugin.ui

import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.aicodequalityrisk.plugin.model.Finding
import com.aicodequalityrisk.plugin.state.AnalysisStateStore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

class RiskToolWindowPanel(project: Project) : JPanel(BorderLayout()) {
    private val scorePanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    private val overallScoreLabel = JLabel("Overall Risk Score: --")
    private val complexityLabel = JLabel("Complexity: --")
    private val duplicationLabel = JLabel("Duplication: --")
    private val performanceLabel = JLabel("Performance: --")
    private val securityLabel = JLabel("Security: --")
    private val findingsLabel = JLabel("Findings")
    private val explanationLabel = JLabel("Explanations")
    private val findingsModel = DefaultListModel<String>()
    private val findingsList = JList(findingsModel)
    private val explanationArea = JTextArea()
    private var unsubscribe: (() -> Unit)? = null

    init {
        scorePanel.add(overallScoreLabel)
        scorePanel.add(complexityLabel)
        scorePanel.add(duplicationLabel)
        scorePanel.add(performanceLabel)
        scorePanel.add(securityLabel)

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

        add(scorePanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)

        val store = AnalysisStateStore.getInstance(project)
        unsubscribe = store.subscribe { render(it) }
    }

    override fun removeNotify() {
        unsubscribe?.invoke()
        unsubscribe = null
        super.removeNotify()
    }

    private fun render(state: AnalysisViewState) {
        ApplicationManager.getApplication().invokeLater {
            when (state) {
                AnalysisViewState.Idle -> {
                    overallScoreLabel.text = "Overall Risk Score: --"
                    complexityLabel.text = "Complexity: --"
                    duplicationLabel.text = "Duplication: --"
                    performanceLabel.text = "Performance: --"
                    securityLabel.text = "Security: --"
                    findingsModel.clear()
                    explanationArea.text = "No analysis yet."
                }

                AnalysisViewState.Loading -> {
                    overallScoreLabel.text = "Overall Risk Score: analyzing..."
                    complexityLabel.text = "Complexity: analyzing..."
                    duplicationLabel.text = "Duplication: analyzing..."
                    performanceLabel.text = "Performance: analyzing..."
                    securityLabel.text = "Security: analyzing..."
                    findingsModel.clear()
                    explanationArea.text = "Running local risk analysis..."
                }

                is AnalysisViewState.Error -> {
                    overallScoreLabel.text = "Overall Risk Score: error"
                    complexityLabel.text = "Complexity: error"
                    duplicationLabel.text = "Duplication: error"
                    performanceLabel.text = "Performance: error"
                    securityLabel.text = "Security: error"
                    findingsModel.clear()
                    explanationArea.text = state.message
                }

                is AnalysisViewState.Ready -> {
                    val result = state.result
                    overallScoreLabel.text = "Overall Risk Score: ${result.score}/100"
                    complexityLabel.text = "Complexity: ${result.complexityScore}/100"
                    duplicationLabel.text = "Duplication: ${result.duplicationScore}/100"
                    performanceLabel.text = "Performance: ${result.performanceScore}/100"
                    securityLabel.text = "Security: ${result.securityScore}/100"
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
