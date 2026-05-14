package com.aicodequalityrisk.plugin.ui

import com.aicodequalityrisk.plugin.analysis.Category
import com.aicodequalityrisk.plugin.ui.UIStrings
import com.aicodequalityrisk.plugin.model.AnalysisViewState
import com.aicodequalityrisk.plugin.model.Finding
import com.aicodequalityrisk.plugin.model.LicenseStatus
import com.aicodequalityrisk.plugin.model.Severity
import com.aicodequalityrisk.plugin.service.LicenseService
import com.aicodequalityrisk.plugin.state.AnalysisStateStore
import com.aicodequalityrisk.plugin.mcp.McpServerService
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

    private fun getScoreColor(score: Int): Color = when {
        score <= 10 -> UIConfig.SCORE_GREEN
        score <= 25 -> UIConfig.SCORE_YELLOW
        score <= 50 -> UIConfig.SCORE_ORANGE
        else -> UIConfig.SCORE_RED
    }

    private val scorePanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)
        minimumSize = Dimension(200, 300)
        preferredSize = Dimension(400, 300)
    }

    private val licenseBannerFiller = JPanel().apply {
        isOpaque = false
        minimumSize = Dimension(0, 0)
        maximumSize = Dimension(Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt())
    }
    private val licenseBanner = JPanel().apply {
        layout = BorderLayout()
        background = UIConfig.LICENSE_UNLICENSED_BG
        border = javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor(UIConfig.BANNER_BORDER_LIGHT, UIConfig.BANNER_BORDER_DARK))
        minimumSize = UIConfig.LICENSE_BANNER_MIN
        preferredSize = UIConfig.LICENSE_BANNER_PREFERRED
        maximumSize = UIConfig.LICENSE_BANNER_MAX
        add(licenseBannerFiller, BorderLayout.CENTER)
    }
    private val licenseLabel = JLabel().apply {
        border = javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 0)
    }
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
    private val overallScoreLabel = JLabel(UIStrings.scoreOverall("--"))
    private val complexityLabel = JLabel(UIStrings.scoreComplexity("--"))
    private val duplicationLabel = JLabel(UIStrings.scoreDuplication("--"))
    private val performanceLabel = JLabel(UIStrings.scorePerformance("--"))
    private val securityLabel = JLabel(UIStrings.scoreSecurity("--"))
    private val lastAnalysisLabel = JLabel("")
    private val filesAnalyzedLabel = JLabel("")
    private val findingsLabel = JLabel(UIStrings.FINDINGS_LABEL)
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
        scorePanel.add(lastAnalysisLabel)
        scorePanel.add(filesAnalyzedLabel)

        val categoryFont = Font(overallScoreLabel.font.name, Font.BOLD, overallScoreLabel.font.size)
        val smallGrayFont = Font(overallScoreLabel.font.name, Font.ITALIC, overallScoreLabel.font.size - 2)
        overallScoreLabel.font = Font(overallScoreLabel.font.name, Font.BOLD, overallScoreLabel.font.size * 2)
        complexityLabel.font = categoryFont
        duplicationLabel.font = categoryFont
        performanceLabel.font = categoryFont
        securityLabel.font = categoryFont
        lastAnalysisLabel.font = smallGrayFont
        lastAnalysisLabel.foreground = Color.GRAY
        filesAnalyzedLabel.font = smallGrayFont
        filesAnalyzedLabel.foreground = Color.GRAY

        val centerPanel = JPanel(BorderLayout())
        val bottomPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            preferredSize = Dimension(0, 0)
        }

        val findingsScrollPane = JScrollPane(findingsContainer)
        findingsScrollPane.preferredSize = Dimension(600, 300)

        centerPanel.add(findingsLabel, BorderLayout.NORTH)
        centerPanel.add(findingsScrollPane, BorderLayout.CENTER)

        licenseBanner.add(licenseLabel, BorderLayout.WEST)
        licenseBanner.add(licenseActionButtonWrapper, BorderLayout.EAST)

        licenseActionButton.addActionListener {
            val status = licenseService?.getLicenseStatus()
            if (status == LicenseStatus.UNLICENSED) {
                // Redirect to Marketplace for trial initiation
                licenseService?.getUpgradeUrl()?.let { url ->
                    BrowserUtil.browse(url)
                }
            } else {
                licenseService?.getUpgradeUrl()?.let { url ->
                    BrowserUtil.browse(url)
                }
            }
        }

        add(scorePanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)
        
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
                    LicenseStatus.UNLICENSED -> UIStrings.LICENSE_UNLICENSED_MESSAGE
                    LicenseStatus.TRIAL_EXPIRED -> UIStrings.LICENSE_TRIAL_EXPIRED_MESSAGE
                    else -> UIStrings.LICENSE_REQUIRED_MESSAGE
                }
                overallScoreLabel.text = "<html>Overall Risk Score: $lockedMessage</html>"
                overallScoreLabel.foreground = Color.GRAY
                complexityLabel.text = UIStrings.scoreComplexity("--")
                duplicationLabel.text = UIStrings.scoreDuplication("--")
                performanceLabel.text = UIStrings.scorePerformance("--")
                securityLabel.text = UIStrings.scoreSecurity("--")
                renderFindings(emptyList())
            } else {
                when (state) {
                    is AnalysisViewState.Idle -> {
                        overallScoreLabel.text = "<html>Overall Risk Score: ${UIStrings.STATUS_WAITING}</html>"
                        overallScoreLabel.foreground = Color.GRAY
                        complexityLabel.text = UIStrings.scoreComplexity("--")
                        duplicationLabel.text = UIStrings.scoreDuplication("--")
                        performanceLabel.text = UIStrings.scorePerformance("--")
                        securityLabel.text = UIStrings.scoreSecurity("--")
                        lastAnalysisLabel.text = ""
                        filesAnalyzedLabel.text = ""
                        renderFindings(emptyList())
                    }

                    is AnalysisViewState.Loading -> {
                        overallScoreLabel.text = "<html>Overall Risk Score: ${UIStrings.STATUS_ANALYZING}</html>"
                        complexityLabel.text = UIStrings.scoreComplexity(UIStrings.STATUS_ANALYZING)
                        duplicationLabel.text = UIStrings.scoreDuplication(UIStrings.STATUS_ANALYZING)
                        performanceLabel.text = UIStrings.scorePerformance(UIStrings.STATUS_ANALYZING)
                        securityLabel.text = UIStrings.scoreSecurity(UIStrings.STATUS_ANALYZING)
                        lastAnalysisLabel.text = ""
                        filesAnalyzedLabel.text = ""
                        renderFindings(emptyList())
                    }

                    is AnalysisViewState.Error -> {
                        overallScoreLabel.text = "<html>Overall Risk Score: ${UIStrings.STATUS_ERROR}</html>"
                        complexityLabel.text = UIStrings.scoreComplexity(UIStrings.STATUS_ERROR)
                        duplicationLabel.text = UIStrings.scoreDuplication(UIStrings.STATUS_ERROR)
                        performanceLabel.text = UIStrings.scorePerformance(UIStrings.STATUS_ERROR)
                        securityLabel.text = UIStrings.scoreSecurity(UIStrings.STATUS_ERROR)
                        lastAnalysisLabel.text = ""
                        filesAnalyzedLabel.text = ""
                        renderFindings(emptyList())
                    }

                    is AnalysisViewState.Ready -> {
                        val result = state.result
                        overallScoreLabel.text = "<html>Overall Risk Score: ${result.score}/100</html>"
                        overallScoreLabel.foreground = getScoreColor(result.score)
                        complexityLabel.text = UIStrings.scoreComplexity("${result.complexityConsolidated}/100")
                        complexityLabel.foreground = getScoreColor(result.complexityConsolidated)
                        duplicationLabel.text = UIStrings.scoreDuplication("${result.duplicationConsolidated}/100")
                        duplicationLabel.foreground = getScoreColor(result.duplicationConsolidated)
                        performanceLabel.text = UIStrings.scorePerformance("${result.performanceConsolidated}/100")
                        performanceLabel.foreground = getScoreColor(result.performanceConsolidated)
                        securityLabel.text = UIStrings.scoreSecurity("${result.securityConsolidated}/100")
                        securityLabel.foreground = getScoreColor(result.securityConsolidated)
                        lastAnalysisLabel.text = UIStrings.timeLastAnalysis(formatRelativeTime(result.timestamp))
                        val fileCount = McpServerService.getInstance(project).getAnalyzedFileCount()
                        filesAnalyzedLabel.text = UIStrings.timeFilesAnalyzed(fileCount)
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
            background = UIConfig.ACCORDION_HEADER_BG
            border = javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, UIConfig.ACCORDION_BORDER)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            isOpaque = true
        }
        
        val toggleIcon = if (isExpanded) "▼" else "▶"
        val headerLabel = JLabel("$toggleIcon ${category.name} (${UIStrings.findingsCount(findings.size)})").apply {
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
                headerLabel.text = "$newToggleIcon ${category.name} (${UIStrings.findingsCount(findings.size)})"
                contentPanel.isVisible = newExpanded
                
                revalidate()
                repaint()
            }
        })
        
        if (findings.isNotEmpty()) {
            val tableModel = object : DefaultTableModel(arrayOf(UIStrings.TABLE_COLUMN_FILE, UIStrings.TABLE_COLUMN_ISSUE, UIStrings.TABLE_COLUMN_SEVERITY), 0) {
                override fun isCellEditable(row: Int, column: Int): Boolean = false
            }
            val table = JTable(tableModel)
            
            table.columnModel.getColumn(0).preferredWidth = 150
            table.columnModel.getColumn(1).preferredWidth = 350
            table.columnModel.getColumn(2).preferredWidth = 80
            table.columnModel.getColumn(2).maxWidth = 100
            table.setRowHeight(24)
            table.setFont(Font("SansSerif", Font.PLAIN, 12))
            table.setDefaultRenderer(Any::class.java, SeverityCellRenderer())
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
            val emptyLabel = JLabel(UIStrings.FINDINGS_EMPTY).apply {
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
        
        when (status) {
            LicenseStatus.LICENSED -> {
                scorePanel.remove(licenseBanner)
            }
            LicenseStatus.TRIAL -> {
                licenseBanner.background = UIConfig.LICENSE_ACTIVATED_BG
                licenseLabel.text = UIStrings.LICENSE_TRIAL_ACTIVE_MESSAGE
                licenseLabel.font = Font(licenseLabel.font.name, Font.BOLD, licenseLabel.font.size)
                licenseActionButton.text = UIStrings.BUTTON_UPGRADE_NOW
                scorePanel.remove(licenseBanner)
                scorePanel.add(licenseBanner, 0)
            }
            LicenseStatus.TRIAL_EXPIRED -> {
                licenseBanner.background = UIConfig.LICENSE_EXPIRED_BG
                licenseLabel.text = UIStrings.LICENSE_TRIAL_EXPIRED_BANNER
                licenseActionButton.text = UIStrings.BUTTON_UPGRADE_NOW
                scorePanel.remove(licenseBanner)
                scorePanel.add(licenseBanner, 0)
            }
            LicenseStatus.UNLICENSED -> {
                licenseBanner.background = UIConfig.LICENSE_UNLICENSED_BG
                licenseLabel.text = UIStrings.LICENSE_UNLICENSED_BANNER
                licenseActionButton.text = UIStrings.BUTTON_START_TRIAL
                scorePanel.remove(licenseBanner)
                scorePanel.add(licenseBanner, 0)
            }
        }
        scorePanel.revalidate()
        scorePanel.repaint()
        logger.info("License banner added to scorePanel")
    }

    private fun getSeverityColor(severity: Severity): Color = when (severity) {
        Severity.LOW -> UIConfig.SCORE_GREEN
        Severity.MEDIUM -> UIConfig.SCORE_ORANGE
        Severity.HIGH -> UIConfig.SCORE_RED
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
                    label.background = UIConfig.TABLE_SEVERITY_FALLBACK
                    label.isOpaque = true
                }
            } else {
                label.background = if (isSelected) UIConfig.TABLE_SELECTED_BG else JBColor.white
                label.foreground = if (isSelected) Color.WHITE else JBColor.black
                label.isOpaque = true
            }
            return label
        }
    }

    private fun formatRelativeTime(timestamp: Long): String {
        if (timestamp == 0L) return "unknown"
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return when {
            seconds < 60 -> UIStrings.TIME_JUST_NOW
            minutes < 60 -> UIStrings.timeMinutesAgo(minutes)
            hours < 24 -> UIStrings.timeHoursAgo(hours)
            days < 7 -> UIStrings.timeDaysAgo(days)
            else -> UIStrings.timeWeeksAgo(days / 7)
        }
    }
}