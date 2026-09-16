package com.example.data.model

import java.util.UUID

enum class UnethicalThreatType(
    val title: String,
    val category: String,
    val defaultSeverity: String,
    val description: String
) {
    ARP_POISONER(
        "ARP Cache Poisoner (MITM)",
        "Interception & Eavesdropping",
        "CRITICAL",
        "Transmitting unsolicited gratuitous ARP replies to hijack gateway traffic and perform Man-in-the-Middle wiretapping."
    ),
    PROMISCUOUS_SNIFFER(
        "Promiscuous Packet Sniffer",
        "Passive Wiretapping",
        "HIGH",
        "NIC set to promiscuous mode capturing all raw broadcast, multicast, and unicast packets across the subnet."
    ),
    EVIL_TWIN_CLONE(
        "Evil Twin / Rogue AP Cloner",
        "Identity Impersonation",
        "CRITICAL",
        "Broadcasting duplicate SSID beacons with mismatched encryption handshakes to entice client credential theft."
    ),
    DEAUTH_FLOODER(
        "802.11 Deauth / Jamming Flooder",
        "Denial of Service",
        "HIGH",
        "Injecting forged 802.11 management deauthentication frames to violently sever client connections."
    ),
    PORT_SCANNER(
        "Aggressive Port Scanner / Exploit Probe",
        "Active Reconnaissance",
        "HIGH",
        "Executing multi-port SYN sweeps against internal infrastructure targeting services 21, 22, 23, 80, 445, 3389."
    ),
    MAC_CLOAKED_IMPOSTOR(
        "MAC Cloaked Impostor",
        "Evasion & Stealth",
        "MEDIUM",
        "Rapidly cycling locally administered random MAC octets to evade firewall ACLs and subnet whitelisting."
    )
}

data class UnethicalDevice(
    val id: String = UUID.randomUUID().toString(),
    val ip: String,
    val mac: String,
    val vendor: String = "Unknown Host",
    val threatType: UnethicalThreatType,
    val severity: String = "CRITICAL", // CRITICAL, HIGH, MEDIUM
    val signatureDetail: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val isQuarantined: Boolean = false,
    val packetAnomalyCount: Int = 142
)

data class ThreatVectorBreakdown(
    val vectorName: String,
    val score: Int, // 0 to 100
    val severity: String, // "SAFE", "WARNING", "CRITICAL"
    val detail: String
)

data class NetworkRiskAssessment(
    val riskScore: Int = 15, // 0 to 100
    val riskLevel: String = "SECURE", // SECURE, LOW, MODERATE, HIGH, CRITICAL
    val summary: String = "Network perimeter verified clean with zero unauthorized penetrations.",
    val threatVectors: List<ThreatVectorBreakdown> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val isGeminiLive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

enum class GatewayRouterType(val label: String) {
    PRIMARY_DEFAULT("Primary Default Gateway"),
    MESH_SATELLITE("Mesh Satellite AP / Node"),
    SECONDARY_GATEWAY("Secondary Subnet Gateway"),
    VIRTUAL_BRIDGE("Virtual / Container Bridge"),
    ROGUE_DUPLICATE("Rogue / Duplicate Impostor")
}

enum class GatewayFilterCategory(val label: String) {
    ALL("All Gateways"),
    PRIMARY("Primary Default"),
    MESH_NODES("Mesh Satellite Nodes"),
    SECONDARY("Secondary Subnets"),
    VIRTUAL("Virtual / Bridges"),
    ROGUE_DUPLICATE("Rogue / Duplicates")
}

data class GatewayRouterNode(
    val id: String = UUID.randomUUID().toString(),
    val ip: String,
    val mac: String,
    val ssid: String,
    val bssid: String,
    val type: GatewayRouterType,
    val vendor: String,
    val hopMetric: Int = 1,
    val latencyMs: Long = 2L,
    val isCurrentActive: Boolean = false,
    val isLocked: Boolean = true,
    val isQuarantined: Boolean = false,
    val filterPolicy: String = "STRICT_ACL_MONITORED", // STRICT_ACL_MONITORED, BYPASS, QUARANTINE_DROP
    val trafficVolumeMb: Float = 14.5f,
    val routeSubnet: String = "192.168.1.0/24",
    val lastSeen: Long = System.currentTimeMillis()
)
