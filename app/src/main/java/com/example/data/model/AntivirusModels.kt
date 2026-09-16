package com.example.data.model

import androidx.compose.ui.graphics.vector.ImageVector

enum class AppRiskLevel(val label: String) {
    SAFE("Safe"),
    SUSPICIOUS("Suspicious"),
    HIGH_RISK("High Risk"),
    CRITICAL("Critical Threat")
}

data class AppSecurityScanResult(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val installerSource: String, // "Google Play", "Sideloaded / APK", "System"
    val riskLevel: AppRiskLevel,
    val riskScore: Int, // 0 - 100
    val riskReasons: List<String> = emptyList(),
    val dangerousPermissions: List<String> = emptyList(),
    val isQuarantined: Boolean = false,
    val isWhitelisted: Boolean = false,
    val appSizeBytes: Long = 0L,
    val targetSdkVersion: Int = 34
)

enum class JunkType {
    APP_CACHE,
    TEMP_FILES,
    LOG_BUFFERS,
    MEMORY_CACHE
}

data class JunkCategoryItem(
    val id: JunkType,
    val title: String,
    val description: String,
    val sizeBytes: Long,
    val itemCount: Int,
    val isSelected: Boolean = true
)

data class JunkCleanResult(
    val totalBytesCleaned: Long = 0L,
    val itemsRemoved: Int = 0,
    val ramFreedMb: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

data class SystemSecurityAudit(
    val isRootDetected: Boolean = false,
    val isUsbDebuggingEnabled: Boolean = false,
    val isDeveloperOptionsEnabled: Boolean = false,
    val isLockScreenSecure: Boolean = true,
    val isUnknownSourcesEnabled: Boolean = false,
    val overallSecurityScore: Int = 95,
    val activeIssuesCount: Int = 0,
    val vulnerabilities: List<String> = emptyList(),
    val hardeningRecommendations: List<String> = emptyList()
)

data class AntivirusScannerState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val currentlyScanningApp: String = "",
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val threatsFound: Int = 0,
    val lastScanTimestamp: Long = 0L
)

data class DeviceStorageMemoryMetrics(
    val totalRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val ramUsagePercent: Int = 0,
    val totalStorageBytes: Long = 0L,
    val freeStorageBytes: Long = 0L,
    val appCacheBytes: Long = 0L,
    val estimatedJunkBytes: Long = 0L
)

data class DailyScanScheduleSettings(
    val isEnabled: Boolean = false,
    val hour: Int = 2, // 24h format (e.g. 2 = 02:00 AM, 14 = 02:00 PM)
    val minute: Int = 0,
    val scanAntivirus: Boolean = true,
    val scanJunkCleaner: Boolean = true,
    val autoCleanSafeJunk: Boolean = true,
    val lastRunTimestamp: Long = 0L,
    val lastScannedAppsCount: Int = 0,
    val lastRunThreatsCount: Int = 0,
    val lastRunJunkBytes: Long = 0L,
    val lastRunSummary: String = ""
) {
    val lastDetectedThreatsCount: Int get() = lastRunThreatsCount
    val lastCleanedBytes: Long get() = lastRunJunkBytes
}
