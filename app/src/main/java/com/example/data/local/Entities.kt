package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "known_devices")
data class NetworkDeviceEntity(
    @PrimaryKey
    val macAddress: String,
    val ipAddress: String,
    val vendor: String,
    val customName: String = "",
    val isAuthorized: Boolean = false,
    val isBlocked: Boolean = false,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "signal_logs")
data class SignalLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val locationName: String,
    val rssiDbm: Int,
    val speedMbps: Int,
    val frequencyMhz: Int,
    val channel: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "security_alerts")
data class SecurityAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val deviceIp: String,
    val deviceMac: String,
    val severity: String, // "CRITICAL", "WARNING", "INFO"
    val timestamp: Long = System.currentTimeMillis(),
    val isAcknowledged: Boolean = false,
    val isFalsePositive: Boolean = false,
    val confidencePercent: Int = 99
)
