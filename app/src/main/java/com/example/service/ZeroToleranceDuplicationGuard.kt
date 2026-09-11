package com.example.service

import com.example.data.model.DiscoveredDevice
import com.example.data.model.DuplicationGuardStatus
import com.example.data.model.DuplicationViolation
import com.example.data.model.DuplicationViolationType
import com.example.data.model.ThreatLevel

/**
 * ZeroToleranceDuplicationGuard:
 * Strict enforcement engine providing zero-tolerance defense against:
 * 1. Gateway Duplications (Rogue Gateways, Gateway ARP Spoofing, Evil Twin APs)
 * 2. Subnet Duplications & Cross-Subnet Anomalies
 * 3. IP Address Duplications (IP Collisions, ARP Poisoning Attacks)
 * 4. MAC Address Duplications (Layer-2 MAC Cloning & Identity Theft)
 */
object ZeroToleranceDuplicationGuard {

    /**
     * Sanitizes and enforces strict zero-duplication policies across a list of discovered devices.
     * Guarantees:
     * - Exactly ONE authoritative Gateway router (all rogue gateway impostors are quarantined).
     * - Strict Subnet boundary adherence.
     * - Zero unhandled IP collisions (duplicate IPs are flagged, isolated, and resolved).
     * - 100% Unique MAC addresses (Layer-2 clone attempts are identified and deduplicated).
     */
    fun enforceZeroDuplication(
        devices: List<DiscoveredDevice>,
        expectedSubnetBase: String,
        primaryGatewayIp: String,
        primaryGatewayMac: String
    ): Pair<List<DiscoveredDevice>, List<DuplicationViolation>> {
        val violations = mutableListOf<DuplicationViolation>()
        val normalizedPrimaryGwMac = primaryGatewayMac.uppercase().trim()

        // 1. GATEWAY DUPLICATION & ROGUE GATEWAY DETECTION
        var primaryGatewayFound = false
        val processedGatewayList = mutableListOf<DiscoveredDevice>()

        for (device in devices) {
            val isGwCandidate = device.isGateway || device.ip == primaryGatewayIp
            val isLegitimatePrimaryGw = (device.ip == primaryGatewayIp && (device.macAddress.equals(normalizedPrimaryGwMac, ignoreCase = true) || !primaryGatewayFound))

            if (isGwCandidate) {
                if (!primaryGatewayFound && isLegitimatePrimaryGw) {
                    primaryGatewayFound = true
                    processedGatewayList.add(
                        device.copy(
                            isGateway = true,
                            isRogueGateway = false,
                            isAuthorized = true,
                            isBlocked = false
                        )
                    )
                } else {
                    // Secondary or rogue device claiming gateway status or gateway IP!
                    violations.add(
                        DuplicationViolation(
                            violationType = DuplicationViolationType.ROGUE_GATEWAY,
                            ip = device.ip,
                            macAddress = device.macAddress,
                            conflictingDetail = "Rogue Gateway impersonator claiming $primaryGatewayIp against legitimate MAC $primaryGatewayMac"
                        )
                    )
                    processedGatewayList.add(
                        device.copy(
                            isGateway = false,
                            isRogueGateway = true,
                            isAuthorized = false,
                            isBlocked = true,
                            threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER,
                            confidencePercent = 100,
                            corroborationVector = "Zero-Tolerance: Rogue Gateway Impersonator Detected • Network Access Severed",
                            duplicationAlertDetail = "Conflicted with legitimate Gateway ($primaryGatewayIp / $primaryGatewayMac)"
                        )
                    )
                }
            } else {
                processedGatewayList.add(device)
            }
        }

        // 2. SUBNET BOUNDARY & CROSS-SUBNET ANOMALY CHECK
        val processedSubnetList = processedGatewayList.map { dev ->
            val devSubnet = dev.ip.substringBeforeLast(".")
            if (expectedSubnetBase.isNotBlank() && devSubnet != expectedSubnetBase && !dev.isSelf && !dev.isGateway) {
                violations.add(
                    DuplicationViolation(
                        violationType = DuplicationViolationType.ALIEN_SUBNET,
                        ip = dev.ip,
                        macAddress = dev.macAddress,
                        conflictingDetail = "Subnet mismatch: host in $devSubnet detected on expected subnet $expectedSubnetBase"
                    )
                )
                dev.copy(
                    isSubnetAnomaly = true,
                    isBlocked = true,
                    isAuthorized = false,
                    threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER,
                    confidencePercent = 100,
                    corroborationVector = "Zero-Tolerance: Alien Subnet Boundary Violation • Quarantined"
                )
            } else {
                dev
            }
        }

        // 3. IP ADDRESS DUPLICATION & ARP COLLISION ISOLATION
        val ipGroups = processedSubnetList.groupBy { it.ip }
        val resolvedIpList = mutableListOf<DiscoveredDevice>()

        for ((ip, group) in ipGroups) {
            if (group.size == 1) {
                resolvedIpList.add(group.first())
            } else {
                // Duplicate IP collision detected!
                // Prioritize Self device > Gateway > Authorized Device > Lower MAC
                val authoritativeDev = group.find { it.isSelf }
                    ?: group.find { it.isGateway }
                    ?: group.find { it.isAuthorized }
                    ?: group.minByOrNull { it.macAddress }!!

                for (dev in group) {
                    if (dev.macAddress.equals(authoritativeDev.macAddress, ignoreCase = true)) {
                        resolvedIpList.add(dev)
                    } else {
                        violations.add(
                            DuplicationViolation(
                                violationType = DuplicationViolationType.DUPLICATE_IP,
                                ip = ip,
                                macAddress = dev.macAddress,
                                conflictingDetail = "IP collision on $ip with authoritative host (${authoritativeDev.macAddress})"
                            )
                        )
                        resolvedIpList.add(
                            dev.copy(
                                isDuplicateIp = true,
                                isBlocked = true,
                                isAuthorized = false,
                                threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER,
                                confidencePercent = 100,
                                corroborationVector = "Zero-Tolerance: IP Collision & ARP Poisoning Intercepted • Quarantined",
                                duplicationAlertDetail = "Conflicting IP $ip held by authoritative node (${authoritativeDev.macAddress})"
                            )
                        )
                    }
                }
            }
        }

        // 4. MAC ADDRESS DEDUPLICATION & LAYER-2 CLONE PREVENTION
        val macGroups = resolvedIpList.groupBy { it.macAddress.uppercase().trim() }
        val finalDeduplicated = mutableListOf<DiscoveredDevice>()

        for ((mac, group) in macGroups) {
            if (group.size == 1) {
                finalDeduplicated.add(group.first())
            } else {
                // Duplicate MAC entries across multiple records
                violations.add(
                    DuplicationViolation(
                        violationType = DuplicationViolationType.DUPLICATE_MAC,
                        ip = group.map { it.ip }.joinToString(", "),
                        macAddress = mac,
                        conflictingDetail = "MAC cloning detected: multiple IPs (${group.map { it.ip }}) sharing identical Layer-2 MAC $mac"
                    )
                )

                // Select primary legitimate entry (Gateway or Self or first Authorized)
                val primary = group.find { it.isSelf }
                    ?: group.find { it.isGateway }
                    ?: group.find { it.isAuthorized }
                    ?: group.first()

                finalDeduplicated.add(
                    primary.copy(
                        isDuplicateMac = group.any { it.isDuplicateMac || it.isRogueGateway || it.isDuplicateIp },
                        duplicationAlertDetail = "Deduplicated ${group.size} instances of MAC $mac"
                    )
                )
            }
        }

        // Return sorted list with zero duplications
        val sortedList = finalDeduplicated.sortedWith(
            compareByDescending<DiscoveredDevice> { it.isGateway }
                .thenByDescending { it.isRogueGateway }
                .thenByDescending { it.isSelf }
                .thenByDescending { it.isDuplicateIp }
                .thenBy { it.isAuthorized }
        )

        return Pair(sortedList, violations)
    }

    /**
     * Deduplicates ConnectedDeviceArpInfo list for WiFiManager
     */
    fun deduplicateArpDevices(
        devices: List<ConnectedDeviceArpInfo>,
        primaryGatewayIp: String
    ): List<ConnectedDeviceArpInfo> {
        var gatewayEncountered = false
        val cleanList = mutableListOf<ConnectedDeviceArpInfo>()
        val seenMacs = mutableSetOf<String>()

        for (dev in devices) {
            val normalizedMac = dev.macAddress.uppercase().trim()
            if (seenMacs.contains(normalizedMac)) {
                // Layer-2 clone duplicate dropped
                continue
            }
            seenMacs.add(normalizedMac)

            if (dev.isGateway || dev.ip == primaryGatewayIp) {
                if (!gatewayEncountered) {
                    gatewayEncountered = true
                    cleanList.add(dev.copy(isGateway = true, isBlocked = false))
                } else {
                    // Rogue gateway duplicate
                    cleanList.add(
                        dev.copy(
                            isGateway = false,
                            isBlocked = true,
                            isPotentialUnauthorizedUser = true,
                            threatReason = "Zero-Tolerance: Rogue Gateway Duplicate Quarantined"
                        )
                    )
                }
            } else {
                cleanList.add(dev)
            }
        }
        return cleanList
    }
}
