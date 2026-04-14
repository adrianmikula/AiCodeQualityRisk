package com.aicodequalityrisk.plugin.ui

import com.aicodequalityrisk.plugin.analysis.Category
import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.aicodequalityrisk.plugin.model.Finding
import com.aicodequalityrisk.plugin.model.LicenseStatus
import com.aicodequalityrisk.plugin.service.LicenseService
import com.aicodequalityrisk.plugin.state.AnalysisStateStore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ide.BrowserUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

class RiskToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val logger = Logger.getInstance(RiskToolWindowPanel::class.java)
    private val licenseService: LicenseService? = try {
        LicenseService.getInstance(project)
    } catch (e: Throwable) {
        logger.warn("LicenseService lookup failed: ${e.message}")
        null
    }
    private val scorePanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)
        minimumSize = Dimension(200, 100)
        preferredSize = Dimension(300, 100)
    }
    private val licenseBanner = JPanel().apply {
        layout = BorderLayout()
        background = Color(255, 245, 230)
        border = javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, Color(200, 180, 150))
        minimumSize = Dimension(200, 36)
        preferredSize = Dimension(300, 36)
    }
    private val licenseLabel = JLabel()
    private val licenseActionButton = JButton()
    private val overallScoreLabel = JLabel("Overall Risk Score: --")
    private val complexityLabel = JLabel("Complexity: --")
    private val duplicationLabel = JLabel("Duplication: --")
    private val performanceLabel = JLabel("Performance: --")
    private val securityLabel = JLabel("Security: --")
    private val findingsLabel = JLabel("Findings")
    private val explanationLabel = JLabel("Explanations")
    private val issuesLabel = JLabel("Issues")
    private val findingsModel = DefaultListModel<String>()
    private val findingsList = JList(findingsModel)
    private val issuesModel = DefaultListModel<String>()
    private val issuesList = JList(issuesModel)
    private val explanationArea = JTextArea()
    private var unsubscribe: (() -> Unit)? = null
    private var currentFindings: List<Finding> = emptyList()
    private var selectedCategory: Category? = null

    init {
        logger.info("RiskToolWindowPanel init start")
        scorePanel.add(overallScoreLabel)
        scorePanel.add(complexityLabel)
        scorePanel.add(duplicationLabel)
        scorePanel.add(performanceLabel)
        scorePanel.add(securityLabel)

        val categoryFont = Font(overallScoreLabel.font.name, Font.BOLD, overallScoreLabel.font.size)
        complexityLabel.font = categoryFont
        duplicationLabel.font = categoryFont
        performanceLabel.font = categoryFont
        securityLabel.font = categoryFont

        listOf(complexityLabel, duplicationLabel, performanceLabel, securityLabel).forEach { label ->
            label.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            label.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    when (label) {
                        complexityLabel -> displayCategoryIssues(Category.COMPLEXITY)
                        duplicationLabel -> displayCategoryIssues(Category.DUPLICATION)
                        performanceLabel -> displayCategoryIssues(Category.PERFORMANCE)
                        securityLabel -> displayCategoryIssues(Category.SECURITY)
                    }
                }
            })
        }

        explanationArea.isEditable = false
        explanationArea.lineWrap = true
        explanationArea.wrapStyleWord = true

        issuesList.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        issuesList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val index = issuesList.locationToIndex(e.point)
                val selected = selectedCategory
                val categoryFindings = selected?.let { currentFindings.filter { finding -> finding.category == it } }.orEmpty()
                if (index >= 0 && index < categoryFindings.size) {
                    openFindingLocation(categoryFindings[index])
                }
            }
        })

        val centerPanel = JPanel(BorderLayout())
        val bottomPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            preferredSize = Dimension(0, 160)
        }

        centerPanel.add(findingsLabel, BorderLayout.NORTH)
        centerPanel.add(JScrollPane(findingsList), BorderLayout.CENTER)

        bottomPanel.add(issuesLabel)
        bottomPanel.add(JScrollPane(issuesList))
        bottomPanel.add(explanationLabel)
        bottomPanel.add(JScrollPane(explanationArea))

        licenseBanner.add(licenseLabel, BorderLayout.CENTER)
        licenseBanner.add(licenseActionButton, BorderLayout.EAST)
        licenseBanner.preferredSize = Dimension(0, 36)

        licenseActionButton.addActionListener {
            licenseService?.getUpgradeUrl()?.let { url ->
                BrowserUtil.browse(url)
            }
        }

        // Restore proper layout
        add(scorePanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)

        // Don't add license banner here - it will be added in updateLicenseBanner()
        
        updateLicenseBanner()

        val store = AnalysisStateStore.getInstance(project)
        unsubscribe = store.subscribe { render(it) }
        logger.info("RiskToolWindowPanel init complete - subscribing to state store")
    }

    override fun removeNotify() {
        unsubscribe?.invoke()
        unsubscribe = null
        super.removeNotify()
    }

    private fun render(state: AnalysisViewState) {
        logger.info("Rendering state: ${state::class.simpleName}")
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
                    logger.info("Updated score labels: overall=${overallScoreLabel.text}")
                    renderFindings(result.findings)
                    explanationArea.text = result.explanations.joinToString("\n\n")
                }
            }
            logger.info("Render complete for state: ${state::class.simpleName}")
        }
    }

    private fun renderFindings(findings: List<Finding>) {
        currentFindings = findings
        findingsModel.clear()
        findings.forEach { finding ->
            findingsModel.addElement("[${finding.severity}] ${finding.title}: ${finding.detail}")
        }
        selectedCategory?.let { displayCategoryIssues(it) }
    }

    private fun displayCategoryIssues(category: Category) {
        selectedCategory = category
        issuesModel.clear()

        val categoryFindings = currentFindings.filter { it.category == category }
        if (categoryFindings.isEmpty()) {
            issuesModel.addElement("No issues found for ${category.name.lowercase().replaceFirstChar { it.titlecase() }}.")
            explanationArea.text = "No file-level issues were detected for this category in the current analysis."
            return
        }

        categoryFindings.forEach { finding ->
            val fileName = finding.filePath?.substringAfterLast('/') ?: "Unknown file"
            val lineInfo = finding.lineNumber?.let { "line $it" } ?: "line unknown"
            issuesModel.addElement("$fileName:$lineInfo - [${finding.severity}] ${finding.title}")
        }

        explanationArea.text = "Click any issue entry to open the affected file in the editor."
    }

    private fun openFindingLocation(finding: Finding) {
        if (licenseService?.isTrialExpired() == true) {
            return
        }
        val path = finding.filePath ?: return
        val file = LocalFileSystem.getInstance().findFileByPath(path) ?: return
        val line = finding.lineNumber?.minus(1) ?: 0
        OpenFileDescriptor(project, file, line, 0).navigate(true)
    }

    private fun updateLicenseBanner() {
        if (licenseService == null) {
            println("DEBUG: licenseService is null, skipping license banner")
            logger.warn("LicenseService is null - skipping banner")
            return
        }
        val status = licenseService.getLicenseStatus()
        logger.info("License status: $status")
        
        // Add license banner at the TOP of scorePanel (first component)
        // This won't conflict with BorderLayout since it's inside scorePanel
        when (status) {
            LicenseStatus.LICENSED -> {
                scorePanel.remove(licenseBanner)
            }
            LicenseStatus.TRIAL -> {
                licenseBanner.background = Color(230, 255, 230)
                licenseLabel.text = "Trial Active - Full Access"
                licenseActionButton.text = "Upgrade Now"
                // Remove and re-add at index 0
                scorePanel.remove(licenseBanner)
                scorePanel.add(licenseBanner, 0)
            }
            LicenseStatus.TRIAL_EXPIRED -> {
                licenseBanner.background = Color(255, 230, 230)
                licenseLabel.text = "Trial Expired - Upgrade to Unlock"
                licenseActionButton.text = "Upgrade Now"
                scorePanel.remove(licenseBanner)
                scorePanel.add(licenseBanner, 0)
            }
            LicenseStatus.UNLICENSED -> {
                licenseBanner.background = Color(255, 245, 230)
                licenseLabel.text = "Start Free Trial for Full Access"
                licenseActionButton.text = "Start Free Trial"
                scorePanel.remove(licenseBanner)
                scorePanel.add(licenseBanner, 0)
            }
        }
        scorePanel.revalidate()
        scorePanel.repaint()
        logger.info("License banner added to scorePanel")
    }
}
