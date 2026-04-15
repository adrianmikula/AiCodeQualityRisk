package com.aicodequalityrisk.plugin.service

import com.aicodequalityrisk.plugin.model.LicenseStatus
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
open class LicenseService {

    private var trialStartTime: Long = 0L
    private var trialEndTime: Long = 0L

    constructor()

    constructor(trialStartTime: Long, trialEndTime: Long) {
        this.trialStartTime = trialStartTime
        this.trialEndTime = trialEndTime
    }

    constructor(project: Project) {

    }

    fun startTrial() {
        val trialDurationMillis = 14L * 24 * 60 * 60 * 1000 // 14 days
        trialStartTime = System.currentTimeMillis()
        trialEndTime = trialStartTime + trialDurationMillis
    }

    fun getLicenseStatus(): LicenseStatus {
        return try {
            checkTrialStatus()
        } catch (e: Throwable) {
            getLicenseStatusWithoutPluginCheck()
        }
    }

    private fun getLicenseStatusWithoutPluginCheck(): LicenseStatus {
        if (trialStartTime > 0) {
            val now = System.currentTimeMillis()
            if (now < trialEndTime) {
                return LicenseStatus.TRIAL
            } else {
                return LicenseStatus.TRIAL_EXPIRED
            }
        }
        return LicenseStatus.UNLICENSED
    }

    private fun checkTrialStatus(): LicenseStatus {
        val pluginId = "PAICODEQUALITY"
        val pluginManager = com.intellij.ide.plugins.PluginManager.getInstance()
        val plugin = pluginManager.findEnabledPlugin(PluginId.getId(pluginId))
        if (plugin != null && !plugin.isBundled) {
            return LicenseStatus.LICENSED
        }

        return getLicenseStatusWithoutPluginCheck()
    }

    fun isPremium(): Boolean {
        val status = getLicenseStatus()
        return status == LicenseStatus.LICENSED || status == LicenseStatus.TRIAL
    }

    fun isTrialExpired(): Boolean = getLicenseStatus() == LicenseStatus.TRIAL_EXPIRED

    fun isInTrial(): Boolean = getLicenseStatus() == LicenseStatus.TRIAL

    fun isUnlicensed(): Boolean = getLicenseStatus() == LicenseStatus.UNLICENSED

    fun isLocked(): Boolean {
        val status = getLicenseStatus()
        return status == LicenseStatus.UNLICENSED || status == LicenseStatus.TRIAL_EXPIRED
    }

    fun getUpgradeUrl(): String = "https://plugins.jetbrains.com/plugin/31227-ai-code-quality-risk"

    companion object {
        fun createDefault(): LicenseService? = null

        fun getInstance(project: Project): LicenseService = project.service()
    }
}