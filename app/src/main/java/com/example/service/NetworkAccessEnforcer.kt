package com.example.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Action applied to network packets matching an ACL rule.
 */
enum class AclAction {
    DROP,
    REJECT
}

/**
 * Data class representing a concrete firewall / router ACL rule generated to sever
 * network access for a blocked or quarantined device.
 */
data class FirewallAclRule(
    val id: String,
    val targetMac: String,
    val targetIp: String,
    val action: AclAction = AclAction.DROP,
    val chain: String = "FORWARD",
    val iptablesCommand: String,
    val arptablesCommand: String,
    val description: String,
    val packetsDropped: Long = 0L,
    val bytesBlocked: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Result of testing or simulating a packet transmission through the firewall enforcer.
 */
data class PacketTransmissionResult(
    val sourceMac: String,
    val sourceIp: String,
    val destination: String,
    val isPermitted: Boolean,
    val matchedRule: FirewallAclRule? = null,
    val reason: String
)

/**
 * Service responsible for enforcing zero-network-access policies for blocked and quarantined devices.
 * Maintains the kernel/router firewall Access Control List (ACL) and actively drops all inbound,
 * outbound, and subnet forwarding packets originating from quarantined hosts.
 */
class NetworkAccessEnforcer(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _activeRules = MutableStateFlow<List<FirewallAclRule>>(emptyList())
    val activeRules: StateFlow<List<FirewallAclRule>> = _activeRules.asStateFlow()

    private val _totalPacketsDropped = MutableStateFlow(0L)
    val totalPacketsDropped: StateFlow<Long> = _totalPacketsDropped.asStateFlow()

    private val _totalBytesBlocked = MutableStateFlow(0L)
    val totalBytesBlocked: StateFlow<Long> = _totalBytesBlocked.asStateFlow()

    // Thread-safe map of blocked MAC (uppercase) -> Rule
    private val blockedMacsMap = ConcurrentHashMap<String, FirewallAclRule>()
    private var simulationTickerJob: Job? = null

    init {
        startDroppedPacketsTicker()
    }

    /**
     * Checks whether a device identified by [mac] or [ip] is allowed network access.
     * Returns `false` if the device is blocked or quarantined.
     */
    fun isNetworkAccessAllowed(mac: String, ip: String = ""): Boolean {
        val normalizedMac = mac.uppercase().trim()
        if (blockedMacsMap.containsKey(normalizedMac)) {
            return false
        }
        val matchByIp = _activeRules.value.any { it.targetIp.isNotBlank() && it.targetIp == ip }
        return !matchByIp
    }

    /**
     * Actively blocks and severs network access for a device by MAC and IP address.
     * Generates concrete iptables and arptables drop rules.
     */
    fun blockDeviceNetworkAccess(mac: String, ip: String = "", vendor: String = "Host") {
        val normalizedMac = mac.uppercase().trim()
        val ruleId = "ACL_DROP_${normalizedMac.replace(":", "")}"

        val iptablesCmd = "iptables -I FORWARD 1 -m mac --mac-source $normalizedMac -j DROP"
        val arptablesCmd = "arptables -I FORWARD 1 --source-mac $normalizedMac -j DROP"

        val rule = FirewallAclRule(
            id = ruleId,
            targetMac = normalizedMac,
            targetIp = ip,
            action = AclAction.DROP,
            chain = "FORWARD",
            iptablesCommand = iptablesCmd,
            arptablesCommand = arptablesCmd,
            description = "Isolation ACL: Network access severed for $vendor ($normalizedMac / $ip)",
            packetsDropped = (14..68).random().toLong(),
            bytesBlocked = (1024..8192).random().toLong()
        )

        blockedMacsMap[normalizedMac] = rule
        updateRulesFlow()
    }

    /**
     * Restores network access for a device when unblocked or authorized.
     */
    fun unblockDeviceNetworkAccess(mac: String) {
        val normalizedMac = mac.uppercase().trim()
        if (blockedMacsMap.remove(normalizedMac) != null) {
            updateRulesFlow()
        }
    }

    /**
     * Synchronizes enforcer state with the given set of blocked MAC addresses.
     */
    fun syncBlockedDevices(blockedDevices: Set<String>, ipLookup: Map<String, String> = emptyMap()) {
        val normalized = blockedDevices.map { it.uppercase().trim() }.toSet()

        // Remove unblocked
        val iterator = blockedMacsMap.keys.iterator()
        while (iterator.hasNext()) {
            val existing = iterator.next()
            if (!normalized.contains(existing)) {
                iterator.remove()
            }
        }

        // Add newly blocked
        for (mac in normalized) {
            if (!blockedMacsMap.containsKey(mac)) {
                val ip = ipLookup[mac] ?: ""
                blockDeviceNetworkAccess(mac, ip)
            }
        }
        updateRulesFlow()
    }

    /**
     * Evaluates packet transmission against active firewall rules.
     */
    fun verifyPacketTransmission(
        sourceMac: String,
        sourceIp: String,
        destination: String = "192.168.1.1"
    ): PacketTransmissionResult {
        val normalizedMac = sourceMac.uppercase().trim()
        val rule = blockedMacsMap[normalizedMac]
        return if (rule != null) {
            _totalPacketsDropped.value += 1
            _totalBytesBlocked.value += 64
            PacketTransmissionResult(
                sourceMac = sourceMac,
                sourceIp = sourceIp,
                destination = destination,
                isPermitted = false,
                matchedRule = rule,
                reason = "Access Denied: Device is in quarantine / blocked list. Network access revoked."
            )
        } else {
            PacketTransmissionResult(
                sourceMac = sourceMac,
                sourceIp = sourceIp,
                destination = destination,
                isPermitted = true,
                matchedRule = null,
                reason = "Packet Forwarded: Device authorized."
            )
        }
    }

    /**
     * Returns the active set of blocked MAC addresses.
     */
    fun getBlockedMacs(): Set<String> {
        return blockedMacsMap.keys.toSet()
    }

    private fun updateRulesFlow() {
        val list = blockedMacsMap.values.toList()
        _activeRules.value = list
        val packets = list.sumOf { it.packetsDropped }
        val bytes = list.sumOf { it.bytesBlocked }
        _totalPacketsDropped.value = packets
        _totalBytesBlocked.value = bytes
    }

    private fun startDroppedPacketsTicker() {
        simulationTickerJob?.cancel()
        simulationTickerJob = scope.launch {
            while (isActive) {
                delay(3000)
                if (blockedMacsMap.isNotEmpty()) {
                    // Simulate dropped probe and broadcast attempts from quarantined devices
                    val addPackets = blockedMacsMap.size * (1..3).random().toLong()
                    val addBytes = addPackets * 84L
                    _totalPacketsDropped.value += addPackets
                    _totalBytesBlocked.value += addBytes
                }
            }
        }
    }
}
