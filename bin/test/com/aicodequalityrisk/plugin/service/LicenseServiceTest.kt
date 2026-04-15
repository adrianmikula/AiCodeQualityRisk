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
    fun `startTrial makes isLocked return false`() {
        val service = LicenseService()
        service.startTrial()
        assertFalse(service.isLocked())
    }

    @Test
    fun `getLicenseStatus returns TRIAL after startTrial`() {
        val service = LicenseService()
        service.startTrial()
        assertEquals(LicenseStatus.TRIAL, service.getLicenseStatus())
    }

    @Test
    fun `startTrial makes isPremium return true`() {
        val service = LicenseService()
        service.startTrial()
        assertTrue(service.isPremium())
    }

    @Test
    fun `startTrial makes isInTrial return true`() {
        val service = LicenseService()
        service.startTrial()
        assertTrue(service.isInTrial())
    }

    @Test
    fun `startTrial makes isTrialExpired return false`() {
        val service = LicenseService()
        service.startTrial()
        assertFalse(service.isTrialExpired())
    }

    @Test
    fun `startTrial makes isUnlicensed return false`() {
        val service = LicenseService()
        service.startTrial()
        assertFalse(service.isUnlicensed())
    }

    @Test
    fun `expired trial returns TRIAL_EXPIRED status`() {
        val pastTime = System.currentTimeMillis() - 1000L
        val service = LicenseService(trialStartTime = pastTime, trialEndTime = pastTime + 100L)
        assertEquals(LicenseStatus.TRIAL_EXPIRED, service.getLicenseStatus())
    }

    @Test
    fun `expired trial is locked`() {
        val pastTime = System.currentTimeMillis() - 1000L
        val service = LicenseService(trialStartTime = pastTime, trialEndTime = pastTime + 100L)
        assertTrue(service.isLocked())
    }

    @Test
    fun `expired trial is not premium`() {
        val pastTime = System.currentTimeMillis() - 1000L
        val service = LicenseService(trialStartTime = pastTime, trialEndTime = pastTime + 100L)
        assertFalse(service.isPremium())
    }

    @Test
    fun `expired trial is not in trial`() {
        val pastTime = System.currentTimeMillis() - 1000L
        val service = LicenseService(trialStartTime = pastTime, trialEndTime = pastTime + 100L)
        assertFalse(service.isInTrial())
    }

    @Test
    fun `expired trial is trial expired`() {
        val pastTime = System.currentTimeMillis() - 1000L
        val service = LicenseService(trialStartTime = pastTime, trialEndTime = pastTime + 100L)
        assertTrue(service.isTrialExpired())
    }

    @Test
    fun `active trial with future end time returns TRIAL status`() {
        val now = System.currentTimeMillis()
        val futureEnd = now + 14L * 24 * 60 * 60 * 1000
        val service = LicenseService(trialStartTime = now, trialEndTime = futureEnd)
        assertEquals(LicenseStatus.TRIAL, service.getLicenseStatus())
    }

    @Test
    fun `active trial is not locked`() {
        val now = System.currentTimeMillis()
        val futureEnd = now + 14L * 24 * 60 * 60 * 1000
        val service = LicenseService(trialStartTime = now, trialEndTime = futureEnd)
        assertFalse(service.isLocked())
    }

    @Test
    fun `getUpgradeUrl returns correct URL`() {
        val service = LicenseService()
        assertEquals("https://plugins.jetbrains.com/plugin/31227-ai-code-quality-risk", service.getUpgradeUrl())
    }
}