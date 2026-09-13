package com.example.service

import com.example.data.local.SecurityAlertEntity
import com.example.data.model.DiscoveredDevice
import com.example.data.model.DuplicationGuardStatus
import com.example.data.model.IncidentTally
import com.example.data.model.NearbyAccessPoint
import com.example.data.model.NetworkHealthGrade
import com.example.data.model.NetworkSummaryReport
import com.example.data.model.ReportIncidentItem
import com.example.data.model.WifiConnectionState

object NetworkReportGenerator {

    fun generateReport(
        wifiState: WifiConnectionState,
        discoveredDevices: List<DiscoveredDevice>,
        whitelistedCount: Int,
        nearbyAps: List<NearbyAccessPoint>,
        alerts: List<SecurityAlertEntity>,
        isGatewayLocked: Boolean,
        isGatewayMatch: Boolean,
        duplicationStatus: DuplicationGuardStatus,
        isZeroToleranceActive: Boolean
    ): NetworkSummaryReport {
        // 1. Calculate RF Congestion on current channel
        val sameChannelAps = nearbyAps.count { it.channel == wifiState.channel }
        val congestionLevel = when {
            sameChannelAps >= 5 -> "Severe ($sameChannelAps APs competing)"
            sameChannelAps >= 3 -> "Moderate ($sameChannelAps APs)"
            sameChannelAps == 1 || sameChannelAps == 2 -> "Light ($sameChannelAps APs)"
            else -> "Clean / Isolated (0 competing APs)"
        }

        // 2. Compute Health Deductions
        var deductions = 0

        // Signal deductions
        val rssi = wifiState.rssi
        when {
            rssi < -80 -> deductions += 20
            rssi < -70 -> deductions += 12
            rssi < -62 -> deductions += 5
        }

        // RF Congestion deductions
        if (sameChannelAps >= 5) deductions += 12
        else if (sameChannelAps >= 3) deductions += 6

        // Gateway verification deductions
        if (!isGatewayMatch) {
            deductions += 25
        } else if (!isGatewayLocked) {
            deductions += 5
        }

        // Unknown / unauthorized hosts
        val unknownCount = discoveredDevices.count { it.isFlaggedUnknown || (!it.isAuthorized && !it.isSelf && !it.isGateway && !it.isFalsePositiveSuppressed) }
        val blockedCount = discoveredDevices.count { it.isBlocked }
        val falsePositivesCount = discoveredDevices.count { it.isFalsePositiveSuppressed }
        deductions += (unknownCount * 12).coerceAtMost(30)

        // Duplication anomalies
        if (duplicationStatus.totalViolations > 0) {
            deductions += (duplicationStatus.totalViolations * 15).coerceAtMost(30)
        }

        // Recent Critical Incidents
        val recentCriticalCount = alerts.count { it.severity.equals("CRITICAL", ignoreCase = true) }
        deductions += (recentCriticalCount * 6).coerceAtMost(24)

        val healthScore = (100 - deductions).coerceIn(15, 100)

        val healthGrade = when {
            healthScore >= 88 -> NetworkHealthGrade.EXCELLENT
            healthScore >= 72 -> NetworkHealthGrade.GOOD
            healthScore >= 52 -> NetworkHealthGrade.FAIR
            else -> NetworkHealthGrade.CRITICAL
        }

        // 3. Incident Tallies
        val criticalCount = alerts.count { it.severity.equals("CRITICAL", ignoreCase = true) }
        val highCount = alerts.count { it.severity.equals("HIGH", ignoreCase = true) }
        val mediumCount = alerts.count { it.severity.equals("MEDIUM", ignoreCase = true) || it.severity.equals("WARNING", ignoreCase = true) }
        val unacknowledgedCount = alerts.count { !it.isAcknowledged }

        val incidentTally = IncidentTally(
            totalCount = alerts.size,
            criticalCount = criticalCount,
            highCount = highCount,
            mediumCount = mediumCount,
            unacknowledgedCount = unacknowledgedCount
        )

        val recentIncidentItems = alerts.take(12).map { alert ->
            ReportIncidentItem(
                id = alert.id,
                title = alert.title,
                description = alert.description,
                severity = alert.severity,
                timestamp = alert.timestamp,
                deviceIp = alert.deviceIp,
                deviceMac = alert.deviceMac
            )
        }

        // 4. Generate Key Findings
        val findings = mutableListOf<String>()

        if (wifiState.isConnected) {
            findings.add("Connected to '${wifiState.ssid}' on ${wifiState.band} (Channel ${wifiState.channel}) with link speed of ${wifiState.linkSpeedMbps} Mbps.")
        } else {
            findings.add("Device is disconnected from Wi-Fi; evaluating cached or fallback network state.")
        }

        if (rssi >= -60) {
            findings.add("Strong RF link quality ($rssi dBm, ${wifiState.signalPercent}%); low packet retransmission probability.")
        } else if (rssi >= -72) {
            findings.add("Moderate RF link quality ($rssi dBm); acceptable for standard streaming and web operations.")
        } else {
            findings.add("Significant RF attenuation detected ($rssi dBm); elevated packet latency and retransmission risk.")
        }

        if (sameChannelAps > 3) {
            findings.add("Channel ${wifiState.channel} exhibits co-channel contention with $sameChannelAps neighboring access points.")
        } else {
            findings.add("RF channel spectrum is relatively clear ($sameChannelAps overlapping external BSSIDs).")
        }

        if (isGatewayLocked && isGatewayMatch) {
            findings.add("Default gateway (${wifiState.gatewayIp}) is verified and locked against ARP spoofing.")
        } else if (!isGatewayMatch) {
            findings.add("GATEWAY INTEGRITY ALERT: Active gateway IP/MAC does not match locked baseline!")
        } else {
            findings.add("Gateway is currently unlocked; BSSID baseline enforcement is inactive.")
        }

        if (unknownCount == 0) {
            findings.add("Subnet whitelist compliance is 100% ($whitelistedCount trusted devices confirmed).")
        } else {
            findings.add("Subnet audit flagged $unknownCount unlisted host(s) violating the trusted device whitelist.")
        }

        if (incidentTally.totalCount == 0) {
            findings.add("Zero security incidents or ARP poisonings detected in current monitoring epoch.")
        } else {
            findings.add("${incidentTally.totalCount} security incident(s) recorded (${incidentTally.criticalCount} critical).")
        }

        if (duplicationStatus.totalViolations > 0) {
            findings.add("Duplication Guard active: ${duplicationStatus.totalViolations} duplication anomalies intercepted.")
        }

        // 5. Generate Actionable Recommendations
        val recommendations = mutableListOf<String>()

        if (unknownCount > 0) {
            recommendations.add("Review the $unknownCount unlisted host(s) in the Whitelist Sentry and assign trusted alias or quarantine.")
        }

        if (!isGatewayLocked) {
            recommendations.add("Lock current gateway BSSID in Gateway Guard to prevent rogue ARP poisoning.")
        }

        if (sameChannelAps >= 3) {
            recommendations.add("Consider migrating Wi-Fi router to an uncontested 5 GHz / 6 GHz channel.")
        }

        if (unacknowledgedCount > 0) {
            recommendations.add("Acknowledge or clear $unacknowledgedCount pending security alerts in the event ledger.")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Current network posture is optimal. Maintain background shield monitoring.")
            recommendations.add("Perform periodic weekly sweeps to verify newly attached smart devices.")
        }

        return NetworkSummaryReport(
            generatedAt = System.currentTimeMillis(),
            overallHealthScore = healthScore,
            healthGrade = healthGrade,
            ssid = wifiState.ssid,
            bssid = wifiState.bssid,
            ipAddress = wifiState.ipAddress,
            gatewayIp = wifiState.gatewayIp,
            isGatewayVerified = isGatewayMatch,
            rssiDbm = rssi,
            signalPercent = wifiState.signalPercent,
            linkSpeedMbps = wifiState.linkSpeedMbps,
            channel = wifiState.channel,
            band = wifiState.band,
            channelCongestionLevel = congestionLevel,
            connectedDevicesCount = discoveredDevices.size,
            whitelistedDevicesCount = whitelistedCount,
            unknownDevicesCount = unknownCount,
            blockedDevicesCount = blockedCount,
            falsePositivesCount = falsePositivesCount,
            incidents = incidentTally,
            recentIncidents = recentIncidentItems,
            keyFindings = findings,
            actionableRecommendations = recommendations
        )
    }
}
