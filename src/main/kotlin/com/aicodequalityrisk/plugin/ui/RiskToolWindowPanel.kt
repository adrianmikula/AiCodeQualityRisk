package com.aicodequalityrisk.plugin.ui

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
import java.awt.Dimension
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane

class RiskToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val logger = Logger.getInstance(RiskToolWindowPanel::class.java)
    private val licenseService: LicenseService? = try {
        LicenseService.getInstance(project)
    } catch (e: Throwable) {
        logger.warn("LicenseService lookup failed: ${e.message}")
        null
    }
    private val SCORE_GREEN = Color(34, 139, 34)
    private val SCORE_YELLOW = Color(218, 165, 32)
    private val SCORE_ORANGE = Color(255, 140, 0)
    private val SCORE_RED = Color(178, 34, 34)

    private fun getScoreColor(score: Int): Color = when {
        score <= 10 -> SCORE_GREEN
        score <= 25 -> SCORE_YELLOW
        score <= 50 -> SCORE_ORANGE
        else -> SCORE_RED
    }

    private val scorePanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)
        minimumSize = Dimension(200, 300)
        preferredSize = Dimension(400, 300)
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
    private val boilerplateBloatLabel = JLabel("Boilerplate Bloat: --")
    private val verboseCommentSpamLabel = JLabel("Verbose Comments: --")
    private val overDefensiveLabel = JLabel("Over-Defensive: --")
    private val magicNumbersLabel = JLabel("Magic Numbers: --")
    private val complexBooleanLabel = JLabel("Complex Boolean: --")
    private val deepNestingLabel = JLabel("Deep Nesting: --")
    private val verboseLoggingLabel = JLabel("Verbose Logging: --")
    private val poorNamingLabel = JLabel("Poor Naming: --")
    private val frameworkMisuseLabel = JLabel("Framework Misuse: --")
    private val excessiveDocsLabel = JLabel("Excessive Docs: --")
    private val findingsLabel = JLabel("Findings")
    private val findingsModel = DefaultListModel<String>()
    private val findingsList = JList(findingsModel)
    private var unsubscribe: (() -> Unit)? = null

    init {
        logger.info("RiskToolWindowPanel init start")
        scorePanel.add(overallScoreLabel)
        scorePanel.add(complexityLabel)
        scorePanel.add(duplicationLabel)
        scorePanel.add(performanceLabel)
        scorePanel.add(securityLabel)
        scorePanel.add(boilerplateBloatLabel)
        scorePanel.add(verboseCommentSpamLabel)
        scorePanel.add(overDefensiveLabel)
        scorePanel.add(magicNumbersLabel)
        scorePanel.add(complexBooleanLabel)
        scorePanel.add(deepNestingLabel)
        scorePanel.add(verboseLoggingLabel)
        scorePanel.add(poorNamingLabel)
        scorePanel.add(frameworkMisuseLabel)
        scorePanel.add(excessiveDocsLabel)

        val categoryFont = Font(overallScoreLabel.font.name, Font.BOLD, overallScoreLabel.font.size)
        complexityLabel.font = categoryFont
        duplicationLabel.font = categoryFont
        performanceLabel.font = categoryFont
        securityLabel.font = categoryFont
        boilerplateBloatLabel.font = categoryFont
        verboseCommentSpamLabel.font = categoryFont
        overDefensiveLabel.font = categoryFont
        magicNumbersLabel.font = categoryFont
        complexBooleanLabel.font = categoryFont
        deepNestingLabel.font = categoryFont
        verboseLoggingLabel.font = categoryFont
        poorNamingLabel.font = categoryFont
        frameworkMisuseLabel.font = categoryFont
        excessiveDocsLabel.font = categoryFont

        val centerPanel = JPanel(BorderLayout())
        val bottomPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            preferredSize = Dimension(0, 0)
        }

        centerPanel.add(findingsLabel, BorderLayout.NORTH)
        centerPanel.add(JScrollPane(findingsList), BorderLayout.CENTER)

        licenseBanner.add(licenseLabel, BorderLayout.CENTER)
        licenseBanner.add(licenseActionButton, BorderLayout.EAST)
        licenseBanner.preferredSize = Dimension(0, 36)

        licenseActionButton.addActionListener {
            val status = licenseService?.getLicenseStatus()
            if (status == LicenseStatus.UNLICENSED) {
                licenseService?.startTrial()
                updateLicenseBanner()
                scorePanel.revalidate()
                scorePanel.repaint()
                licenseService?.let { service ->
                    val store = AnalysisStateStore.getInstance(project)
                    store.update(AnalysisViewState.Idle)
                }
            } else {
                licenseService?.getUpgradeUrl()?.let { url ->
                    BrowserUtil.browse(url)
                }
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
        val isLocked = licenseService?.isLocked() ?: true
        ApplicationManager.getApplication().invokeLater {
            if (isLocked) {
                val status = licenseService?.getLicenseStatus()
                val lockedMessage = when (status) {
                    LicenseStatus.UNLICENSED -> "Click 'Start Free Trial' to see analysis"
                    LicenseStatus.TRIAL_EXPIRED -> "Trial expired - Upgrade to unlock"
                    else -> "License required"
                }
                overallScoreLabel.text = "Overall Risk Score: $lockedMessage"
                overallScoreLabel.foreground = Color.GRAY
                complexityLabel.text = "Complexity: --"
                duplicationLabel.text = "Duplication: --"
                performanceLabel.text = "Performance: --"
                securityLabel.text = "Security: --"
                boilerplateBloatLabel.text = "Boilerplate Bloat: --"
                verboseCommentSpamLabel.text = "Verbose Comments: --"
                overDefensiveLabel.text = "Over-Defensive: --"
                magicNumbersLabel.text = "Magic Numbers: --"
                complexBooleanLabel.text = "Complex Boolean: --"
                deepNestingLabel.text = "Deep Nesting: --"
                verboseLoggingLabel.text = "Verbose Logging: --"
                poorNamingLabel.text = "Poor Naming: --"
                frameworkMisuseLabel.text = "Framework Misuse: --"
                excessiveDocsLabel.text = "Excessive Docs: --"
                findingsModel.clear()
            } else {
                when (state) {
                    is AnalysisViewState.Idle -> {
                        overallScoreLabel.text = "Overall Risk Score: --"
                        complexityLabel.text = "Complexity: --"
                        duplicationLabel.text = "Duplication: --"
                        performanceLabel.text = "Performance: --"
                        securityLabel.text = "Security: --"
                        boilerplateBloatLabel.text = "Boilerplate Bloat: --"
                        verboseCommentSpamLabel.text = "Verbose Comments: --"
                        overDefensiveLabel.text = "Over-Defensive: --"
                        magicNumbersLabel.text = "Magic Numbers: --"
                        complexBooleanLabel.text = "Complex Boolean: --"
                        deepNestingLabel.text = "Deep Nesting: --"
                        verboseLoggingLabel.text = "Verbose Logging: --"
                        poorNamingLabel.text = "Poor Naming: --"
                        frameworkMisuseLabel.text = "Framework Misuse: --"
                        excessiveDocsLabel.text = "Excessive Docs: --"
                        findingsModel.clear()
                    }

                    is AnalysisViewState.Loading -> {
                        overallScoreLabel.text = "Overall Risk Score: analyzing..."
                        complexityLabel.text = "Complexity: analyzing..."
                        duplicationLabel.text = "Duplication: analyzing..."
                        performanceLabel.text = "Performance: analyzing..."
                        securityLabel.text = "Security: analyzing..."
                        boilerplateBloatLabel.text = "Boilerplate Bloat: analyzing..."
                        verboseCommentSpamLabel.text = "Verbose Comments: analyzing..."
                        overDefensiveLabel.text = "Over-Defensive: analyzing..."
                        magicNumbersLabel.text = "Magic Numbers: analyzing..."
                        complexBooleanLabel.text = "Complex Boolean: analyzing..."
                        deepNestingLabel.text = "Deep Nesting: analyzing..."
                        verboseLoggingLabel.text = "Verbose Logging: analyzing..."
                        poorNamingLabel.text = "Poor Naming: analyzing..."
                        frameworkMisuseLabel.text = "Framework Misuse: analyzing..."
                        excessiveDocsLabel.text = "Excessive Docs: analyzing..."
                        findingsModel.clear()
                    }

                    is AnalysisViewState.Error -> {
                        overallScoreLabel.text = "Overall Risk Score: error"
                        complexityLabel.text = "Complexity: error"
                        duplicationLabel.text = "Duplication: error"
                        performanceLabel.text = "Performance: error"
                        securityLabel.text = "Security: error"
                        boilerplateBloatLabel.text = "Boilerplate Bloat: error"
                        verboseCommentSpamLabel.text = "Verbose Comments: error"
                        overDefensiveLabel.text = "Over-Defensive: error"
                        magicNumbersLabel.text = "Magic Numbers: error"
                        complexBooleanLabel.text = "Complex Boolean: error"
                        deepNestingLabel.text = "Deep Nesting: error"
                        verboseLoggingLabel.text = "Verbose Logging: error"
                        poorNamingLabel.text = "Poor Naming: error"
                        frameworkMisuseLabel.text = "Framework Misuse: error"
                        excessiveDocsLabel.text = "Excessive Docs: error"
                        findingsModel.clear()
                    }

                    is AnalysisViewState.Ready -> {
                        val result = state.result
                        overallScoreLabel.text = "Overall Risk Score: ${result.score}/100"
                        overallScoreLabel.foreground = getScoreColor(result.score)
                        complexityLabel.text = "Complexity: ${result.complexityScore}/100"
                        complexityLabel.foreground = getScoreColor(result.complexityScore)
                        duplicationLabel.text = "Duplication: ${result.duplicationScore}/100"
                        duplicationLabel.foreground = getScoreColor(result.duplicationScore)
                        performanceLabel.text = "Performance: ${result.performanceScore}/100"
                        performanceLabel.foreground = getScoreColor(result.performanceScore)
                        securityLabel.text = "Security: ${result.securityScore}/100"
                        securityLabel.foreground = getScoreColor(result.securityScore)
                        boilerplateBloatLabel.text = "Boilerplate Bloat: ${result.boilerplateBloatScore}/100"
                        boilerplateBloatLabel.foreground = getScoreColor(result.boilerplateBloatScore)
                        verboseCommentSpamLabel.text = "Verbose Comments: ${result.verboseCommentSpamScore}/100"
                        verboseCommentSpamLabel.foreground = getScoreColor(result.verboseCommentSpamScore)
                        overDefensiveLabel.text = "Over-Defensive: ${result.overDefensiveProgrammingScore}/100"
                        overDefensiveLabel.foreground = getScoreColor(result.overDefensiveProgrammingScore)
                        magicNumbersLabel.text = "Magic Numbers: ${result.magicNumbersScore}/100"
                        magicNumbersLabel.foreground = getScoreColor(result.magicNumbersScore)
                        complexBooleanLabel.text = "Complex Boolean: ${result.complexBooleanLogicScore}/100"
                        complexBooleanLabel.foreground = getScoreColor(result.complexBooleanLogicScore)
                        deepNestingLabel.text = "Deep Nesting: ${result.deepNestingScore}/100"
                        deepNestingLabel.foreground = getScoreColor(result.deepNestingScore)
                        verboseLoggingLabel.text = "Verbose Logging: ${result.verboseLoggingScore}/100"
                        verboseLoggingLabel.foreground = getScoreColor(result.verboseLoggingScore)
                        poorNamingLabel.text = "Poor Naming: ${result.poorNamingScore}/100"
                        poorNamingLabel.foreground = getScoreColor(result.poorNamingScore)
                        frameworkMisuseLabel.text = "Framework Misuse: ${result.frameworkMisuseScore}/100"
                        frameworkMisuseLabel.foreground = getScoreColor(result.frameworkMisuseScore)
                        excessiveDocsLabel.text = "Excessive Docs: ${result.excessiveDocumentationScore}/100"
                        excessiveDocsLabel.foreground = getScoreColor(result.excessiveDocumentationScore)
                        logger.info("Updated score labels: overall=${overallScoreLabel.text}")
                        renderFindings(result.findings)
                    }
                }
            }
            logger.info("Render complete for state: ${state::class.simpleName}")
        }
    }

    private fun renderFindings(findings: List<Finding>) {
        findingsModel.clear()
        findings.forEach { finding ->
            findingsModel.addElement("[${finding.severity}] ${finding.title}: ${finding.detail}")
        }
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
