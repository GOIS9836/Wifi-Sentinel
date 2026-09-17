package com.example.wifisentinel.data.repository

import com.example.wifisentinel.data.local.SentinelAlertDao
import com.example.wifisentinel.data.local.SentinelAlertEntity
import com.example.wifisentinel.data.local.SentinelDeviceDao
import com.example.wifisentinel.data.local.SentinelDeviceEntity
import com.example.wifisentinel.security.MacSanitizer
import kotlinx.coroutines.flow.Flow

/**
 * High-level repository coordinating device inventory, threat alerting,
 * and POTRAZ Chapter 12:07 compliant identifier anonymization.
 */
class SentinelRepository(
    private val deviceDao: SentinelDeviceDao,
    private val alertDao: SentinelAlertDao,
    private val salt: String = "WiFiSentinel-POTRAZ-V1"
) {
    val allDevices: Flow<List<SentinelDeviceEntity>> = deviceDao.getAllDevices()
    val whitelistedDevices: Flow<List<SentinelDeviceEntity>> = deviceDao.getWhitelistedDevices()
    val whitelistedCount: Flow<Int> = deviceDao.getWhitelistedCount()
    val blockedDevices: Flow<List<SentinelDeviceEntity>> = deviceDao.getBlockedDevices()
    val securityAlerts: Flow<List<SentinelAlertEntity>> = alertDao.getAllAlerts()
    val unacknowledgedAlertsCount: Flow<Int> = alertDao.getUnacknowledgedCount()

    /**
     * Ingests a raw discovered endpoint, automatically applying POTRAZ Chapter 12:07
     * anonymization and LAA MAC randomization detection.
     */
    suspend fun ingestDiscoveredDevice(
        rawMac: String,
        ipAddress: String,
        vendor: String = "Unknown",
        rssiDbm: Int = -65,
        frequencyMhz: Int = 2412,
        initialAuthorized: Boolean = false
    ): SentinelDeviceEntity {
        val normalizedMac = rawMac.trim().uppercase()
        val isRandom = MacSanitizer.isRandomizedMac(normalizedMac)
        val anonymizedHash = MacSanitizer.hashIdentifier(normalizedMac, salt)

        val existing = deviceDao.getDeviceByMac(normalizedMac)
        val entity = if (existing != null) {
            existing.copy(
                ipAddress = ipAddress,
                vendor = if (vendor != "Unknown") vendor else existing.vendor,
                rssiDbm = rssiDbm,
                frequencyMhz = frequencyMhz,
                lastSeenTimestamp = System.currentTimeMillis()
            )
        } else {
            SentinelDeviceEntity(
                macAddress = normalizedMac,
                anonymizedHash = anonymizedHash,
                ipAddress = ipAddress,
                vendor = vendor,
                isAuthorized = initialAuthorized,
                isBlocked = false,
                isRandomizedMac = isRandom,
                firstSeenTimestamp = System.currentTimeMillis(),
                lastSeenTimestamp = System.currentTimeMillis(),
                rssiDbm = rssiDbm,
                frequencyMhz = frequencyMhz
            )
        }

        deviceDao.insertOrUpdate(entity)
        return entity
    }

    suspend fun setDeviceAuthorized(mac: String, authorized: Boolean) {
        deviceDao.setAuthorized(mac.trim().uppercase(), authorized)
    }

    suspend fun setDeviceBlocked(mac: String, blocked: Boolean) {
        deviceDao.setBlocked(mac.trim().uppercase(), blocked)
    }

    suspend fun setCustomName(mac: String, name: String) {
        deviceDao.setCustomName(mac.trim().uppercase(), name)
    }

    suspend fun recordSecurityAlert(
        title: String,
        description: String,
        deviceIp: String,
        deviceMac: String,
        severity: String = "WARNING"
    ): SentinelAlertEntity {
        val normalizedMac = deviceMac.trim().uppercase()
        val hash = MacSanitizer.hashIdentifier(normalizedMac, salt)
        val alert = SentinelAlertEntity(
            title = title,
            description = description,
            deviceIp = deviceIp,
            deviceMac = normalizedMac,
            anonymizedHash = hash,
            severity = severity,
            timestamp = System.currentTimeMillis()
        )
        alertDao.insert(alert)
        return alert
    }

    suspend fun acknowledgeAlert(alertId: Long) {
        alertDao.acknowledgeAlert(alertId)
    }

    suspend fun markAlertFalsePositive(alertId: Long) {
        alertDao.markFalsePositive(alertId)
    }

    suspend fun clearAllData() {
        deviceDao.clearAll()
        alertDao.clearAll()
    }
}
