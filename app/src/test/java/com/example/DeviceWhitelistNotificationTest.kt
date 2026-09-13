package com.example

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NetworkDeviceEntity
import com.example.data.model.DiscoveredDevice
import com.example.data.model.ThreatLevel
import com.example.data.model.WhitelistAuditStatus
import com.example.service.DeviceWhitelistComparisonEngine
import com.example.service.SecurityNotificationDispatcher
import com.example.ui.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
class DeviceWhitelistNotificationTest {

    private lateinit var application: Application
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = MainViewModel(application)
    }

    @Test
    fun testNotificationChannelInitialization() {
        SecurityNotificationDispatcher.initNotificationChannel(application)
        val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(SecurityNotificationDispatcher.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(SecurityNotificationDispatcher.CHANNEL_ID, channel.id)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }

    @Test
    fun testComparisonEngineAllWhitelisted() {
        val whitelist = listOf(
            NetworkDeviceEntity(
                macAddress = "AA:BB:CC:11:22:33",
                ipAddress = "192.168.1.10",
                vendor = "Apple Inc.",
                isAuthorized = true
            ),
            NetworkDeviceEntity(
                macAddress = "AA:BB:CC:44:55:66",
                ipAddress = "192.168.1.20",
                vendor = "Google LLC",
                isAuthorized = true
            )
        )

        val connected = listOf(
            DiscoveredDevice(
                ip = "192.168.1.10",
                macAddress = "aa:bb:cc:11:22:33", // lowercase test
                vendor = "Apple Inc."
            ),
            DiscoveredDevice(
                ip = "192.168.1.20",
                macAddress = "AA:BB:CC:44:55:66",
                vendor = "Google LLC"
            ),
            DiscoveredDevice(
                ip = "192.168.1.105",
                macAddress = "11:22:33:44:55:66",
                vendor = "Self Device",
                isSelf = true
            ),
            DiscoveredDevice(
                ip = "192.168.1.1",
                macAddress = "DE:AD:BE:EF:00:01",
                vendor = "Router",
                isGateway = true
            )
        )

        val result = DeviceWhitelistComparisonEngine.compareConnectedAgainstWhitelist(
            connectedDevices = connected,
            whitelist = whitelist,
            currentIp = "192.168.1.105",
            gatewayIp = "192.168.1.1",
            gatewayMac = "DE:AD:BE:EF:00:01"
        )

        assertEquals(4, result.totalConnected)
        assertEquals(4, result.whitelistedCount)
        assertEquals(0, result.unknownCount)
        assertEquals(0, result.newlyDetectedUnknowns.size)
        assertEquals(WhitelistAuditStatus.ALL_WHITELISTED, result.status)
    }

    @Test
    fun testComparisonEngineDetectsUnknownIntruderAndFlagsNotification() {
        val whitelist = listOf(
            NetworkDeviceEntity(
                macAddress = "AA:BB:CC:11:22:33",
                ipAddress = "192.168.1.10",
                vendor = "Authorized Laptop",
                isAuthorized = true
            )
        )

        val connected = listOf(
            DiscoveredDevice(
                ip = "192.168.1.10",
                macAddress = "AA:BB:CC:11:22:33",
                vendor = "Authorized Laptop"
            ),
            DiscoveredDevice(
                ip = "192.168.1.199",
                macAddress = "FE:ED:FA:CE:99:88",
                vendor = "Rogue Probe Vector (ESP32)"
            )
        )

        val result = DeviceWhitelistComparisonEngine.compareConnectedAgainstWhitelist(
            connectedDevices = connected,
            whitelist = whitelist,
            currentIp = "192.168.1.105",
            gatewayIp = "192.168.1.1"
        )

        assertEquals(2, result.totalConnected)
        assertEquals(1, result.whitelistedCount)
        assertEquals(1, result.unknownCount)
        assertEquals(1, result.newlyDetectedUnknowns.size)
        assertEquals(WhitelistAuditStatus.ALERT_TRIGGERED, result.status)

        val unknown = result.newlyDetectedUnknowns.first()
        assertEquals("192.168.1.199", unknown.ip)
        assertEquals("FE:ED:FA:CE:99:88", unknown.macAddress)
        assertFalse(unknown.isAuthorized)
        assertTrue(unknown.isFlaggedUnknown)
        assertEquals(ThreatLevel.UNAUTHORIZED_INTRUDER, unknown.threatLevel)
    }

    @Test
    fun testNotificationDeduplicationWithPreviouslyAlertedMacs() {
        val connected = listOf(
            DiscoveredDevice(
                ip = "192.168.1.199",
                macAddress = "FE:ED:FA:CE:99:88",
                vendor = "Rogue Probe Vector"
            )
        )

        // Pass FE:ED:FA:CE:99:88 as already alerted
        val result = DeviceWhitelistComparisonEngine.compareConnectedAgainstWhitelist(
            connectedDevices = connected,
            whitelist = emptyList(),
            currentIp = "192.168.1.105",
            previouslyAlertedMacs = setOf("FE:ED:FA:CE:99:88")
        )

        assertEquals(1, result.unknownCount)
        assertEquals(0, result.newlyDetectedUnknowns.size)
        assertEquals(WhitelistAuditStatus.UNKNOWN_DETECTED, result.status)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProcessWhitelistComparisonTriggersAlertAndNotification() = runTest {
        val unknownHost = DiscoveredDevice(
            ip = "192.168.1.189",
            macAddress = "B8:27:EB:12:34:56",
            vendor = "Raspberry Pi Foundation"
        )

        val audit = viewModel.processWhitelistComparison(listOf(unknownHost), triggerNotifications = true)

        assertEquals(1, audit.totalConnected)
        assertEquals(1, audit.unknownCount)
        assertEquals(1, audit.newlyDetectedUnknowns.size)
        assertEquals(WhitelistAuditStatus.ALERT_TRIGGERED, audit.status)
        assertEquals(audit, viewModel.whitelistAuditResult.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testAddAndRemoveFromWhitelistFlow() = runTest {
        val testDev = DiscoveredDevice(
            ip = "192.168.1.155",
            macAddress = "99:88:77:66:55:44",
            vendor = "IoT Smart TV",
            customName = "Living Room TV"
        )

        val dao = com.example.data.local.AppDatabase.getInstance(application).networkDeviceDao()
        dao.insertOrUpdate(
            NetworkDeviceEntity(
                macAddress = testDev.macAddress,
                ipAddress = testDev.ip,
                vendor = testDev.vendor,
                customName = testDev.customName,
                isAuthorized = true,
                isBlocked = false
            )
        )

        val whitelisted = dao.getWhitelistedDevicesList()
        assertTrue("Device should be present in whitelisted list in Room", whitelisted.any { it.macAddress.equals(testDev.macAddress, ignoreCase = true) && it.isAuthorized })

        // Remove from whitelist
        dao.setAuthorized(testDev.macAddress, false)

        val whitelistedAfter = dao.getWhitelistedDevicesList()
        assertFalse("Device should no longer be authorized in Room", whitelistedAfter.any { it.macAddress.equals(testDev.macAddress, ignoreCase = true) && it.isAuthorized })
    }
}
