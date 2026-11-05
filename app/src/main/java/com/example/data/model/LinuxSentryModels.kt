package com.example.data.model

data class LinuxEndpointNode(
    val nodeId: String,
    val hostname: String,
    val ipAddress: String,
    val macAddress: String,
    val distro: String,
    val kernelVersion: String,
    val activeIface: String,
    val isWifi: Boolean = true,
    val wifiSsid: String = "Office_Ultra_5G",
    val wifiRssi: Int = -52,
    val activeThreatsCount: Int = 0,
    val isIsolated: Boolean = false,
    val activeFirewallRulesCount: Int = 0,
    val rogueListeners: List<String> = emptyList(),
    val subnetPeersCount: Int = 4,
    val lastSeenEpochMs: Long = System.currentTimeMillis(),
    val status: NodeClusterStatus = NodeClusterStatus.ONLINE
) {
    val formattedLastSeen: String
        get() {
            val diffSec = (System.currentTimeMillis() - lastSeenEpochMs) / 1000
            return when {
                diffSec < 5 -> "Live / Streaming"
                diffSec < 60 -> "${diffSec}s ago"
                diffSec < 3600 -> "${diffSec / 60}m ago"
                else -> "${diffSec / 3600}h ago"
            }
        }
}

data class LinuxEndpointAlert(
    val id: String,
    val nodeId: String,
    val hostname: String,
    val title: String,
    val severity: String,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class LinuxHubServerState(
    val isRunning: Boolean = true,
    val port: Int = 8721,
    val serverIp: String = "127.0.0.1",
    val ingestedPayloadCount: Int = 0,
    val connectedNodesCount: Int = 0,
    val lastIngestTimestamp: Long = 0L,
    val lastError: String? = null
)
