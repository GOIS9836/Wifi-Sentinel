package com.example.security

/**
 * Data class representing an active gateway shift/anomaly detected by NetworkGuardManager.
 */
data class GatewayAlert(
    val oldGateway: String,
    val newGateway: String,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = "Critical Gateway shift detected from $oldGateway to $newGateway. Possible ARP poisoning or Rogue AP.",
    val isKnownInDatabase: Boolean = false,
    val matchedLabel: String? = null
)

/**
 * UI State container for the Compliant Self-Defense and Privacy Guard.
 */
data class SecurityDashboardUiState(
    val activeAlert: GatewayAlert? = null,
    val intrudersCount: Int = 0,
    val isLockdownActive: Boolean = false,
    val fpFilterEnabled: Boolean = true,
    val fpFilteredCount: Int = 0,
    val lockedGatewayBaseline: String? = null,
    val totalHashedAuditsCount: Int = 0
)
