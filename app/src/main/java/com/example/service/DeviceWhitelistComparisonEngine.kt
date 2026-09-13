package com.example.service

import com.example.data.local.NetworkDeviceEntity
import com.example.data.model.DeviceWhitelistAuditResult
import com.example.data.model.DiscoveredDevice
import com.example.data.model.ThreatLevel
import com.example.data.model.WhitelistAuditStatus

object DeviceWhitelistComparisonEngine {

    /**
     * Executes the logic flow comparing the active connected subnet devices against the known-device whitelist.
     *
     * @param connectedDevices Active devices discovered on the Wi-Fi subnet.
     * @param whitelist Registered known/whitelisted devices from database or persistent store.
     * @param currentIp IP address of the scanning device (self).
     * @param gatewayIp IP address of the primary default gateway router.
     * @param gatewayMac MAC address of the locked/primary default gateway router.
     * @param previouslyAlertedMacs MAC addresses of unknown devices that have already been notified.
     * @param suppressedMacs Set of MAC addresses suppressed by user or false-positive policy.
     * @return Audit result containing partitioned lists and newly detected unknown devices requiring alerts.
     */
    fun compareConnectedAgainstWhitelist(
        connectedDevices: List<DiscoveredDevice>,
        whitelist: List<NetworkDeviceEntity>,
        currentIp: String,
        gatewayIp: String = "",
        gatewayMac: String = "",
        previouslyAlertedMacs: Set<String> = emptySet(),
        suppressedMacs: Set<String> = emptySet()
    ): DeviceWhitelistAuditResult {
        // Step 1: Index all explicitly whitelisted MAC addresses (normalized to uppercase)
        val whitelistedMacSet = whitelist
            .filter { it.isAuthorized }
            .map { it.macAddress.trim().uppercase() }
            .toMutableSet()

        val whitelistedDevices = mutableListOf<DiscoveredDevice>()
        val unknownDevices = mutableListOf<DiscoveredDevice>()
        val newlyDetectedUnknowns = mutableListOf<DiscoveredDevice>()

        for (device in connectedDevices) {
            val normalizedMac = device.macAddress.trim().uppercase()
            val isSelfDevice = device.isSelf || (currentIp.isNotBlank() && device.ip == currentIp)
            val isGatewayDevice = device.isGateway ||
                    (gatewayIp.isNotBlank() && device.ip == gatewayIp) ||
                    (gatewayMac.isNotBlank() && normalizedMac.equals(gatewayMac.trim().uppercase(), ignoreCase = true))

            // Check if device matches known-device whitelist or is inherently trusted infrastructure
            val isWhitelisted = whitelistedMacSet.contains(normalizedMac) || isSelfDevice || isGatewayDevice

            if (isWhitelisted) {
                whitelistedDevices.add(
                    device.copy(
                        isAuthorized = true,
                        isFlaggedUnknown = false,
                        threatLevel = if (device.threatLevel == ThreatLevel.UNAUTHORIZED_INTRUDER) ThreatLevel.SAFE else device.threatLevel
                    )
                )
            } else {
                // Device is not in whitelist: evaluate unknown status
                val isSuppressed = device.isFalsePositiveSuppressed || suppressedMacs.contains(normalizedMac)
                val enrichedUnknown = device.copy(
                    isAuthorized = false,
                    isFlaggedUnknown = true,
                    threatLevel = if (isSuppressed) ThreatLevel.FALSE_POSITIVE_SUPPRESSED else ThreatLevel.UNAUTHORIZED_INTRUDER,
                    corroborationVector = if (device.corroborationVector.isBlank()) "Whitelist Comparison Check: Host Unlisted" else device.corroborationVector
                )

                unknownDevices.add(enrichedUnknown)

                // Check if this is a newly discovered unknown device requiring immediate notification
                if (!previouslyAlertedMacs.contains(normalizedMac) && !isSuppressed) {
                    newlyDetectedUnknowns.add(enrichedUnknown)
                }
            }
        }

        val auditStatus = when {
            newlyDetectedUnknowns.isNotEmpty() -> WhitelistAuditStatus.ALERT_TRIGGERED
            unknownDevices.isNotEmpty() -> WhitelistAuditStatus.UNKNOWN_DETECTED
            else -> WhitelistAuditStatus.ALL_WHITELISTED
        }

        return DeviceWhitelistAuditResult(
            totalConnected = connectedDevices.size,
            whitelistedCount = whitelistedDevices.size,
            unknownCount = unknownDevices.size,
            whitelistedDevices = whitelistedDevices,
            unknownDevices = unknownDevices,
            newlyDetectedUnknowns = newlyDetectedUnknowns,
            status = auditStatus,
            auditTimestamp = System.currentTimeMillis(),
            notificationsDispatched = newlyDetectedUnknowns.size,
            isAutoNotifyEnabled = true
        )
    }
}
