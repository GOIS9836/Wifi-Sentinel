package com.example.wifisentinel.ui

import com.example.wifisentinel.data.local.SentinelAlertEntity
import com.example.wifisentinel.data.local.SentinelDeviceEntity

/**
 * Tactical UI State for Sentinel Security HUD.
 */
data class SentinelUiState(
    val devices: List<SentinelDeviceEntity> = emptyList(),
    val whitelistedDevices: List<SentinelDeviceEntity> = emptyList(),
    val blockedDevices: List<SentinelDeviceEntity> = emptyList(),
    val alerts: List<SentinelAlertEntity> = emptyList(),
    val unacknowledgedAlertsCount: Int = 0,
    val isLoading: Boolean = false,
    val selectedFilter: DeviceFilter = DeviceFilter.ALL,
    val complianceStatus: String = "POTRAZ Chapter 12:07 Compliant",
    val classification: String = "BENEDICTUS"
)

enum class DeviceFilter {
    ALL,
    WHITELISTED,
    BLOCKED,
    RANDOMIZED_LAA
}
