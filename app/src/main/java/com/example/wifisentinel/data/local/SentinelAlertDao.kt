package com.example.wifisentinel.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for security alert management.
 */
@Dao
interface SentinelAlertDao {

    @Query("SELECT * FROM sentinel_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<SentinelAlertEntity>>

    @Query("SELECT * FROM sentinel_alerts ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentAlertsList(limit: Int = 20): List<SentinelAlertEntity>

    @Query("SELECT COUNT(*) FROM sentinel_alerts WHERE isAcknowledged = 0")
    fun getUnacknowledgedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: SentinelAlertEntity)

    @Query("UPDATE sentinel_alerts SET isAcknowledged = 1 WHERE id = :id")
    suspend fun acknowledgeAlert(id: Long)

    @Query("UPDATE sentinel_alerts SET isFalsePositive = 1, isAcknowledged = 1 WHERE id = :id")
    suspend fun markFalsePositive(id: Long)

    @Query("UPDATE sentinel_alerts SET isFalsePositive = 1, isAcknowledged = 1 WHERE deviceMac = :mac")
    suspend fun markFalsePositiveByMac(mac: String)

    @Query("UPDATE sentinel_alerts SET isAcknowledged = 1")
    suspend fun acknowledgeAll()

    @Query("DELETE FROM sentinel_alerts")
    suspend fun clearAll()
}
