package com.example.data.model

enum class NodeClusterStatus(val label: String) {
    ONLINE("Active / Online"),
    SYNCING("Synchronizing"),
    STANDBY("Standby / Ready"),
    ISOLATED("Isolated / Contained"),
    HARDENED("Hardened Core")
}

data class PergamusClusterNode(
    val nodeId: String,
    val clusterName: String = "Nodes Pergamus",
    val displayName: String,
    val ipAddress: String,
    val macAddress: String,
    val role: String,
    val status: NodeClusterStatus = NodeClusterStatus.ONLINE,
    val latencyMs: Long = 12L,
    val uptimePercent: Double = 99.98,
    val lastHeartbeatFormatted: String = "Just now",
    val sourceDataCount: Int = 142,
    val trialCount: Int = 38
)

enum class TrialCategory(val label: String, val iconText: String) {
    ALL("All Records", "📑"),
    SECURITY_TRIAL("Attack Trials", "🎯"),
    PROTOCOL_ERROR("Protocol Errors", "⚠️"),
    PACKET_TRACE("Packet Traces", "🔬"),
    HARDENING_BENCHMARK("Hardening Benchmarks", "🛡️"),
    SOURCE_DATA_CAPTURE("Raw Source-Data", "💾")
}

enum class TrialOutcome(val label: String) {
    PASSED("Passed / Verified"),
    MITIGATED("Mitigated"),
    REPRODUCED("Error Reproduced"),
    ISOLATED("Threat Isolated"),
    FLAGGED("Review Flagged")
}

data class TrialErrorEntry(
    val id: String,
    val title: String,
    val category: TrialCategory,
    val protocol: String,
    val sourceNode: String,
    val outcome: TrialOutcome,
    val rootCause: String,
    val sourceDataPayloadHex: String,
    val sourceDataSummary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val testIterations: Int = 1
)

data class ClusterBackupSnapshot(
    val backupId: String,
    val workspaceName: String = "Global Opifex Innovative Solutions",
    val clusterTarget: String = "Nodes Pergamus (All Clusters)",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String,
    val nodeCount: Int,
    val devicesCount: Int,
    val trialsCount: Int,
    val sizeKb: Long,
    val checksumSha256: String,
    val isNativeStored: Boolean = true,
    val rawJsonPayload: String
)

data class RecurringBetaTester(
    val testerId: String,
    val email: String,
    val name: String,
    val flightRank: String,
    val tier: String = "Recurring Beta Flight",
    val iterationsCompleted: Int,
    val activeStreakDays: Int,
    val devicePlatform: String,
    val lastActiveFormatted: String = "Active Today",
    val flightChannel: String = "Opifex Beta Ring 4.2",
    val notes: String = ""
)

data class BetaFlightRunResult(
    val flightId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val testsExecuted: Int,
    val passedCount: Int,
    val flaggedCount: Int,
    val durationMs: Long,
    val summaryReport: String
)

data class WorkspaceProfile(
    val id: String,
    val name: String,
    val organization: String = "Global Opifex Innovative Solutions",
    val organizationCode: String = "OPX-CORP-9836",
    val tier: String = "Enterprise Defense Cluster",
    val activeNodesCount: Int,
    val totalBackupsCount: Int,
    val isCurrent: Boolean = false,
    val description: String = ""
)
