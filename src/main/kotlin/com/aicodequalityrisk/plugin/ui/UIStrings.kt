package com.aicodequalityrisk.plugin.ui

import java.util.ResourceBundle

/**
 * Centralized access to UI strings from properties file.
 * This makes it easy to edit all UI text in one place without touching code.
 */
object UIStrings {
    private val bundle = ResourceBundle.getBundle("ui_strings")

    // Tool Window
    val TOOL_WINDOW_ID: String = bundle.getString("tool.window.id")
    val TOOL_WINDOW_NAME: String = bundle.getString("tool.window.name")

    // Score Labels
    fun scoreOverall(value: String): String = bundle.getString("score.overall").replace("{0}", value)
    fun scoreComplexity(value: String): String = bundle.getString("score.complexity").replace("{0}", value)
    fun scoreDuplication(value: String): String = bundle.getString("score.duplication").replace("{0}", value)
    fun scorePerformance(value: String): String = bundle.getString("score.performance").replace("{0}", value)
    fun scoreSecurity(value: String): String = bundle.getString("score.security").replace("{0}", value)

    // Status Messages
    val STATUS_WAITING: String = bundle.getString("status.waiting")
    val STATUS_ANALYZING: String = bundle.getString("status.analyzing")
    val STATUS_ERROR: String = bundle.getString("status.error")
    val STATUS_UNKNOWN: String = bundle.getString("status.unknown")

    // Findings
    val FINDINGS_LABEL: String = bundle.getString("findings.label")
    val FINDINGS_EMPTY: String = bundle.getString("findings.empty")
    fun findingsCount(count: Int): String = bundle.getString("findings.count").replace("{0}", count.toString())
    val FINDINGS_SUFFIX: String = bundle.getString("findings.suffix")

    // Table Headers
    val TABLE_COLUMN_FILE: String = bundle.getString("table.column.file")
    val TABLE_COLUMN_ISSUE: String = bundle.getString("table.column.issue")
    val TABLE_COLUMN_SEVERITY: String = bundle.getString("table.column.severity")

    // License Messages
    val LICENSE_UNLICENSED_MESSAGE: String = bundle.getString("license.unlicensed.message")
    val LICENSE_TRIAL_EXPIRED_MESSAGE: String = bundle.getString("license.trial.expired.message")
    val LICENSE_REQUIRED_MESSAGE: String = bundle.getString("license.required.message")
    val LICENSE_TRIAL_ACTIVE_MESSAGE: String = bundle.getString("license.trial.active.message")
    val LICENSE_TRIAL_EXPIRED_BANNER: String = bundle.getString("license.trial.expired.banner")
    val LICENSE_UNLICENSED_BANNER: String = bundle.getString("license.unlicensed.banner")

    // Button Text
    val BUTTON_UPGRADE_NOW: String = bundle.getString("button.upgrade.now")
    val BUTTON_START_TRIAL: String = bundle.getString("button.start.trial")

    // Time Formatting
    val TIME_JUST_NOW: String = bundle.getString("time.just.now")
    fun timeMinutesAgo(minutes: Long): String = bundle.getString("time.minutes.ago").replace("{0}", minutes.toString())
    fun timeHoursAgo(hours: Long): String = bundle.getString("time.hours.ago").replace("{0}", hours.toString())
    fun timeDaysAgo(days: Long): String = bundle.getString("time.days.ago").replace("{0}", days.toString())
    fun timeWeeksAgo(weeks: Long): String = bundle.getString("time.weeks.ago").replace("{0}", weeks.toString())
    fun timeLastAnalysis(time: String): String = bundle.getString("time.last.analysis").replace("{0}", time)
    fun timeFilesAnalyzed(count: Int): String = bundle.getString("time.files.analyzed").replace("{0}", count.toString())
}
