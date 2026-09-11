package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("DELETE FROM known_devices WHERE macAddress = :mac")
    suspend fun deleteDevice(mac: String)
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
