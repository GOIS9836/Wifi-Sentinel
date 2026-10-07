package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppRiskLevel
import com.example.data.model.AppSecurityScanResult
import com.example.data.model.DailyScanScheduleSettings
import com.example.service.AclAction
import com.example.service.DailyScanScheduler
import com.example.service.NetworkAccessEnforcer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoQuarantineUnsafeAppsTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = DailyScanScheduler.getPrefs(context)
        prefs.edit().clear().commit()
    }

    @Test
    fun testAppRiskClassificationAndIsUnsafeProperty() {
        val safeApp = AppSecurityScanResult(
            packageName = "com.example.safeapp",
            appName = "Safe App",
            versionName = "1.0",
            isSystemApp = false,
            installerSource = "Google Play",
            riskLevel = AppRiskLevel.SAFE,
            riskScore = 5
        )
        assertFalse("Safe app should not be classified as unsafe", safeApp.isUnsafe)

        val criticalApp = AppSecurityScanResult(
            packageName = "com.rogue.malware",
            appName = "Rogue Dropper",
            versionName = "1.0",
            isSystemApp = false,
            installerSource = "Sideloaded / APK",
            riskLevel = AppRiskLevel.CRITICAL,
            riskScore = 85
        )
        assertTrue("Critical risk app must be classified as unsafe", criticalApp.isUnsafe)

        val highRiskApp = AppSecurityScanResult(
            packageName = "com.rogue.banktrojan",
            appName = "Bank Spy Overlay",
            versionName = "1.0",
            isSystemApp = false,
            installerSource = "Sideloaded / APK",
            riskLevel = AppRiskLevel.HIGH_RISK,
            riskScore = 55
        )
        assertTrue("High risk app must be classified as unsafe", highRiskApp.isUnsafe)

        val suspiciousSideloadedApp = AppSecurityScanResult(
            packageName = "com.suspicious.app",
            appName = "Unknown Sideload",
            versionName = "1.0",
            isSystemApp = false,
            installerSource = "Sideloaded / APK",
            riskLevel = AppRiskLevel.SUSPICIOUS,
            riskScore = 30
        )
        assertTrue("Suspicious sideloaded non-system app must be classified as unsafe", suspiciousSideloadedApp.isUnsafe)

        val whitelistedCriticalApp = criticalApp.copy(isWhitelisted = true)
        assertFalse("Whitelisted apps should not be flagged as unsafe for auto-quarantine", whitelistedCriticalApp.isUnsafe)
    }

    @Test
    fun testDailyScanScheduleSettingsDefaultsAndPersistence() {
        val defaultSettings = DailyScanScheduleSettings()
        assertTrue("Default daily scan settings must have autoQuarantineUnsafeApps enabled", defaultSettings.autoQuarantineUnsafeApps)

        val loaded = DailyScanScheduler.loadSettings(context)
        assertTrue("Loaded initial settings must default autoQuarantineUnsafeApps to true", loaded.autoQuarantineUnsafeApps)

        val updated = loaded.copy(autoQuarantineUnsafeApps = false)
        DailyScanScheduler.saveSettings(context, updated)

        val reloaded = DailyScanScheduler.loadSettings(context)
        assertFalse("Persisted autoQuarantineUnsafeApps setting should be reflected upon reload", reloaded.autoQuarantineUnsafeApps)
    }

    @Test
    fun testQuarantinedPackagesPersistence() {
        val initial = DailyScanScheduler.loadQuarantinedPackages(context)
        assertTrue("Initial quarantined set should be empty", initial.isEmpty())

        val testSet = setOf("com.malware.one", "com.spyware.two")
        DailyScanScheduler.saveQuarantinedPackages(context, testSet)

        val reloaded = DailyScanScheduler.loadQuarantinedPackages(context)
        assertEquals("Quarantined packages should persist across reload", testSet, reloaded)
    }

    @Test
    fun testNetworkAccessEnforcerAppQuarantineRuleAndPreservation() {
        val enforcer = NetworkAccessEnforcer()

        val rule = enforcer.quarantineAppNetworkAccess(
            packageName = "com.trojan.stealer",
            appName = "Stealer Trojan"
        )

        assertNotNull(rule)
        assertEquals(AclAction.DROP, rule.action)
        assertTrue("Rule ID must have APP_DROP_ prefix", rule.id.startsWith("APP_DROP_"))
        assertTrue("iptables rule must drop socket stack for app", rule.iptablesCommand.contains("-j DROP"))

        // Active rules should include this app drop rule
        val activeRules = enforcer.activeRules.value
        assertTrue("Active rules must include quarantined app", activeRules.any { it.id == rule.id })

        // When device MAC synchronization occurs, app drop rules must be preserved (not pruned)
        enforcer.syncBlockedDevices(emptySet())
        val preservedRules = enforcer.activeRules.value
        assertTrue("App drop rules must be preserved across device sync", preservedRules.any { it.id == rule.id })

        // Unquarantine app removes the rule
        enforcer.unquarantineAppNetworkAccess("com.trojan.stealer")
        val finalRules = enforcer.activeRules.value
        assertFalse("App drop rule should be removed after unquarantine", finalRules.any { it.id == rule.id })
    }
}
