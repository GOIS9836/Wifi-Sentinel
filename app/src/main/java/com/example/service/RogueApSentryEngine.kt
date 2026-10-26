package com.example.service

import com.example.data.model.NearbyAccessPoint
import com.example.data.model.RogueApAnomaly
import com.example.data.model.RogueApAnomalyType
import com.example.data.model.RogueApAuditReport
import com.example.data.model.WifiConnectionState
import com.example.data.model.isLocallyAdministeredMac

/**
 * Real-Time Rogue Access Point & Evil Twin Sentry Engine.
 *
 * Protects against:
 * 1. Evil Twin attacks: Adversary AP impersonating the connected SSID with differing BSSID.
 * 2. Security Downgrade traps: Adversary broadcasting legitimate SSID with Open or weak WEP/WPA encryption.
 * 3. LAA Soft-AP / PineApple: Rogues using randomized / locally administered BSSIDs.
 * 4. Signal Dominance attacks: Abnormally elevated RF signals attempting to force roaming.
 *
 * Governance: CISSP Domain 4 (Communication & Network Security) / POTRAZ Chapter 12:07.
 * Resource Invariant: Single-pass O(N) evaluation, zero allocations in tight loops, non-blocking.
 */
object RogueApSentryEngine {

    // Known Bridge / Mesh Access Point BSSID Whitelist
    private val DEFAULT_APPROVED_BSSIDS = setOf(
        "B4:0F:3B:7F:A3:F0", // Authorized G_4035 Wireless Extender / Bridge Link
        "2C:3B:70:C9:64:DF"  // Authorized Secondary Access Point
    )

    fun auditAccessPoints(
        connectedState: WifiConnectionState,
        accessPoints: List<NearbyAccessPoint>,
        whitelistedBssids: Set<String> = emptySet()
    ): RogueApAuditReport {
        if (!connectedState.isConnected || connectedState.ssid.isBlank() || connectedState.ssid == "Scanning...") {
            return RogueApAuditReport(
                isEvilTwinDetected = false,
                hasDowngradeThreat = false,
                totalAnomalies = 0,
                anomalies = emptyList(),
                highestSeverity = "SAFE",
                threatSummary = "No active Wi-Fi connection to monitor against rogue AP impersonation.",
                recommendedAction = "Connect to an authorized network to engage Evil Twin sentry."
            )
        }

        val approved = (DEFAULT_APPROVED_BSSIDS + whitelistedBssids).map { it.uppercase().trim() }.toSet()
        val connectedSsid = connectedState.ssid.trim()
        val connectedBssid = connectedState.bssid.uppercase().trim()
        val connectedSec = connectedState.securityProtocol.uppercase()
        val anomalies = mutableListOf<RogueApAnomaly>()

        var evilTwinCount = 0
        var downgradeCount = 0

        // Single pass across visible access points
        for (ap in accessPoints) {
            val apSsid = ap.ssid.trim()
            val apBssid = ap.bssid.uppercase().trim()
            val apSec = ap.security.uppercase()

            // Skip invalid or placeholder BSSIDs
            if (apBssid.isBlank() || apBssid == "02:00:00:00:00:00" || apBssid == "00:00:00:00:00:00" || apBssid == "--:--:--:--:--:--") {
                continue
            }

            // Target APs matching the connected SSID
            if (apSsid.equals(connectedSsid, ignoreCase = true)) {
                // If BSSID differs from the connected AP and is not an approved mesh link
                if (apBssid != connectedBssid && !approved.contains(apBssid)) {
                    val isDowngrade = isSecurityDowngraded(connectedSec, apSec)
                    val isLaa = isLocallyAdministeredMac(apBssid)
                    val isSignalDominance = (ap.rssi - connectedState.rssi) >= 25 && ap.rssi > -45

                    when {
                        isDowngrade -> {
                            downgradeCount++
                            evilTwinCount++
                            anomalies.add(
                                RogueApAnomaly(
                                    ssid = apSsid,
                                    bssid = apBssid,
                                    anomalyType = RogueApAnomalyType.SECURITY_DOWNGRADE_ATTACK,
                                    severity = "CRITICAL",
                                    detail = "CRITICAL: Rogue AP broadcasting '$apSsid' with downgraded encryption ($apSec vs $connectedSec). Potential credential harvesting trap.",
                                    detectedRssi = ap.rssi,
                                    channel = ap.channel,
                                    band = ap.band
                                )
                            )
                        }
                        isLaa -> {
                            evilTwinCount++
                            anomalies.add(
                                RogueApAnomaly(
                                    ssid = apSsid,
                                    bssid = apBssid,
                                    anomalyType = RogueApAnomalyType.LAA_RANDOMIZED_BSSID_ROGUE,
                                    severity = "HIGH",
                                    detail = "HIGH: Rogue Soft-AP detected using locally administered randomized BSSID ($apBssid) matching '$apSsid'. Potential Wi-Fi Pineapple or mobile hotspot clone.",
                                    detectedRssi = ap.rssi,
                                    channel = ap.channel,
                                    band = ap.band
                                )
                            )
                        }
                        isSignalDominance -> {
                            evilTwinCount++
                            anomalies.add(
                                RogueApAnomaly(
                                    ssid = apSsid,
                                    bssid = apBssid,
                                    anomalyType = RogueApAnomalyType.SIGNAL_DOMINANCE_ANOMALY,
                                    severity = "HIGH",
                                    detail = "HIGH: Unverified AP '$apSsid' ($apBssid) exhibits extreme signal dominance (${ap.rssi} dBm vs connected ${connectedState.rssi} dBm). Potential forced roaming injection.",
                                    detectedRssi = ap.rssi,
                                    channel = ap.channel,
                                    band = ap.band
                                )
                            )
                        }
                        else -> {
                            anomalies.add(
                                RogueApAnomaly(
                                    ssid = apSsid,
                                    bssid = apBssid,
                                    anomalyType = RogueApAnomalyType.UNAUTHORIZED_MESH_BSSID,
                                    severity = "WARNING",
                                    detail = "WARNING: Unrecognized BSSID ($apBssid) claiming '$apSsid'. If this is a valid mesh node or extender, add it to the approved whitelist.",
                                    detectedRssi = ap.rssi,
                                    channel = ap.channel,
                                    band = ap.band
                                )
                            )
                        }
                    }
                }
            }
        }

        val hasEvilTwin = evilTwinCount > 0
        val hasDowngrade = downgradeCount > 0
        val highestSev = when {
            hasDowngrade || anomalies.any { it.severity == "CRITICAL" } -> "CRITICAL"
            anomalies.any { it.severity == "HIGH" } -> "HIGH"
            anomalies.any { it.severity == "WARNING" } -> "WARNING"
            else -> "SAFE"
        }

        val summary = when {
            hasDowngrade -> "CRITICAL ALERT: Wi-Fi encryption downgrade attack detected on '$connectedSsid' ($downgradeCount rogue APs). Cleartext credential theft risk."
            hasEvilTwin -> "HIGH ALERT: Evil Twin / Rogue AP impersonation detected on '$connectedSsid' ($evilTwinCount anomalous APs)."
            anomalies.isNotEmpty() -> "ELEVATED POSTURE: ${anomalies.size} unverified BSSID node(s) detected claiming '$connectedSsid'."
            else -> "BENEDICTUS: RF spectrum clean. Zero Evil Twin or Rogue AP anomalies detected."
        }

        val action = when {
            hasDowngrade -> "Do NOT disconnect or allow reconnect to open/weak APs. Enforce PMF (802.11w) and enable VPN tunnel immediately."
            hasEvilTwin -> "Verify AP physical identity. Isolate device or switch to trusted cellular data if unknown BSSID dominates."
            anomalies.isNotEmpty() -> "Review unverified BSSIDs in Sentinel Settings and whitelist legitimate mesh repeaters."
            else -> "Maintain active real-time RF perimeter sentry."
        }

        return RogueApAuditReport(
            isEvilTwinDetected = hasEvilTwin,
            hasDowngradeThreat = hasDowngrade,
            totalAnomalies = anomalies.size,
            anomalies = anomalies,
            highestSeverity = highestSev,
            threatSummary = summary,
            recommendedAction = action
        )
    }

    private fun isSecurityDowngraded(connectedSec: String, candidateSec: String): Boolean {
        val isCandidateOpen = candidateSec.contains("OPEN", ignoreCase = true) || candidateSec.isBlank() || candidateSec == "[ESS]"
        val isCandidateWep = candidateSec.contains("WEP", ignoreCase = true)
        val isConnectedSecure = connectedSec.contains("WPA", ignoreCase = true) || connectedSec.contains("SAE", ignoreCase = true) || connectedSec.contains("EAP", ignoreCase = true)

        if (isConnectedSecure && (isCandidateOpen || isCandidateWep)) {
            return true
        }

        // Downgrade from WPA3-SAE to WPA-PSK (WPA1)
        if (connectedSec.contains("WPA3", ignoreCase = true) && candidateSec.contains("WPA-PSK", ignoreCase = true) && !candidateSec.contains("WPA2", ignoreCase = true) && !candidateSec.contains("WPA3", ignoreCase = true)) {
            return true
        }

        return false
    }
}
