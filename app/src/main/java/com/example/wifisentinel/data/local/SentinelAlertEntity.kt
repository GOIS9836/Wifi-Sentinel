package com.example.wifisentinel.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * High-integrity security alert entity recording rogue endpoints, ARP anomalies,
 * and gateway spoofing attempts.
 */
@Entity(tableName = "sentinel_alerts")
data class SentinelAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val deviceIp: String,
    val deviceMac: String,
    val anonymizedHash: String,
    val severity: String, // "CRITICAL", "WARNING", "INFO"
    val timestamp: Long = System.currentTimeMillis(),
    val isAcknowledged: Boolean = false,
    val isFalsePositive: Boolean = false,
    val confidenceScore: Int = 99
)
