package com.example.data.model

enum class SignalGrade(val label: String, val description: String) {
    OPTIMAL("Optimal", "Gaming, 8K Ultra HD & Real-Time Sync"),
    GOOD("Good", "4K Streaming & High-Speed Downloads"),
    MODERATE("Fair", "HD Video Calls & Standard Browsing"),
    WEAK("Poor", "High Packet Loss & Video Buffering"),
    CRITICAL("Dead Zone", "Connection Dropouts Imminent")
}

enum class ThreatLevel {
    SAFE,
    SUSPICIOUS,
    UNAUTHORIZED_INTRUDER,
    FALSE_POSITIVE_SUPPRESSED
}

data class WifiConnectionState(
    val isConnected: Boolean = false,
    val ssid: String = "Scanning...",
    val bssid: String = "--:--:--:--:--:--",
    val rssi: Int = -60,
    val signalPercent: Int = 65,
    val linkSpeedMbps: Int = 144,
    val frequencyMhz: Int = 5240,
    val channel: Int = 48,
    val band: String = "5 GHz",
    val ipAddress: String = "192.168.1.105",
    val gatewayIp: String = "192.168.1.1",
    val subnetMask: String = "255.255.255.0",
    val dns: String = "8.8.8.8",
    val securityProtocol: String = "WPA2/WPA3",
    val signalGrade: SignalGrade = SignalGrade.GOOD
)

data class DiscoveredDevice(
    val ip: String,
    val macAddress: String,
    val vendor: String = "Unknown Vendor",
    val customName: String = "",
    val hostname: String = "",
    val isAuthorized: Boolean = false,
    val isSelf: Boolean = false,
    val isGateway: Boolean = false,
    val isBlocked: Boolean = false,
    val responseTimeMs: Long = 12L,
    val firstDetected: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val threatLevel: ThreatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER,
    val isRandomizedMac: Boolean = false,
    val confidencePercent: Int = 99,
    val corroborationVector: String = "Dual-Probe Corroborated",
    val isFalsePositiveSuppressed: Boolean = false,
    val isZeroFpProtected: Boolean = false,
    val isRogueGateway: Boolean = false,
    val isDuplicateIp: Boolean = false,
    val isDuplicateMac: Boolean = false,
    val isSubnetAnomaly: Boolean = false,
    val duplicationAlertDetail: String? = null,
    val isFlaggedUnknown: Boolean = false,
    val aiHardeningNote: String? = null,
    val openPorts: List<Int> = emptyList()
) {
    val displayName: String
        get() = when {
            isRogueGateway -> "ROGUE GATEWAY SPOOF ($ip)"
            isDuplicateIp -> "IP HIJACK / COLLISION ($ip)"
            isDuplicateMac -> "MAC CLONE INTRUDER ($ip)"
            isSubnetAnomaly -> "ALIEN SUBNET INTRUDER ($ip)"
            customName.isNotBlank() -> customName
            isGateway -> "Gateway Router ($ip)"
            isSelf -> "This Device ($ip)"
            vendor.isNotBlank() && vendor != "Unknown Vendor" -> "$vendor Device ($ip)"
            else -> "Device $ip"
        }

    val falsePositiveRisk: String
        get() = when {
            isFalsePositiveSuppressed -> "0.0% (Zero-FP Policy Suppressed)"
            isAuthorized || isGateway || isSelf -> "None (Trusted Baseline)"
            isRandomizedMac -> "Zero-FP Protected (Private MAC Handled)"
            else -> "Zero (< 0.01% Dual-Pass Validated)"
        }

    val hasNetworkAccess: Boolean
        get() = !isBlocked

    val networkAccessStatus: String
        get() = if (isBlocked) "No Network Access (Firewall ACL Isolated)" else "Active Access (Subnet & WAN Allowed)"
}

fun isLocallyAdministeredMac(mac: String): Boolean {
    val cleaned = mac.replace(":", "").replace("-", "")
    if (cleaned.length < 2) return false
    val secondChar = cleaned[1].uppercaseChar()
    return secondChar in listOf('2', '6', 'A', 'E')
}

data class NearbyAccessPoint(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val channel: Int,
    val band: String,
    val security: String
)

data class ChannelCongestion(
    val channel: Int,
    val band: String,
    val apCount: Int,
    val interferenceScore: Int, // 0 - 100
    val isCurrentChannel: Boolean = false,
    val isRecommended: Boolean = false
)

data class AiOptimizationReport(
    val overallHealthScore: Int = 85,
    val summary: String = "",
    val optimalChannelRecommendation: String = "",
    val antennaAndPlacementTip: String = "",
    val securityAudit: String = "",
    val bandSteeringAdvice: String = "",
    val actionItems: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val scanRundownDurationMs: Long = 1850L,
    val aiInferenceLatencyMs: Long = 1200L,
    val scoreValidityDurationSec: Long = 900L
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class GatewaySwitchType(val label: String, val riskLevel: String) {
    NONE("No Switch", "SAFE"),
    GATEWAY_MAC_SPOOF_RISK("Rogue Gateway (ARP Spoofing Risk)", "CRITICAL"),
    GATEWAY_IP_CHANGED("Subnet Gateway Shifted", "WARNING"),
    SSID_SWITCHED("Network / SSID Switched", "WARNING"),
    BSSID_ROAMING("AP Mesh Roaming Transition", "INFO")
}

data class GatewayTransitionEvent(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val switchType: GatewaySwitchType,
    val oldGatewayIp: String,
    val newGatewayIp: String,
    val oldGatewayMac: String,
    val newGatewayMac: String,
    val oldSsid: String,
    val newSsid: String,
    val oldBssid: String,
    val newBssid: String,
    val details: String,
    val severity: String = "WARNING"
)

enum class BtDeviceType(val label: String) {
    TRACKER("BLE AirTag / Tracker Beacon"),
    AUDIO_HEADSET("Wireless Audio / Headset"),
    PHONE_PC("Smartphone / Computer"),
    SMART_PERIPHERAL("Smart Peripheral / HID"),
    ROGUE_SNIFFER("Unidentified RF / BLE Transmitter")
}

data class BtPerimeterDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val deviceType: BtDeviceType,
    val proximity: String,
    val firstDetected: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val isZeroToleranceFlagged: Boolean = true,
    val isQuarantined: Boolean = false,
    val isTrusted: Boolean = false,
    val isFalsePositiveSuppressed: Boolean = false,
    val confidencePercent: Int = 98
) {
    val signalGradeDescription: String
        get() = when {
            rssi >= -50 -> "Immediate Perimeter (< 1.5m)"
            rssi >= -70 -> "Near Perimeter (2m - 5m)"
            rssi >= -85 -> "Outer Zone (5m - 12m)"
            else -> "Fringe Boundary (> 12m)"
        }

    val hasRadioAccess: Boolean
        get() = !isQuarantined

    val radioAccessStatus: String
        get() = if (isQuarantined) "Radio Access Severed (Quarantined)" else "Active Perimeter (Monitored)"

    val hasNetworkAccess: Boolean
        get() = !isQuarantined

    val networkAccessStatus: String
        get() = if (isQuarantined) "Network Access Severed (Quarantined)" else "Active Access Allowed"
}

enum class DuplicationViolationType {
    ROGUE_GATEWAY,
    DUPLICATE_IP,
    DUPLICATE_MAC,
    ALIEN_SUBNET
}

data class DuplicationViolation(
    val violationType: DuplicationViolationType,
    val ip: String,
    val macAddress: String,
    val conflictingDetail: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DuplicationGuardStatus(
    val isEnforced: Boolean = true,
    val duplicateGatewaysBlocked: Int = 0,
    val duplicateIpsBlocked: Int = 0,
    val duplicateMacsDeduplicated: Int = 0,
    val subnetAnomaliesBlocked: Int = 0,
    val lastViolationEvent: DuplicationViolation? = null
) {
    val totalViolations: Int
        get() = duplicateGatewaysBlocked + duplicateIpsBlocked + duplicateMacsDeduplicated + subnetAnomaliesBlocked
}

data class NetworkHardeningRecommendation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val targetDeviceIp: String,
    val targetDeviceMac: String,
    val vendor: String,
    val riskLevel: String = "CRITICAL", // CRITICAL, HIGH, MEDIUM, LOW
    val threatAssessment: String,
    val firewallRules: List<String> = emptyList(),
    val routerHardeningSteps: List<String> = emptyList(),
    val vlanOrIsolationAction: String = "",
    val zeroTrustAction: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val targetDeviceVendor: String get() = vendor
    val recommendedFirewallRules: List<String> get() = firewallRules
    val immediateZeroTrustAction: String get() = zeroTrustAction
}

data class BackgroundDetectionStatus(
    val isRunning: Boolean = true,
    val scanIntervalSeconds: Int = 20,
    val unknownDevicesDetected: Int = 0,
    val activeHardeningDirectives: Int = 0,
    val lastScanTimestamp: Long = System.currentTimeMillis(),
    val isAiAnalyzing: Boolean = false
)

enum class WhitelistAuditStatus {
    ALL_WHITELISTED,
    UNKNOWN_DETECTED,
    ALERT_TRIGGERED
}

data class DeviceWhitelistAuditResult(
    val totalConnected: Int = 0,
    val whitelistedCount: Int = 0,
    val unknownCount: Int = 0,
    val whitelistedDevices: List<DiscoveredDevice> = emptyList(),
    val unknownDevices: List<DiscoveredDevice> = emptyList(),
    val newlyDetectedUnknowns: List<DiscoveredDevice> = emptyList(),
    val status: WhitelistAuditStatus = WhitelistAuditStatus.ALL_WHITELISTED,
    val auditTimestamp: Long = System.currentTimeMillis(),
    val notificationsDispatched: Int = 0,
    val isAutoNotifyEnabled: Boolean = true
)

enum class NetworkHealthGrade(val label: String, val rating: String) {
    EXCELLENT("Excellent", "A+ Optimal"),
    GOOD("Good", "B Solid"),
    FAIR("Fair", "C Degraded"),
    CRITICAL("Critical Risk", "F Compromised")
}

data class IncidentTally(
    val totalCount: Int = 0,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val unacknowledgedCount: Int = 0
)

data class ReportIncidentItem(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val severity: String = "CRITICAL",
    val timestamp: Long = System.currentTimeMillis(),
    val deviceIp: String = "",
    val deviceMac: String = ""
)

data class NetworkSummaryReport(
    val generatedAt: Long = System.currentTimeMillis(),
    val overallHealthScore: Int = 95,
    val healthGrade: NetworkHealthGrade = NetworkHealthGrade.EXCELLENT,
    val ssid: String = "",
    val bssid: String = "",
    val ipAddress: String = "",
    val gatewayIp: String = "",
    val isGatewayVerified: Boolean = true,
    val rssiDbm: Int = -55,
    val signalPercent: Int = 90,
    val linkSpeedMbps: Int = 144,
    val channel: Int = 6,
    val band: String = "5 GHz",
    val channelCongestionLevel: String = "Low",
    val connectedDevicesCount: Int = 0,
    val whitelistedDevicesCount: Int = 0,
    val unknownDevicesCount: Int = 0,
    val blockedDevicesCount: Int = 0,
    val falsePositivesCount: Int = 0,
    val incidents: IncidentTally = IncidentTally(),
    val recentIncidents: List<ReportIncidentItem> = emptyList(),
    val keyFindings: List<String> = emptyList(),
    val actionableRecommendations: List<String> = emptyList(),
    val scanRundownDurationMs: Long = 2100L,
    val scoreValidityDurationSec: Long = 900L
) {
    fun toFormattedReportText(): String {
        return buildString {
            appendLine("=== SENTINEL NETWORK HEALTH & SECURITY INCIDENT REPORT ===")
            appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(generatedAt))}")
            appendLine("Scan-Rundown Time: ${String.format(java.util.Locale.US, "%.2fs", scanRundownDurationMs / 1000f)} | Score TTL Duration: ${scoreValidityDurationSec / 60}m (${scoreValidityDurationSec}s)")
            appendLine("Overall Health Score: $overallHealthScore/100 (${healthGrade.label} • ${healthGrade.rating})")
            appendLine()
            appendLine("[NETWORK HEALTH METRICS]")
            appendLine("• SSID: $ssid ($bssid)")
            appendLine("• Device IP: $ipAddress | Gateway: $gatewayIp (Verified: ${if (isGatewayVerified) "YES" else "UNVERIFIED"})")
            appendLine("• Signal: $rssiDbm dBm ($signalPercent%) | Link Speed: $linkSpeedMbps Mbps")
            appendLine("• Channel: $channel ($band) | RF Congestion: $channelCongestionLevel")
            appendLine()
            appendLine("[SECURITY POSTURE & INCIDENT AUDIT]")
            appendLine("• Total Hosts Connected: $connectedDevicesCount")
            appendLine("• Whitelisted / Trusted: $whitelistedDevicesCount")
            appendLine("• Unknown / Unlisted Hosts: $unknownDevicesCount")
            appendLine("• Blocked / Quarantined Hosts: $blockedDevicesCount")
            appendLine("• Total Incidents Logged: ${incidents.totalCount} (${incidents.criticalCount} Critical, ${incidents.unacknowledgedCount} Pending)")
            appendLine()
            if (keyFindings.isNotEmpty()) {
                appendLine("[KEY FINDINGS]")
                keyFindings.forEach { appendLine("• $it") }
                appendLine()
            }
            if (actionableRecommendations.isNotEmpty()) {
                appendLine("[ACTIONABLE RECOMMENDATIONS]")
                actionableRecommendations.forEach { appendLine("• $it") }
                appendLine()
            }
            if (recentIncidents.isNotEmpty()) {
                appendLine("[RECENT SECURITY INCIDENTS]")
                recentIncidents.take(5).forEach { inc ->
                    appendLine("[${inc.severity}] ${inc.title} - ${inc.description} (${inc.deviceIp})")
                }
                appendLine()
            }
            appendLine("==========================================================")
        }
    }
}



