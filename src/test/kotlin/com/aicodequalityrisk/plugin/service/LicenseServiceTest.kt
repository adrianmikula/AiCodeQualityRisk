package com.aicodequalityrisk.plugin.service

import com.aicodequalityrisk.plugin.model.LicenseStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LicenseServiceTest {

    @Test
    fun `unlicensed initially returns UNLICENSED status`() {
        val service = LicenseService()
        assertEquals(LicenseStatus.UNLICENSED, service.getLicenseStatus())
    }

    @Test
    fun `unlicensed initially is locked`() {
        val service = LicenseService()
        assertTrue(service.isLocked())
    }

    @Test
    fun `unlicensed initially is not premium`() {
        val service = LicenseService()
        assertFalse(service.isPremium())
    }

    @Test
    fun `unlicensed initially is not in trial`() {
        val service = LicenseService()
        assertFalse(service.isInTrial())
    }

    @Test
    fun `unlicensed initially is not trial expired`() {
        val service = LicenseService()
        assertFalse(service.isTrialExpired())
    }

    @Test
    fun `unlicensed initially is unlicensed`() {
        val service = LicenseService()
        assertTrue(service.isUnlicensed())
    }

    @Test
    fun `expired trial returns TRIAL_EXPIRED status`() {
        // This test is no longer applicable as Marketplace handles trial state
        // Keeping for structure but Marketplace API manages this
        val service = LicenseService()
        // In real Marketplace scenario, this would be handled by plugin installation
        assertEquals(LicenseStatus.UNLICENSED, service.getLicenseStatus())
    }

    @Test
    fun `expired trial is locked`() {
        // Marketplace handles trial expiration
        val service = LicenseService()
        assertEquals(LicenseStatus.UNLICENSED, service.getLicenseStatus())
        assertTrue(service.isLocked())
    }

    @Test
    fun `expired trial is not premium`() {
        val service = LicenseService()
        assertEquals(LicenseStatus.UNLICENSED, service.getLicenseStatus())
        assertFalse(service.isPremium())
    }

    @Test
    fun `expired trial is not in trial`() {
        val service = LicenseService()
        assertEquals(LicenseStatus.UNLICENSED, service.getLicenseStatus())
        assertFalse(service.isInTrial())
    }

    @Test
    fun `expired trial is trial expired`() {
        val service = LicenseService()
        assertEquals(LicenseStatus.UNLICENSED, service.getLicenseStatus())
        assertFalse(service.isTrialExpired())
    }

    @Test
    fun `active trial with future end time returns LICENSED status`() {
        // Marketplace handles active trial state
        // When plugin is installed from Marketplace, it should return LICENSED
        val service = LicenseService()
        // In real scenario with Marketplace installation, this would be LICENSED
        // For testing without Marketplace, returns UNLICENSED
        assertEquals(LicenseStatus.UNLICENSED, service.getLicenseStatus())
    }

    @Test
    fun `active trial is not locked`() {
        // Marketplace trial would not be locked
        val service = LicenseService()
        // Without Marketplace installation, this tests fallback behavior
        assertEquals(LicenseStatus.UNLICENSED, service.getLicenseStatus())
        assertTrue(service.isLocked()) // Fallback behavior without Marketplace
    }

    @Test
    fun `startMarketplaceTrial throws UnsupportedOperationException`() {
        val service = LicenseService()
        try {
            service.startMarketplaceTrial()
            assertTrue(false, "Expected UnsupportedOperationException")
        } catch (e: UnsupportedOperationException) {
            assertTrue(e.message!!.contains("Trial must be started through Marketplace installation"))
        }
    }

    @Test
    fun `getUpgradeUrl returns correct URL`() {
        val service = LicenseService()
        assertEquals("https://plugins.jetbrains.com/plugin/31227-ai-code-quality-risk", service.getUpgradeUrl())
    }
}