package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
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

/**
 * Historical network connection data entity for WiFi Sentinel.
 * Records WiFi connection sessions, network metrics, gateway details,
 * and POTRAZ CDPA [Chapter 12:07] data minimization pseudonym hashes.
 */
@Entity(
    tableName = "network_connection_history",
    indices = [
        Index(value = ["bssid"]),
        Index(value = ["ssid"]),
        Index(value = ["connectedTimestamp"]),
        Index(value = ["pseudonymHash"])
    ]
)
data class NetworkConnectionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ssid: String,
    val bssid: String,
    val gatewayIp: String,
    val gatewayMac: String,
    val localIp: String,
    val frequencyMhz: Int,
    val channel: Int,
    val securityType: String, // e.g., "WPA3-SAE", "WPA2-PSK", "WPA/WPA2-Enterprise", "OPEN", "WEP"
    val rssiDbm: Int,
    val linkSpeedMbps: Int,
    val connectedTimestamp: Long = System.currentTimeMillis(),
    val disconnectedTimestamp: Long? = null,
    val durationSeconds: Long? = null,
    val safetyScore: Int = 100, // 0 - 100 safety score
    val pseudonymHash: String, // POTRAZ Chapter 12:07 SHA-256 pseudonym hash
    val isCaptivePortal: Boolean = false,
    val dnsServers: String = "",
    val isVpnActive: Boolean = false,
    val notes: String = ""
)

/**
 * Detected security threat entity for WiFi Sentinel.
 * Persists detected anomalies such as Evil Twins, SSL Stripping / Downgrade attacks,
 * ARP Spoofing / Poisoning, Rogue Access Points, Rogue Listeners, and Deauthentication Floods.
 * Complies with POTRAZ CDPA [Chapter 12:07] data minimization standards via pseudonymization.
 */
@Entity(
    tableName = "detected_threats",
    indices = [
        Index(value = ["threatType"]),
        Index(value = ["severity"]),
        Index(value = ["timestamp"]),
        Index(value = ["sourceMac"]),
        Index(value = ["pseudonymHash"]),
        Index(value = ["isMitigated"])
    ]
)
data class DetectedThreatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threatType: String, // "EVIL_TWIN", "DOWNGRADE_ATTACK", "ARP_SPOOFING", "ROGUE_AP", "ROGUE_LISTENER", "KARMA_ATTACK", "DEAUTH_FLOOD", "SUSPICIOUS_PROBE"
    val severity: String, // "CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"
    val title: String,
    val description: String,
    val sourceIp: String,
    val sourceMac: String,
    val pseudonymHash: String, // Salted SHA-256 hash for POTRAZ compliance
    val targetSsid: String,
    val targetBssid: String,
    val channel: Int = 0,
    val rssiDbm: Int = 0,
    val mitigationAction: String = "",
    val isMitigated: Boolean = false,
    val isFalsePositive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val confidencePercent: Int = 95
)
