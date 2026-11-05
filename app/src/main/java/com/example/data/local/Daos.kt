package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDeviceDao {
    @Query("SELECT * FROM known_devices ORDER BY lastSeen DESC")
    fun getAllDevices(): Flow<List<NetworkDeviceEntity>>

    @Query("SELECT * FROM known_devices WHERE macAddress = :mac LIMIT 1")
    suspend fun getDeviceByMac(mac: String): NetworkDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: NetworkDeviceEntity)

    @Update
    suspend fun update(device: NetworkDeviceEntity)

    @Query("UPDATE known_devices SET isAuthorized = :isAuthorized WHERE macAddress = :mac")
    suspend fun setAuthorized(mac: String, isAuthorized: Boolean)

    @Query("UPDATE known_devices SET isBlocked = :isBlocked WHERE macAddress = :mac")
    suspend fun setBlocked(mac: String, isBlocked: Boolean)

    @Query("UPDATE known_devices SET customName = :name WHERE macAddress = :mac")
    suspend fun setCustomName(mac: String, name: String)

    @Query("SELECT * FROM known_devices WHERE isAuthorized = 1 ORDER BY lastSeen DESC")
    fun getWhitelistedDevices(): Flow<List<NetworkDeviceEntity>>

    @Query("SELECT * FROM known_devices WHERE isAuthorized = 1")
    suspend fun getWhitelistedDevicesList(): List<NetworkDeviceEntity>

    @Query("SELECT COUNT(*) FROM known_devices WHERE isAuthorized = 1")
    fun getWhitelistedCount(): Flow<Int>

    @Query("DELETE FROM known_devices WHERE macAddress = :mac")
    suspend fun deleteDevice(mac: String)

    @Query("DELETE FROM known_devices WHERE isBlocked = 1")
    suspend fun deleteBlockedDevices(): Int

    @Query("DELETE FROM known_devices WHERE macAddress = :mac AND isBlocked = 1")
    suspend fun deleteBlockedDevice(mac: String): Int
}

@Dao
interface SignalLogDao {
    @Query("SELECT * FROM signal_logs ORDER BY timestamp DESC")
    fun getAllSignalLogs(): Flow<List<SignalLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SignalLogEntity)

    @Query("DELETE FROM signal_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM signal_logs")
    suspend fun clearAll()
}

@Dao
interface SecurityAlertDao {
    @Query("SELECT * FROM security_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<SecurityAlertEntity>>

    @Query("SELECT * FROM security_alerts ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentAlertsList(limit: Int = 20): List<SecurityAlertEntity>

    @Query("SELECT COUNT(*) FROM security_alerts WHERE isAcknowledged = 0")
    fun getUnacknowledgedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: SecurityAlertEntity)

    @Query("UPDATE security_alerts SET isAcknowledged = 1 WHERE id = :id")
    suspend fun acknowledgeAlert(id: Long)

    @Query("UPDATE security_alerts SET isFalsePositive = 1, isAcknowledged = 1 WHERE id = :id")
    suspend fun markFalsePositive(id: Long)

    @Query("UPDATE security_alerts SET isFalsePositive = 1, isAcknowledged = 1 WHERE deviceMac = :mac")
    suspend fun markFalsePositiveByMac(mac: String)

    @Query("UPDATE security_alerts SET isAcknowledged = 1")
    suspend fun acknowledgeAll()

    @Query("DELETE FROM security_alerts")
    suspend fun clearAll()
}

@Dao
interface TrustedGatewayDao {
    @Query("SELECT * FROM trusted_gateways ORDER BY isPrimary DESC, lastSeenTimestamp DESC")
    fun getAllTrustedGateways(): Flow<List<TrustedGateway>>

    @Query("SELECT * FROM trusted_gateways WHERE gatewayIp = :ip LIMIT 1")
    suspend fun getGatewayByIp(ip: String): TrustedGateway?

    @Query("SELECT * FROM trusted_gateways WHERE isPrimary = 1 LIMIT 1")
    fun getPrimaryGateway(): Flow<TrustedGateway?>

    @Query("SELECT * FROM trusted_gateways WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryGatewaySync(): TrustedGateway?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(gateway: TrustedGateway)

    @Query("UPDATE trusted_gateways SET isPrimary = 0")
    suspend fun clearPrimaryFlags()

    @Transaction
    suspend fun setAsPrimary(gatewayIp: String) {
        clearPrimaryFlags()
        setPrimaryFlag(gatewayIp, true)
    }

    @Query("UPDATE trusted_gateways SET isPrimary = :isPrimary WHERE gatewayIp = :gatewayIp")
    suspend fun setPrimaryFlag(gatewayIp: String, isPrimary: Boolean)

    @Query("UPDATE trusted_gateways SET lastSeenTimestamp = :timestamp WHERE gatewayIp = :gatewayIp")
    suspend fun updateLastSeen(gatewayIp: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM trusted_gateways WHERE gatewayIp = :gatewayIp")
    suspend fun deleteGateway(gatewayIp: String)

    @Query("DELETE FROM trusted_gateways")
    suspend fun clearAll()
}

@Dao
interface NetworkConnectionHistoryDao {
    @Query("SELECT * FROM network_connection_history ORDER BY connectedTimestamp DESC")
    fun getAllConnectionHistory(): Flow<List<NetworkConnectionHistoryEntity>>

    @Query("SELECT * FROM network_connection_history WHERE ssid = :ssid ORDER BY connectedTimestamp DESC")
    fun getHistoryForSsid(ssid: String): Flow<List<NetworkConnectionHistoryEntity>>

    @Query("SELECT * FROM network_connection_history WHERE bssid = :bssid ORDER BY connectedTimestamp DESC")
    fun getHistoryForBssid(bssid: String): Flow<List<NetworkConnectionHistoryEntity>>

    @Query("SELECT * FROM network_connection_history ORDER BY connectedTimestamp DESC LIMIT :limit")
    suspend fun getRecentConnections(limit: Int = 20): List<NetworkConnectionHistoryEntity>

    @Query("SELECT * FROM network_connection_history WHERE disconnectedTimestamp IS NULL ORDER BY connectedTimestamp DESC LIMIT 1")
    suspend fun getActiveConnection(): NetworkConnectionHistoryEntity?

    @Query("SELECT * FROM network_connection_history WHERE id = :id LIMIT 1")
    suspend fun getConnectionById(id: Long): NetworkConnectionHistoryEntity?

    @Query("SELECT COUNT(*) FROM network_connection_history")
    fun getConnectionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: NetworkConnectionHistoryEntity): Long

    @Update
    suspend fun update(connection: NetworkConnectionHistoryEntity)

    @Query("UPDATE network_connection_history SET disconnectedTimestamp = :disconnectedTimestamp, durationSeconds = :durationSeconds WHERE id = :id")
    suspend fun closeSession(id: Long, disconnectedTimestamp: Long, durationSeconds: Long)

    @Query("UPDATE network_connection_history SET safetyScore = :score WHERE id = :id")
    suspend fun updateSafetyScore(id: Long, score: Int)

    @Query("DELETE FROM network_connection_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM network_connection_history WHERE connectedTimestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM network_connection_history")
    suspend fun clearAll()
}

@Dao
interface DetectedThreatDao {
    @Query("SELECT * FROM detected_threats ORDER BY timestamp DESC")
    fun getAllThreats(): Flow<List<DetectedThreatEntity>>

    @Query("SELECT * FROM detected_threats WHERE isMitigated = 0 AND isFalsePositive = 0 ORDER BY timestamp DESC")
    fun getActiveThreats(): Flow<List<DetectedThreatEntity>>

    @Query("SELECT * FROM detected_threats WHERE severity = :severity ORDER BY timestamp DESC")
    fun getThreatsBySeverity(severity: String): Flow<List<DetectedThreatEntity>>

    @Query("SELECT * FROM detected_threats WHERE threatType = :threatType ORDER BY timestamp DESC")
    fun getThreatsByType(threatType: String): Flow<List<DetectedThreatEntity>>

    @Query("SELECT COUNT(*) FROM detected_threats WHERE isMitigated = 0 AND isFalsePositive = 0")
    fun getActiveThreatCount(): Flow<Int>

    @Query("SELECT * FROM detected_threats WHERE id = :id LIMIT 1")
    suspend fun getThreatById(id: Long): DetectedThreatEntity?

    @Query("SELECT * FROM detected_threats ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentThreatsList(limit: Int = 20): List<DetectedThreatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(threat: DetectedThreatEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(threats: List<DetectedThreatEntity>)

    @Update
    suspend fun update(threat: DetectedThreatEntity)

    @Query("UPDATE detected_threats SET isMitigated = :isMitigated, mitigationAction = :mitigationAction WHERE id = :id")
    suspend fun updateMitigation(id: Long, isMitigated: Boolean, mitigationAction: String)

    @Query("UPDATE detected_threats SET isFalsePositive = 1, isMitigated = 1 WHERE id = :id")
    suspend fun markFalsePositive(id: Long)

    @Query("UPDATE detected_threats SET isFalsePositive = 1, isMitigated = 1 WHERE sourceMac = :mac")
    suspend fun markFalsePositiveByMac(mac: String)

    @Query("DELETE FROM detected_threats WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM detected_threats WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM detected_threats")
    suspend fun clearAll()
}
