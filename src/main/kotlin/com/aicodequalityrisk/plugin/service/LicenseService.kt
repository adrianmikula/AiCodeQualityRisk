package com.aicodequalityrisk.plugin.service

import com.aicodequalityrisk.plugin.model.LicenseStatus
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.ide.plugins.PluginManager

@Service(Service.Level.PROJECT)
open class LicenseService {

    private val productCode = "PAICODEQUALITY"

    constructor()

    constructor(project: Project) {
        // Marketplace API handles initialization
    }

    // Trial management handled by Marketplace through plugin.xml product-descriptor

    fun getLicenseStatus(): LicenseStatus {
        return try {
            checkTrialStatus()
        } catch (e: Throwable) {
            getLicenseStatusWithoutPluginCheck()
        }
    }

    private fun getLicenseStatusWithoutPluginCheck(): LicenseStatus {
        // Marketplace handles trial state through product-descriptor
        // This fallback should not normally be reached
        return LicenseStatus.UNLICENSED
    }

    private fun checkTrialStatus(): LicenseStatus {
        val pluginManager = PluginManager.getInstance()
        val plugin = pluginManager.findEnabledPlugin(PluginId.getId(productCode))
        
        return when {
            plugin != null && !plugin.isBundled -> {
                // Marketplace manages trial/paid status automatically
                // For now, assume active if plugin is enabled and not bundled
                LicenseStatus.LICENSED
            }
            else -> getLicenseStatusWithoutPluginCheck()
        }
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

    fun startMarketplaceTrial() {
        // Marketplace handles trial initiation through plugin.xml
        // Users need to install from Marketplace to start trial
        throw UnsupportedOperationException("Trial must be started through Marketplace installation")
    }

    fun getUpgradeUrl(): String = "https://plugins.jetbrains.com/plugin/31227-ai-code-quality-risk"

    companion object {
        fun createDefault(): LicenseService? = null

        fun getInstance(project: Project): LicenseService = project.service()
    }
}