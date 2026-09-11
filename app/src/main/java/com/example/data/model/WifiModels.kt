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
    val aiHardeningNote: String? = null
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
    val timestamp: Long = System.currentTimeMillis()
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

