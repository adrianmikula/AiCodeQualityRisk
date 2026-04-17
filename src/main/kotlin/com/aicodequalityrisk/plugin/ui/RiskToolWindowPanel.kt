package com.aicodequalityrisk.plugin.ui

import com.aicodequalityrisk.plugin.analysis.Category
import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.aicodequalityrisk.plugin.model.Finding
import com.aicodequalityrisk.plugin.model.LicenseStatus
import com.aicodequalityrisk.plugin.model.Severity
import com.aicodequalityrisk.plugin.service.LicenseService
import com.aicodequalityrisk.plugin.state.AnalysisStateStore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ide.BrowserUtil
import com.intellij.ui.JBColor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn

class RiskToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val logger = Logger.getInstance(RiskToolWindowPanel::class.java)
    private val licenseService: LicenseService? = try {
        LicenseService.getInstance(project)
    } catch (e: Throwable) {
        logger.warn("LicenseService lookup failed: ${e.message}")
        null
    }
    private val SCORE_GREEN = JBColor(Color(34, 139, 34), Color(60, 179, 113))
    private val SCORE_YELLOW = JBColor(Color(218, 165, 32), Color(255, 215, 0))
    private val SCORE_ORANGE = JBColor(Color(255, 140, 0), Color(255, 165, 0))
    private val SCORE_RED = JBColor(Color(178, 34, 34), Color(205, 92, 92))

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
        background = JBColor(Color(255, 245, 230), Color(80, 75, 60))
        border = javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor(Color(200, 180, 150), Color(100, 90, 70)))
        minimumSize = UIConfig.LICENSE_BANNER_MIN
        preferredSize = UIConfig.LICENSE_BANNER_PREFERRED
        maximumSize = UIConfig.LICENSE_BANNER_MAX
    }
    private val licenseLabel = JLabel()
    private val licenseActionButton = JButton().apply {
        maximumSize = UIConfig.LICENSE_BUTTON_MAX
        minimumSize = UIConfig.LICENSE_BUTTON_MIN
        preferredSize = UIConfig.LICENSE_BUTTON_PREFERRED
    }
    private val licenseActionButtonWrapper = JPanel().apply {
        layout = BorderLayout()
        isOpaque = false
        preferredSize = UIConfig.LICENSE_BUTTON_WRAPPER_PREFERRED
        maximumSize = UIConfig.LICENSE_BUTTON_WRAPPER_MAX
        minimumSize = UIConfig.LICENSE_BUTTON_WRAPPER_MIN
        add(licenseActionButton, BorderLayout.CENTER)
    }
    private val overallScoreLabel = JLabel("Overall Risk Score: --")
    private val complexityLabel = JLabel("Complexity: --")
    private val duplicationLabel = JLabel("Duplication: --")
    private val performanceLabel = JLabel("Performance: --")
    private val securityLabel = JLabel("Security: --")
    private val findingsLabel = JLabel("Scan Results")
    private val findingsContainer = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    private val findingsCacheByCategory = mutableMapOf<Category, List<Finding>>()
    private val categoryExpanded = mutableMapOf<Category, Boolean>().apply {
        Category.entries.forEach { this[it] = true }
    }
    private var unsubscribe: (() -> Unit)? = null
    private var findingsCache: List<Finding> = emptyList()

    init {
        logger.info("RiskToolWindowPanel init start")
        scorePanel.add(overallScoreLabel)
        scorePanel.add(complexityLabel)
        scorePanel.add(duplicationLabel)
        scorePanel.add(performanceLabel)
        scorePanel.add(securityLabel)

        val categoryFont = Font(overallScoreLabel.font.name, Font.BOLD, overallScoreLabel.font.size)
        overallScoreLabel.font = Font(overallScoreLabel.font.name, Font.BOLD, overallScoreLabel.font.size * 2)
        complexityLabel.font = categoryFont
        duplicationLabel.font = categoryFont
        performanceLabel.font = categoryFont
        securityLabel.font = categoryFont

        val centerPanel = JPanel(BorderLayout())
        val bottomPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            preferredSize = Dimension(0, 0)
        }

        val findingsScrollPane = JScrollPane(findingsContainer)
        findingsScrollPane.preferredSize = Dimension(600, 300)

        centerPanel.add(findingsLabel, BorderLayout.NORTH)
        centerPanel.add(findingsScrollPane, BorderLayout.CENTER)

        licenseBanner.add(licenseLabel, BorderLayout.CENTER)
        licenseBanner.add(licenseActionButtonWrapper, BorderLayout.EAST)

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
                overallScoreLabel.text = "<html>Overall Risk Score: $lockedMessage</html>"
                overallScoreLabel.foreground = Color.GRAY
                complexityLabel.text = "Complexity: --"
                duplicationLabel.text = "Duplication: --"
                performanceLabel.text = "Performance: --"
                securityLabel.text = "Security: --"
                renderFindings(emptyList())
            } else {
                when (state) {
                    is AnalysisViewState.Idle -> {
                        overallScoreLabel.text = "<html>Overall Risk Score: --</html>"
                        complexityLabel.text = "Complexity: --"
                        duplicationLabel.text = "Duplication: --"
                        performanceLabel.text = "Performance: --"
                        securityLabel.text = "Security: --"
                        renderFindings(emptyList())
                    }

                    is AnalysisViewState.Loading -> {
                        overallScoreLabel.text = "<html>Overall Risk Score: analyzing...</html>"
                        complexityLabel.text = "Complexity: analyzing..."
                        duplicationLabel.text = "Duplication: analyzing..."
                        performanceLabel.text = "Performance: analyzing..."
                        securityLabel.text = "Security: analyzing..."
                        renderFindings(emptyList())
                    }

                    is AnalysisViewState.Error -> {
                        overallScoreLabel.text = "<html>Overall Risk Score: error</html>"
                        complexityLabel.text = "Complexity: error"
                        duplicationLabel.text = "Duplication: error"
                        performanceLabel.text = "Performance: error"
                        securityLabel.text = "Security: error"
                        renderFindings(emptyList())
                    }

                    is AnalysisViewState.Ready -> {
                        val result = state.result
                        overallScoreLabel.text = "<html>Overall Risk Score: ${result.score}/100</html>"
                        overallScoreLabel.foreground = getScoreColor(result.score)
                        complexityLabel.text = "Complexity: ${result.complexityConsolidated}/100"
                        complexityLabel.foreground = getScoreColor(result.complexityConsolidated)
                        duplicationLabel.text = "Duplication: ${result.duplicationConsolidated}/100"
                        duplicationLabel.foreground = getScoreColor(result.duplicationConsolidated)
                        performanceLabel.text = "Performance: ${result.performanceConsolidated}/100"
                        performanceLabel.foreground = getScoreColor(result.performanceConsolidated)
                        securityLabel.text = "Security: ${result.securityConsolidated}/100"
                        securityLabel.foreground = getScoreColor(result.securityConsolidated)
                        logger.info("Updated score labels: overall=${overallScoreLabel.text}")
                        renderFindings(result.findings)
                    }
                }
            }
            logger.info("Render complete for state: ${state::class.simpleName}")
        }
    }

    private fun renderFindings(findings: List<Finding>) {
        findingsCache = findings
        findingsContainer.removeAll()
        
        val grouped = findings.groupBy { it.category }
        findingsCacheByCategory.clear()
        findingsCacheByCategory.putAll(grouped)
        
        Category.entries.forEach { category ->
            val categoryFindings = grouped[category] ?: emptyList()
            val accordionPanel = createAccordionSection(category, categoryFindings)
            findingsContainer.add(accordionPanel)
        }
        
        findingsContainer.revalidate()
        findingsContainer.repaint()
    }

    private fun createAccordionSection(category: Category, findings: List<Finding>): JPanel {
        val isExpanded = categoryExpanded[category] ?: true
        
        val headerPanel = JPanel(BorderLayout()).apply {
            background = JBColor(Color(240, 240, 245), Color(60, 63, 66))
            border = javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, JBColor(Color(200, 200, 200), Color(80, 80, 80)))
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            isOpaque = true
        }
        
        val toggleIcon = if (isExpanded) "▼" else "▶"
        val headerLabel = JLabel("$toggleIcon ${category.name} (${findings.size} findings)").apply {
            font = Font("SansSerif", Font.BOLD, 13)
            border = javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12)
        }
        
        val contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isVisible = isExpanded
        }
        
        headerPanel.add(headerLabel, BorderLayout.CENTER)
        
        headerPanel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val newExpanded = !(categoryExpanded[category] ?: true)
                categoryExpanded[category] = newExpanded
                
                val newToggleIcon = if (newExpanded) "▼" else "▶"
                headerLabel.text = "$newToggleIcon ${category.name} (${findings.size} findings)"
                contentPanel.isVisible = newExpanded
                
                revalidate()
                repaint()
            }
        })
        
        if (findings.isNotEmpty()) {
            val tableModel = DefaultTableModel(arrayOf("File", "Issue", "Severity"), 0)
            val table = JTable(tableModel)
            
            table.columnModel.getColumn(0).preferredWidth = 150
            table.columnModel.getColumn(1).preferredWidth = 350
            table.columnModel.getColumn(2).preferredWidth = 80
            table.columnModel.getColumn(2).maxWidth = 100
            table.setRowHeight(24)
            table.setFont(Font("SansSerif", Font.PLAIN, 12))
            table.setDefaultRenderer(Object::class.java, SeverityCellRenderer())
            table.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 1) {
                        val row = table.selectedRow
                        if (row >= 0 && row < findings.size) {
                            openFindingLocation(findings[row])
                        }
                    }
                }
            })
            
            findings.forEach { finding ->
                val fileName = finding.filePath?.substringAfterLast("/") ?: "-"
                val issue = "${finding.title}: ${finding.detail}"
                tableModel.addRow(arrayOf(fileName, issue, finding.severity.name))
            }
            
            val tableScrollPane = JScrollPane(table)
            tableScrollPane.preferredSize = Dimension(580, (findings.size.coerceAtMost(5) * 24 + 30))
            contentPanel.add(tableScrollPane)
        } else {
            val emptyLabel = JLabel("No findings").apply {
                font = Font("SansSerif", Font.ITALIC, 11)
                foreground = Color.GRAY
                border = javax.swing.BorderFactory.createEmptyBorder(8, 40, 8, 12)
            }
            contentPanel.add(emptyLabel)
        }
        
        return JPanel(BorderLayout()).apply {
            add(headerPanel, BorderLayout.NORTH)
            add(contentPanel, BorderLayout.CENTER)
            border = javax.swing.BorderFactory.createEmptyBorder(2, 0, 2, 0)
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

    private fun getSeverityColor(severity: Severity): Color = when (severity) {
        Severity.LOW -> SCORE_GREEN
        Severity.MEDIUM -> SCORE_ORANGE
        Severity.HIGH -> SCORE_RED
    }

    private inner class SeverityCellRenderer : TableCellRenderer {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): java.awt.Component {
            val label = JLabel(value?.toString() ?: "")
            label.font = Font("SansSerif", Font.BOLD, 12)
            label.horizontalAlignment = JLabel.CENTER

            if (column == 2) {
                try {
                    label.foreground = JBColor.white
                    label.background = getSeverityColor(Severity.valueOf(value.toString()))
                    label.isOpaque = true
                } catch (e: Exception) {
                    label.background = JBColor(Color.LIGHT_GRAY, Color.darkGray)
                    label.isOpaque = true
                }
            } else {
                label.background = if (isSelected) JBColor(Color(51, 153, 255), Color(80, 80, 80)) else JBColor.white
                label.foreground = if (isSelected) Color.WHITE else JBColor.black
                label.isOpaque = true
            }
            return label
        }
    }
}
