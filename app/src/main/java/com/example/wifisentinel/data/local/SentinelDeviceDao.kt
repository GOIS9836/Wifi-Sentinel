package com.example.wifisentinel.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for privacy-compliant device inventory.
 */
@Dao
interface SentinelDeviceDao {

    @Query("SELECT * FROM sentinel_devices ORDER BY lastSeenTimestamp DESC")
    fun getAllDevices(): Flow<List<SentinelDeviceEntity>>

    @Query("SELECT * FROM sentinel_devices WHERE macAddress = :mac LIMIT 1")
    suspend fun getDeviceByMac(mac: String): SentinelDeviceEntity?

    @Query("SELECT * FROM sentinel_devices WHERE anonymizedHash = :hash LIMIT 1")
    suspend fun getDeviceByHash(hash: String): SentinelDeviceEntity?

    @Query("SELECT * FROM sentinel_devices WHERE isAuthorized = 1 ORDER BY lastSeenTimestamp DESC")
    fun getWhitelistedDevices(): Flow<List<SentinelDeviceEntity>>

    @Query("SELECT * FROM sentinel_devices WHERE isAuthorized = 1")
    suspend fun getWhitelistedDevicesList(): List<SentinelDeviceEntity>

    @Query("SELECT COUNT(*) FROM sentinel_devices WHERE isAuthorized = 1")
    fun getWhitelistedCount(): Flow<Int>

    @Query("SELECT * FROM sentinel_devices WHERE isBlocked = 1 ORDER BY lastSeenTimestamp DESC")
    fun getBlockedDevices(): Flow<List<SentinelDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: SentinelDeviceEntity)

    @Update
    suspend fun update(device: SentinelDeviceEntity)

    @Query("UPDATE sentinel_devices SET isAuthorized = :isAuthorized WHERE macAddress = :mac")
    suspend fun setAuthorized(mac: String, isAuthorized: Boolean)

    @Query("UPDATE sentinel_devices SET isBlocked = :isBlocked WHERE macAddress = :mac")
    suspend fun setBlocked(mac: String, isBlocked: Boolean)

    @Query("UPDATE sentinel_devices SET customName = :name WHERE macAddress = :mac")
    suspend fun setCustomName(mac: String, name: String)

    @Query("UPDATE sentinel_devices SET rssiDbm = :rssi, lastSeenTimestamp = :timestamp WHERE macAddress = :mac")
    suspend fun updateRssiAndTimestamp(mac: String, rssi: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM sentinel_devices WHERE macAddress = :mac")
    suspend fun deleteDevice(mac: String)

    @Query("DELETE FROM sentinel_devices WHERE isBlocked = 1")
    suspend fun deleteQuarantinedDevices(): Int

    @Query("DELETE FROM sentinel_devices WHERE macAddress = :mac AND isBlocked = 1")
    suspend fun deleteQuarantinedDevice(mac: String): Int

    @Query("DELETE FROM sentinel_devices")
    suspend fun clearAll()
}
