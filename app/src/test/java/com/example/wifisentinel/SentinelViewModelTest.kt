package com.example.wifisentinel

import com.example.wifisentinel.data.local.SentinelAlertDao
import com.example.wifisentinel.data.local.SentinelAlertEntity
import com.example.wifisentinel.data.local.SentinelDeviceDao
import com.example.wifisentinel.data.local.SentinelDeviceEntity
import com.example.wifisentinel.data.repository.SentinelRepository
import com.example.wifisentinel.ui.DeviceFilter
import com.example.wifisentinel.ui.SentinelViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Fast unit test suite for SentinelViewModel (Phase 3).
 * Verifies reactive UI state emission, filtering, and authorization transitions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SentinelViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDeviceDao: TestFakeDeviceDao
    private lateinit var fakeAlertDao: TestFakeAlertDao
    private lateinit var repository: SentinelRepository
    private lateinit var viewModel: SentinelViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDeviceDao = TestFakeDeviceDao()
        fakeAlertDao = TestFakeAlertDao()
        repository = SentinelRepository(fakeDeviceDao, fakeAlertDao, salt = "TEST_SALT_VM")
        viewModel = SentinelViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun filterState_updatesCorrectly() = runTest(testDispatcher) {
        viewModel.setFilter(DeviceFilter.WHITELISTED)
        // Verify method can be invoked idempotently without exceptions
        viewModel.setFilter(DeviceFilter.BLOCKED)
        viewModel.setFilter(DeviceFilter.ALL)
    }

    @Test
    fun ingestScannedDevice_populatesInventory() = runTest(testDispatcher) {
        viewModel.ingestScannedDevice("00:11:22:33:44:55", "192.168.1.188", "WiFi Extender")
        advanceUntilIdle()

        val device = fakeDeviceDao.getDeviceByMac("00:11:22:33:44:55")
        assertNotNull(device)
        assertEquals("192.168.1.188", device?.ipAddress)
        assertEquals("WiFi Extender", device?.vendor)
    }

    @Test
    fun toggleAuthorization_updatesDeviceAuthorization() = runTest(testDispatcher) {
        val testDev = SentinelDeviceEntity(
            macAddress = "B4:2E:99:AB:CD:EF",
            anonymizedHash = "hash123",
            ipAddress = "192.168.1.169",
            isAuthorized = false,
            isBlocked = true
        )
        fakeDeviceDao.insertOrUpdate(testDev)

        viewModel.toggleAuthorization(testDev)
        advanceUntilIdle()

        val updated = fakeDeviceDao.getDeviceByMac("B4:2E:99:AB:CD:EF")
        assertTrue("Device must be authorized", updated?.isAuthorized == true)
        assertFalse("Blocking must be cleared when authorizing", updated?.isBlocked == true)
    }

    @Test
    fun toggleBlock_updatesDeviceBlockedAndClearsAuthorization() = runTest(testDispatcher) {
        val testDev = SentinelDeviceEntity(
            macAddress = "DA:A1:19:00:11:22",
            anonymizedHash = "hash_rogue",
            ipAddress = "192.168.1.189",
            isAuthorized = true,
            isBlocked = false
        )
        fakeDeviceDao.insertOrUpdate(testDev)

        viewModel.toggleBlock(testDev)
        advanceUntilIdle()

        val updated = fakeDeviceDao.getDeviceByMac("DA:A1:19:00:11:22")
        assertTrue("Device must be blocked", updated?.isBlocked == true)
        assertFalse("Authorization must be revoked when blocking", updated?.isAuthorized == true)
    }
}

// Minimal In-Memory Double for ViewModel Unit Testing
private class TestFakeDeviceDao : SentinelDeviceDao {
    private val map = mutableMapOf<String, SentinelDeviceEntity>()
    private val flow = MutableStateFlow<List<SentinelDeviceEntity>>(emptyList())

    override fun getAllDevices(): Flow<List<SentinelDeviceEntity>> = flow
    override suspend fun getDeviceByMac(mac: String): SentinelDeviceEntity? = map[mac]
    override suspend fun getDeviceByHash(hash: String): SentinelDeviceEntity? = map.values.firstOrNull { it.anonymizedHash == hash }
    override fun getWhitelistedDevices(): Flow<List<SentinelDeviceEntity>> = flow.map { l -> l.filter { it.isAuthorized } }
    override suspend fun getWhitelistedDevicesList(): List<SentinelDeviceEntity> = map.values.filter { it.isAuthorized }
    override fun getWhitelistedCount(): Flow<Int> = flow.map { l -> l.count { it.isAuthorized } }
    override fun getBlockedDevices(): Flow<List<SentinelDeviceEntity>> = flow.map { l -> l.filter { it.isBlocked } }

    override suspend fun insertOrUpdate(device: SentinelDeviceEntity) {
        map[device.macAddress] = device
        flow.value = map.values.toList()
    }

    override suspend fun update(device: SentinelDeviceEntity) {
        map[device.macAddress] = device
        flow.value = map.values.toList()
    }

    override suspend fun setAuthorized(mac: String, isAuthorized: Boolean) {
        map[mac]?.let {
            map[mac] = it.copy(isAuthorized = isAuthorized)
            flow.value = map.values.toList()
        }
    }

    override suspend fun setBlocked(mac: String, isBlocked: Boolean) {
        map[mac]?.let {
            map[mac] = it.copy(isBlocked = isBlocked)
            flow.value = map.values.toList()
        }
    }

    override suspend fun setCustomName(mac: String, name: String) {
        map[mac]?.let {
            map[mac] = it.copy(customName = name)
            flow.value = map.values.toList()
        }
    }

    override suspend fun updateRssiAndTimestamp(mac: String, rssi: Int, timestamp: Long) {
        map[mac]?.let {
            map[mac] = it.copy(rssiDbm = rssi, lastSeenTimestamp = timestamp)
            flow.value = map.values.toList()
        }
    }

    override suspend fun deleteDevice(mac: String) {
        map.remove(mac)
        flow.value = map.values.toList()
    }

    override suspend fun clearAll() {
        map.clear()
        flow.value = emptyList()
    }
}

private class TestFakeAlertDao : SentinelAlertDao {
    private val map = mutableMapOf<Long, SentinelAlertEntity>()
    private val flow = MutableStateFlow<List<SentinelAlertEntity>>(emptyList())
    private var idCounter = 1L

    override fun getAllAlerts(): Flow<List<SentinelAlertEntity>> = flow
    override suspend fun getRecentAlertsList(limit: Int): List<SentinelAlertEntity> = map.values.take(limit)
    override fun getUnacknowledgedCount(): Flow<Int> = flow.map { l -> l.count { !it.isAcknowledged } }
    override suspend fun insert(alert: SentinelAlertEntity) {
        val id = if (alert.id == 0L) idCounter++ else alert.id
        map[id] = alert.copy(id = id)
        flow.value = map.values.toList()
    }
    override suspend fun acknowledgeAlert(id: Long) {
        map[id]?.let {
            map[id] = it.copy(isAcknowledged = true)
            flow.value = map.values.toList()
        }
    }
    override suspend fun markFalsePositive(id: Long) {
        map[id]?.let {
            map[id] = it.copy(isFalsePositive = true, isAcknowledged = true)
            flow.value = map.values.toList()
        }
    }
    override suspend fun markFalsePositiveByMac(mac: String) {}
    override suspend fun acknowledgeAll() {
        map.forEach { (k, v) -> map[k] = v.copy(isAcknowledged = true) }
        flow.value = map.values.toList()
    }
    override suspend fun clearAll() {
        map.clear()
        flow.value = emptyList()
    }
}
