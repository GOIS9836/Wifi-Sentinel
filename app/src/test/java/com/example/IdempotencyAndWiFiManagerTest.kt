package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DiscoveredDevice
import com.example.data.model.ThreatLevel
import com.example.service.WiFiManager
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
class IdempotencyAndWiFiManagerTest {

    private lateinit var application: Application
    private lateinit var viewModel: MainViewModel
    private lateinit var wifiManager: WiFiManager

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = MainViewModel(application)
        wifiManager = WiFiManager(application)
    }

    @Test
    fun testWiFiManagerSnapshotRetrieval() {
        val state = wifiManager.getNetworkState()
        assertNotNull(state)
        assertNotNull(state.ssid)
        assertTrue(state.rssi <= 0)
        assertTrue(state.signalLevelPercent in 0..100)

        val ssid = wifiManager.getConnectedSSID()
        assertNotNull(ssid)
        assertFalse(ssid.contains("\""))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testDismissDeviceAsFalsePositiveIdempotency() = runTest {
        val testDevice = DiscoveredDevice(
            ip = "192.168.1.188",
            macAddress = "AA:BB:CC:DD:EE:FF",
            vendor = "Test Vendor",
            customName = "Test Probe",
            isAuthorized = false,
            isBlocked = true,
            responseTimeMs = 12L,
            threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER,
            isRandomizedMac = true,
            confidencePercent = 75,
            corroborationVector = "Test Vector"
        )

        // First dismissal
        viewModel.dismissDeviceAsFalsePositive(testDevice)
        advanceUntilIdle()

        val countAfterFirst = viewModel.falsePositivesSuppressedCount.value

        // Second dismissal with the exact same device
        viewModel.dismissDeviceAsFalsePositive(testDevice)
        advanceUntilIdle()

        val countAfterSecond = viewModel.falsePositivesSuppressedCount.value
        assertEquals("Subsequent dismissal of the same device must be idempotent and not re-increment counter",
            countAfterFirst, countAfterSecond)

        // Third dismissal with another call
        viewModel.dismissDeviceAsFalsePositive(testDevice)
        advanceUntilIdle()
        val countAfterThird = viewModel.falsePositivesSuppressedCount.value
        assertEquals("Third dismissal must remain idempotent", countAfterFirst, countAfterThird)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testDismissBtDeviceFalsePositiveIdempotency() = runTest {
        val btMac = "11:22:33:44:55:66"
        val initialSuppressed = viewModel.falsePositivesSuppressedCount.value

        viewModel.dismissBtDeviceFalsePositive(btMac)
        advanceUntilIdle()

        val countAfterFirst = viewModel.falsePositivesSuppressedCount.value
        assertEquals(initialSuppressed + 1, countAfterFirst)

        // Calling again for the same BT device
        viewModel.dismissBtDeviceFalsePositive(btMac)
        advanceUntilIdle()

        val countAfterSecond = viewModel.falsePositivesSuppressedCount.value
        assertEquals("Subsequent dismissal of the same BT device must be idempotent",
            countAfterFirst, countAfterSecond)
    }

    @Test
    fun testArpTableParsing() {
        val sampleProcNetArp = """
            IP address       HW type     Flags       HW address            Mask     Device
            192.168.1.1      0x1         0x2         00:1A:2B:3C:4D:01     *        wlan0
            192.168.1.25     0x1         0x2         B4:FB:E4:91:22:A1     *        wlan0
            192.168.1.40     0x1         0x0         00:00:00:00:00:00     *        wlan0
            192.168.1.55     0x1         0x2         7A:B4:9C:12:34:56     *        wlan0
        """.trimIndent()

        val entries = wifiManager.parseArpTableContent(sampleProcNetArp)
        assertEquals(4, entries.size)

        val gateway = entries.find { it.ip == "192.168.1.1" }
        assertNotNull(gateway)
        assertTrue(gateway!!.isComplete)
        assertEquals("00:1A:2B:3C:4D:01", gateway.macAddress)

        val incomplete = entries.find { it.ip == "192.168.1.40" }
        assertNotNull(incomplete)
        assertFalse(incomplete!!.isComplete)

        // Randomized / Locally Administered MAC check
        val randomizedEntry = entries.find { it.ip == "192.168.1.55" }
        assertNotNull(randomizedEntry)
        assertTrue(wifiManager.isLocallyAdministeredMac(randomizedEntry!!.macAddress))
        assertFalse(wifiManager.isLocallyAdministeredMac(gateway.macAddress))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testScanForConnectedDevicesUsingArpIdentifiesUnauthorized() = runTest {
        val authorizedMacs = setOf("00:1A:2B:3C:4D:01", "B4:FB:E4:91:22:A1")
        val scanResult = wifiManager.scanForConnectedDevicesUsingArp(
            authorizedMacs = authorizedMacs,
            subnetRange = 1..10,
            probeTimeoutMs = 50
        )

        assertNotNull(scanResult)
        assertTrue(scanResult.totalDevicesFound >= 2)
        assertTrue(scanResult.devices.isNotEmpty())

        // Check that authorized devices are marked correctly
        val gateway = scanResult.devices.find { it.isGateway }
        assertNotNull(gateway)
        assertTrue(gateway!!.isAuthorized)
        assertFalse(gateway.isPotentialUnauthorizedUser)

        // Check unauthorized detection
        val unauthorized = scanResult.unauthorizedDevices
        for (dev in unauthorized) {
            assertTrue(dev.isPotentialUnauthorizedUser)
            assertFalse(dev.isAuthorized)
            assertNotNull(dev.threatReason)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testBlockedAndQuarantinedDevicesHaveNoNetworkAccess() = runTest {
        val targetMac = "E4:5F:01:3B:11:FE"
        val testDevice = DiscoveredDevice(
            ip = "192.168.1.119",
            macAddress = targetMac,
            vendor = "Unknown Host",
            customName = "Intruder Linux",
            isAuthorized = false,
            isBlocked = false,
            responseTimeMs = 25L,
            threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER
        )

        // Initially unblocked, network access should be allowed
        assertTrue("Initially device should not be blocked", viewModel.networkAccessEnforcer.isNetworkAccessAllowed(targetMac))

        // Quarantine / block device
        viewModel.quarantineDevice(testDevice)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        advanceUntilIdle()

        // Blocked device MUST NOT have network access
        assertFalse("Quarantined device must NOT have network access", viewModel.networkAccessEnforcer.isNetworkAccessAllowed(targetMac))

        // Check ACL rules
        val activeRules = viewModel.activeAclRules.value
        val rule = activeRules.find { it.targetMac.equals(targetMac, ignoreCase = true) }
        assertNotNull("Firewall ACL rule must be generated for quarantined device", rule)
        assertEquals(com.example.service.AclAction.DROP, rule!!.action)

        // Verify discovered devices list in ViewModel reflects no network access
        val devInList = viewModel.discoveredDevices.value.find { it.macAddress.equals(targetMac, ignoreCase = true) }
        if (devInList != null) {
            assertTrue(devInList.isBlocked)
            assertFalse(devInList.hasNetworkAccess)
            assertEquals(0L, devInList.responseTimeMs)
        }

        // Unblock device
        val blockedDevice = testDevice.copy(isBlocked = true)
        viewModel.quarantineDevice(blockedDevice)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        advanceUntilIdle()

        // Network access should be restored after unblocking
        assertTrue("Unblocked device should have network access restored", viewModel.networkAccessEnforcer.isNetworkAccessAllowed(targetMac))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testArpScanSeveresNetworkAccessForBlockedDevices() = runTest {
        val blockedMac = "B4:FB:E4:91:22:A1"
        val scanResult = wifiManager.scanForConnectedDevicesUsingArp(
            authorizedMacs = setOf("00:1A:2B:3C:4D:01"),
            blockedMacs = setOf(blockedMac),
            subnetRange = 1..10,
            probeTimeoutMs = 50
        )

        val quarantinedDevice = scanResult.devices.find { it.macAddress.equals(blockedMac, ignoreCase = true) }
        assertNotNull(quarantinedDevice)
        assertTrue("Device must be marked as blocked", quarantinedDevice!!.isBlocked)
        assertFalse("Quarantined device must not have network access", quarantinedDevice.hasNetworkAccess)
        assertEquals("Quarantined device must have response time of 0ms (dropped traffic)", 0L, quarantinedDevice.responseTimeMs)
        val reason = quarantinedDevice.threatReason ?: ""
        assertTrue(reason.contains("quarantine", ignoreCase = true) ||
                reason.contains("Access Denied", ignoreCase = true) ||
                reason.contains("Firewall", ignoreCase = true))
    }

    @Test
    fun testSentinelBuilderEngineOuiDatabaseResolution() {
        // Test OUI resolution matching Sentinel Backend / Builder Engine specifications
        assertEquals("Espressif Inc. (IoT Chipset)", wifiManager.resolveVendor("74:AC:B9:40:12:09"))
        assertEquals("Espressif Inc. (IoT Chipset)", wifiManager.resolveVendor("74:AC:B9:40:12:0A"))
        assertEquals("Espressif Inc. (IoT Chipset)", wifiManager.resolveVendor("74:AC:B9:40:12:0C"))
        assertEquals("Raspberry Pi Foundation", wifiManager.resolveVendor("B8:27:EB:40:12:00"))
        assertEquals("Ayecom Technology", wifiManager.resolveVendor("00:1A:2B:40:12:00"))
        assertEquals("Apple Inc.", wifiManager.resolveVendor("F0:99:BF:40:12:00"))
    }
}
