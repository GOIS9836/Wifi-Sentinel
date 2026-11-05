package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NetworkDeviceEntity::class,
        SignalLogEntity::class,
        SecurityAlertEntity::class,
        TrustedGateway::class,
        com.example.wifisentinel.data.local.SentinelDeviceEntity::class,
        com.example.wifisentinel.data.local.SentinelAlertEntity::class,
        NetworkConnectionHistoryEntity::class,
        DetectedThreatEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun networkDeviceDao(): NetworkDeviceDao
    abstract fun signalLogDao(): SignalLogDao
    abstract fun securityAlertDao(): SecurityAlertDao
    abstract fun trustedGatewayDao(): TrustedGatewayDao
    abstract fun sentinelDeviceDao(): com.example.wifisentinel.data.local.SentinelDeviceDao
    abstract fun sentinelAlertDao(): com.example.wifisentinel.data.local.SentinelAlertDao
    abstract fun networkConnectionHistoryDao(): NetworkConnectionHistoryDao
    abstract fun detectedThreatDao(): DetectedThreatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wifi_sentinel.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
