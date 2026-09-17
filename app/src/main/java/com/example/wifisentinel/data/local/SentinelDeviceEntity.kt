package com.example.wifisentinel.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * POTRAZ Chapter 12:07 compliant persistent entity for discovered network endpoints.
 * Plain identifiers (MAC and IP) are stored alongside cryptographically salted SHA-256
 * anonymized hashes and LAA randomization flags.
 */
@Entity(tableName = "sentinel_devices")
data class SentinelDeviceEntity(
    @PrimaryKey
    val macAddress: String,
    val anonymizedHash: String,
    val ipAddress: String,
    val vendor: String = "Unknown",
    val customName: String = "",
    val isAuthorized: Boolean = false,
    val isBlocked: Boolean = false,
    val isRandomizedMac: Boolean = false,
    val firstSeenTimestamp: Long = System.currentTimeMillis(),
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val rssiDbm: Int = -65,
    val frequencyMhz: Int = 2412
)
