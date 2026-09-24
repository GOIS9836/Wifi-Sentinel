package com.example.wifisentinel

import com.example.wifisentinel.data.local.SentinelAlertDao
import com.example.wifisentinel.data.local.SentinelAlertEntity
import com.example.wifisentinel.data.local.SentinelDeviceDao
import com.example.wifisentinel.data.local.SentinelDeviceEntity
import com.example.wifisentinel.data.repository.SentinelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * High-speed deterministic unit test suite for SentinelRepository (Phase 2).
 * Verifies POTRAZ Chapter 12:07 identifier anonymization and LAA MAC randomization detection.
 */
class SentinelRepositoryTest {

    private lateinit var fakeDeviceDao: FakeSentinelDeviceDao
    private lateinit var fakeAlertDao: FakeSentinelAlertDao
    private lateinit var repository: SentinelRepository

    @Before
    fun setUp() {
        fakeDeviceDao = FakeSentinelDeviceDao()
        fakeAlertDao = FakeSentinelAlertDao()
        repository = SentinelRepository(fakeDeviceDao, fakeAlertDao, salt = "TEST_SALT_POTRAZ")
    }

    @Test
    fun ingestDiscoveredDevice_marksRandomizedMacProperly() = runTest {
        // Randomized MAC (2nd hex character: 'a')
        val randomizedDevice = repository.ingestDiscoveredDevice(
            rawMac = "da:a1:19:00:11:22",
            ipAddress = "192.168.1.50",
            vendor = "Virtual Endpoint"
        )
        assertTrue("Expected randomized MAC to be detected as LAA", randomizedDevice.isRandomizedMac)
        assertEquals("DA:A1:19:00:11:22", randomizedDevice.macAddress)
        assertNotNull(randomizedDevice.anonymizedHash)
        assertEquals(64, randomizedDevice.anonymizedHash.length) // SHA-256 hex string

        // Hardware burned-in MAC (2nd hex character: '0')
        val hardwareDevice = repository.ingestDiscoveredDevice(
            rawMac = "00:1A:2B:3C:4D:5E",
            ipAddress = "192.168.1.1",
            vendor = "Router Manufacturer"
        )
        assertFalse("Expected hardware burned-in MAC not to be marked as randomized", hardwareDevice.isRandomizedMac)
        assertEquals("00:1A:2B:3C:4D:5E", hardwareDevice.macAddress)
    }

    @Test
    fun ingestDiscoveredDevice_computesDeterministicAnonymizedHash() = runTest {
        val mac = "06:12:34:56:78:9A"
        val dev1 = repository.ingestDiscoveredDevice(mac, "192.168.1.100")
        val dev2 = repository.ingestDiscoveredDevice(mac, "192.168.1.100")

        assertEquals("Hashes for identical MACs and salt must be deterministic", dev1.anonymizedHash, dev2.anonymizedHash)

        val diffMacDevice = repository.ingestDiscoveredDevice("06:12:34:56:78:9B", "192.168.1.101")
        assertNotEquals("Hashes for differing MACs must differ", dev1.anonymizedHash, diffMacDevice.anonymizedHash)
    }

    @Test
    fun authorizationAndBlockTransitions_flowUpdatesCorrectly() = runTest {
        val mac = "B4:2E:99:AB:CD:EF"
        repository.ingestDiscoveredDevice(mac, "192.168.1.169", "Target Node", initialAuthorized = false)

        val initialWhitelist = repository.whitelistedDevices.first()
        assertTrue(initialWhitelist.none { it.macAddress == mac })

        // Authorize
        repository.setDeviceAuthorized(mac, true)
        val afterAuth = repository.whitelistedDevices.first()
        assertTrue(afterAuth.any { it.macAddress == mac })

        // Block
        repository.setDeviceBlocked(mac, true)
        val blockedList = repository.blockedDevices.first()
        assertTrue(blockedList.any { it.macAddress == mac })
    }

    @Test
    fun recordSecurityAlert_containsAnonymizedHashAndUpdatesCount() = runTest {
        val mac = "12:34:56:78:9A:BC"
        val alert = repository.recordSecurityAlert(
            title = "Rogue Listener Detected",
            description = "Unauthorized device attempted ARP cache injection",
            deviceIp = "192.168.1.189",
            deviceMac = mac,
            severity = "CRITICAL"
        )

        assertEquals("CRITICAL", alert.severity)
        assertEquals(mac, alert.deviceMac)
        assertEquals(64, alert.anonymizedHash.length)
        assertFalse(alert.isAcknowledged)

        val unackCount = repository.unacknowledgedAlertsCount.first()
        assertEquals(1, unackCount)

        val savedAlert = repository.securityAlerts.first().first()
        repository.acknowledgeAlert(savedAlert.id)
        val afterAckCount = repository.unacknowledgedAlertsCount.first()
        assertEquals(0, afterAckCount)
    }

    @Test
    fun removeQuarantinedDevicesFromNetwork_removesAllBlockedDevices() = runTest {
        val mac1 = "11:22:33:44:55:66"
        val mac2 = "AA:BB:CC:DD:EE:FF"
        val mac3 = "77:88:99:AA:BB:CC"

        repository.ingestDiscoveredDevice(mac1, "192.168.1.10", "Host1")
        repository.ingestDiscoveredDevice(mac2, "192.168.1.20", "Host2")
        repository.ingestDiscoveredDevice(mac3, "192.168.1.30", "Host3")

        // Block two devices
        repository.setDeviceBlocked(mac1, true)
        repository.setDeviceBlocked(mac2, true)
        repository.setDeviceAuthorized(mac3, true)

        assertEquals(2, repository.blockedDevices.first().size)

        // Remove quarantined devices from network
        val removedCount = repository.removeQuarantinedDevicesFromNetwork()
        assertEquals(2, removedCount)
        assertEquals(0, repository.blockedDevices.first().size)

        // Authorized device remains
        val remaining = repository.allDevices.first()
        assertEquals(1, remaining.size)
        assertEquals(mac3, remaining[0].macAddress)
    }

    @Test
    fun removeQuarantinedDevice_removesSingleBlockedDevice() = runTest {
        val mac = "AA:BB:CC:11:22:33"
        repository.ingestDiscoveredDevice(mac, "192.168.1.45", "Target")
        repository.setDeviceBlocked(mac, true)

        val removed = repository.removeQuarantinedDevice(mac)
        assertTrue(removed)
        assertTrue(repository.blockedDevices.first().isEmpty())
    }
}

// =============================================================================
// TEST DOUBLES FOR IN-MEMORY TESTING
// =============================================================================

private class FakeSentinelDeviceDao : SentinelDeviceDao {
    private val devices = mutableMapOf<String, SentinelDeviceEntity>()
    private val devicesFlow = MutableStateFlow<List<SentinelDeviceEntity>>(emptyList())

    private fun emit() {
        devicesFlow.value = devices.values.toList()
    }

    override fun getAllDevices(): Flow<List<SentinelDeviceEntity>> = devicesFlow

    override suspend fun getDeviceByMac(mac: String): SentinelDeviceEntity? = devices[mac]

    override suspend fun getDeviceByHash(hash: String): SentinelDeviceEntity? =
        devices.values.firstOrNull { it.anonymizedHash == hash }

    override fun getWhitelistedDevices(): Flow<List<SentinelDeviceEntity>> =
        devicesFlow.map { list -> list.filter { it.isAuthorized } }

    override suspend fun getWhitelistedDevicesList(): List<SentinelDeviceEntity> =
        devices.values.filter { it.isAuthorized }

    override fun getWhitelistedCount(): Flow<Int> =
        devicesFlow.map { list -> list.count { it.isAuthorized } }

    override fun getBlockedDevices(): Flow<List<SentinelDeviceEntity>> =
        devicesFlow.map { list -> list.filter { it.isBlocked } }

    override suspend fun insertOrUpdate(device: SentinelDeviceEntity) {
        devices[device.macAddress] = device
        emit()
    }

    override suspend fun update(device: SentinelDeviceEntity) {
        devices[device.macAddress] = device
        emit()
    }

    override suspend fun setAuthorized(mac: String, isAuthorized: Boolean) {
        devices[mac]?.let {
            devices[mac] = it.copy(isAuthorized = isAuthorized)
            emit()
        }
    }

    override suspend fun setBlocked(mac: String, isBlocked: Boolean) {
        devices[mac]?.let {
            devices[mac] = it.copy(isBlocked = isBlocked)
            emit()
        }
    }

    override suspend fun setCustomName(mac: String, name: String) {
        devices[mac]?.let {
            devices[mac] = it.copy(customName = name)
            emit()
        }
    }

    override suspend fun updateRssiAndTimestamp(mac: String, rssi: Int, timestamp: Long) {
        devices[mac]?.let {
            devices[mac] = it.copy(rssiDbm = rssi, lastSeenTimestamp = timestamp)
            emit()
        }
    }

    override suspend fun deleteDevice(mac: String) {
        devices.remove(mac)
        emit()
    }

    override suspend fun deleteQuarantinedDevices(): Int {
        val blockedKeys = devices.filterValues { it.isBlocked }.keys.toList()
        blockedKeys.forEach { devices.remove(it) }
        emit()
        return blockedKeys.size
    }

    override suspend fun deleteQuarantinedDevice(mac: String): Int {
        val dev = devices[mac]
        return if (dev != null && dev.isBlocked) {
            devices.remove(mac)
            emit()
            1
        } else {
            0
        }
    }

    override suspend fun clearAll() {
        devices.clear()
        emit()
    }
}

private class FakeSentinelAlertDao : SentinelAlertDao {
    private val alerts = mutableMapOf<Long, SentinelAlertEntity>()
    private val alertsFlow = MutableStateFlow<List<SentinelAlertEntity>>(emptyList())
    private var nextId = 1L

    private fun emit() {
        alertsFlow.value = alerts.values.toList()
    }

    override fun getAllAlerts(): Flow<List<SentinelAlertEntity>> = alertsFlow

    override suspend fun getRecentAlertsList(limit: Int): List<SentinelAlertEntity> =
        alerts.values.sortedByDescending { it.timestamp }.take(limit)

    override fun getUnacknowledgedCount(): Flow<Int> =
        alertsFlow.map { list -> list.count { !it.isAcknowledged } }

    override suspend fun insert(alert: SentinelAlertEntity) {
        val assignedId = if (alert.id == 0L) nextId++ else alert.id
        alerts[assignedId] = alert.copy(id = assignedId)
        emit()
    }

    override suspend fun acknowledgeAlert(id: Long) {
        alerts[id]?.let {
            alerts[id] = it.copy(isAcknowledged = true)
            emit()
        }
    }

    override suspend fun markFalsePositive(id: Long) {
        alerts[id]?.let {
            alerts[id] = it.copy(isFalsePositive = true, isAcknowledged = true)
            emit()
        }
    }

    override suspend fun markFalsePositiveByMac(mac: String) {
        alerts.filter { it.value.deviceMac == mac }.forEach { (k, v) ->
            alerts[k] = v.copy(isFalsePositive = true, isAcknowledged = true)
        }
        emit()
    }

    override suspend fun acknowledgeAll() {
        alerts.forEach { (k, v) ->
            alerts[k] = v.copy(isAcknowledged = true)
        }
        emit()
    }

    override suspend fun clearAll() {
        alerts.clear()
        emit()
    }
}
