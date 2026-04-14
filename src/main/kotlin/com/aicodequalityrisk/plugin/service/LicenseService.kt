package com.aicodequalityrisk.plugin.service

import com.aicodequalityrisk.plugin.model.LicenseStatus
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
open class LicenseService(project: Project) {

    private val projectRef = project

    private var trialPreviouslyActive = false

    fun getLicenseStatus(): LicenseStatus {
        return try {
            fallbackLicenseCheck()
        } catch (e: Throwable) {
            LicenseStatus.UNLICENSED
        }
    }

    private fun fallbackLicenseCheck(): LicenseStatus {
        return try {
            val pluginId = "PAICODEQUALITY"
            val pluginManager = com.intellij.ide.plugins.PluginManager.getInstance()
            val plugin = pluginManager.findEnabledPlugin(PluginId.getId(pluginId))
            if (plugin != null && !plugin.isBundled) {
                LicenseStatus.LICENSED
            } else {
                LicenseStatus.UNLICENSED
            }
        } catch (e: Throwable) {
            LicenseStatus.UNLICENSED
        }
    }

    fun isPremium(): Boolean {
        val status = getLicenseStatus()
        return status == LicenseStatus.LICENSED || status == LicenseStatus.TRIAL
    }

    fun isTrialExpired(): Boolean = getLicenseStatus() == LicenseStatus.TRIAL_EXPIRED

    fun isInTrial(): Boolean = getLicenseStatus() == LicenseStatus.TRIAL

    fun isUnlicensed(): Boolean = getLicenseStatus() == LicenseStatus.UNLICENSED

    fun getUpgradeUrl(): String = "https://plugins.jetbrains.com/plugin/31227-ai-code-quality-risk"

    companion object {
        fun createDefault(): LicenseService? = null

        fun getInstance(project: Project): LicenseService = project.service()
    }
}