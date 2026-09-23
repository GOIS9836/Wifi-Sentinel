package com.example

import com.example.data.local.SecurityAlertEntity
import com.example.data.model.DiscoveredDevice
import com.example.data.model.DuplicationGuardStatus
import com.example.data.model.IncidentTally
import com.example.data.model.NearbyAccessPoint
import com.example.data.model.NetworkHealthGrade
import com.example.data.model.NetworkSummaryReport
import com.example.data.model.ReportIncidentItem
import com.example.data.model.WifiConnectionState
import com.example.service.NetworkReportGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSummaryReportTest {

    @Test
    fun testOptimalNetworkHealthReportGeneratesGradeA() {
        val optimalWifiState = WifiConnectionState(
            isConnected = true,
            ssid = "SentinelSecure-5G",
            bssid = "00:11:22:33:44:55",
            rssi = -42,
            signalPercent = 95,
            linkSpeedMbps = 866,
            frequencyMhz = 5200,
            channel = 36,
            band = "5 GHz",
            gatewayIp = "192.168.1.1"
        )

        val discoveredDevices = listOf(
            DiscoveredDevice(
                ip = "192.168.1.1",
                macAddress = "00:11:22:33:44:55",
                isGateway = true,
                isAuthorized = true,
                isFlaggedUnknown = false
            ),
            DiscoveredDevice(
                ip = "192.168.1.25",
                macAddress = "AA:BB:CC:DD:EE:01",
                isAuthorized = true,
                isFlaggedUnknown = false
            )
        )

        val report = NetworkReportGenerator.generateReport(
            wifiState = optimalWifiState,
            discoveredDevices = discoveredDevices,
            whitelistedCount = 2,
            nearbyAps = emptyList(),
            alerts = emptyList(),
            isGatewayLocked = true,
            isGatewayMatch = true,
            duplicationStatus = DuplicationGuardStatus(isEnforced = true),
            isZeroToleranceActive = true
        )

        assertEquals("SentinelSecure-5G", report.ssid)
        assertEquals(100, report.overallHealthScore)
        assertEquals(NetworkHealthGrade.EXCELLENT, report.healthGrade)
        assertEquals(0, report.unknownDevicesCount)
        assertEquals(0, report.incidents.totalCount)
        assertTrue(report.isGatewayVerified)
        assertTrue(report.keyFindings.any { it.contains("Zero security incidents") })
    }

    @Test
    fun testCompromisedNetworkCalculatesDeductionsAndGradeCritical() {
        val degradedWifiState = WifiConnectionState(
            isConnected = true,
            ssid = "Public-Guest",
            bssid = "AA:BB:CC:00:11:22",
            rssi = -85, // deduction for weak RSSI
            signalPercent = 25,
            linkSpeedMbps = 24,
            frequencyMhz = 2412,
            channel = 6,
            band = "2.4 GHz",
            gatewayIp = "192.168.1.1"
        )

        // 2 unknown devices
        val discoveredDevices = listOf(
            DiscoveredDevice(
                ip = "192.168.1.1",
                macAddress = "AA:BB:CC:00:11:22",
                isGateway = true,
                isAuthorized = true
            ),
            DiscoveredDevice(
                ip = "192.168.1.100",
                macAddress = "02:00:00:11:22:33",
                isAuthorized = false,
                isFlaggedUnknown = true
            ),
            DiscoveredDevice(
                ip = "192.168.1.101",
                macAddress = "02:00:00:44:55:66",
                isAuthorized = false,
                isFlaggedUnknown = true
            )
        )

        val heavyCongestionAPs = (1..6).map {
            NearbyAccessPoint(
                ssid = "Neighbor-$it",
                bssid = "00:00:00:00:00:0$it",
                rssi = -60,
                frequencyMhz = 2437,
                channel = 6,
                band = "2.4 GHz",
                security = "WPA2"
            )
        }

        val criticalAlerts = listOf(
            SecurityAlertEntity(
                id = 1,
                title = "ARP Poisoning Detected",
                description = "Rogue MAC attempting MITM",
                deviceIp = "192.168.1.100",
                deviceMac = "02:00:00:11:22:33",
                severity = "CRITICAL",
                timestamp = System.currentTimeMillis(),
                isAcknowledged = false
            )
        )

        val report = NetworkReportGenerator.generateReport(
            wifiState = degradedWifiState,
            discoveredDevices = discoveredDevices,
            whitelistedCount = 1,
            nearbyAps = heavyCongestionAPs,
            alerts = criticalAlerts,
            isGatewayLocked = true,
            isGatewayMatch = false, // gateway mismatch deduction
            duplicationStatus = DuplicationGuardStatus(duplicateGatewaysBlocked = 1),
            isZeroToleranceActive = true
        )

        assertTrue(report.overallHealthScore < 50)
        assertEquals(NetworkHealthGrade.CRITICAL, report.healthGrade)
        assertEquals(2, report.unknownDevicesCount)
        assertEquals(1, report.incidents.criticalCount)
        assertEquals(1, report.incidents.unacknowledgedCount)
        assertFalse(report.isGatewayVerified)
        assertTrue(report.actionableRecommendations.isNotEmpty())
    }

    @Test
    fun testReportTextFormattingContainsRequiredHeadersAndData() {
        val report = NetworkSummaryReport(
            ssid = "SentinelTestNet",
            overallHealthScore = 88,
            healthGrade = NetworkHealthGrade.GOOD,
            rssiDbm = -55,
            linkSpeedMbps = 433,
            band = "5 GHz",
            channel = 44,
            channelCongestionLevel = "Low Congestion (1 APs)",
            gatewayIp = "192.168.1.1",
            isGatewayVerified = true,
            connectedDevicesCount = 5,
            whitelistedDevicesCount = 4,
            unknownDevicesCount = 1,
            incidents = IncidentTally(
                totalCount = 1,
                criticalCount = 0,
                highCount = 1,
                unacknowledgedCount = 1
            ),
            recentIncidents = listOf(
                ReportIncidentItem(
                    title = "Unknown Device Detected: 192.168.1.50",
                    description = "Device not in whitelist",
                    severity = "HIGH",
                    deviceIp = "192.168.1.50",
                    deviceMac = "AA:BB:CC:DD:EE:FF",
                    timestamp = System.currentTimeMillis()
                )
            ),
            keyFindings = listOf("1 unknown device present on subnet"),
            actionableRecommendations = listOf("Inspect unknown host 192.168.1.50")
        )

        val text = report.toFormattedReportText()

        assertTrue(text.contains("SENTINEL NETWORK HEALTH & SECURITY"))
        assertTrue(text.contains("Scan-Rundown Time:"))
        assertTrue(text.contains("Score TTL Duration:"))
        assertTrue(text.contains("SentinelTestNet"))
        assertTrue(text.contains("88/100"))
        assertTrue(text.contains("Good"))
        assertTrue(text.contains("NETWORK HEALTH METRICS"))
        assertTrue(text.contains("SECURITY POSTURE & INCIDENT AUDIT"))
        assertTrue(text.contains("KEY FINDINGS"))
        assertTrue(text.contains("ACTIONABLE RECOMMENDATIONS"))
        assertTrue(text.contains("192.168.1.50"))
    }
}
