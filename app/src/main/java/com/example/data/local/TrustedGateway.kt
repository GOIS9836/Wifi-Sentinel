package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_gateways")
data class TrustedGateway(
    @PrimaryKey val gatewayIp: String,      // e.g., "192.168.1.1", "10.0.0.1"
    val bssid: String?,                    // MAC yechipangidzo (kana iripo/yakareruka)
    val ssid: String,                      // Network Name (e.g., "Office_Mesh_Node1")
    val subnetMask: String,                // e.g., "255.255.255.0"
    val label: String,                     // e.g., "Main Router (Lounge)", "AP Upstairs"
    val isPrimary: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
