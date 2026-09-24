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
