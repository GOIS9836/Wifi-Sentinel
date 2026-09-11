package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.NetworkDeviceEntity
import com.example.data.local.SecurityAlertEntity
import com.example.data.local.SignalLogEntity
import com.example.data.model.AiOptimizationReport
import com.example.data.model.BtDeviceType
import com.example.data.model.BtPerimeterDevice
import com.example.data.model.DiscoveredDevice
import com.example.data.model.GatewayTransitionEvent
import com.example.data.model.NearbyAccessPoint
import com.example.data.model.ThreatLevel
import com.example.data.model.WifiConnectionState
import com.example.data.remote.GeminiService
import com.example.service.BluetoothSentryScanner
import com.example.service.NetworkAccessEnforcer
import com.example.service.WifiScannerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class WifiRepository(
    private val scannerService: WifiScannerService,
    private val btSentryScanner: BluetoothSentryScanner,
    private val database: AppDatabase,
    private val geminiService: GeminiService,
    private val networkAccessEnforcer: NetworkAccessEnforcer? = null
) {
    val knownDevicesFlow: Flow<List<NetworkDeviceEntity>> = database.networkDeviceDao().getAllDevices()
    val signalLogsFlow: Flow<List<SignalLogEntity>> = database.signalLogDao().getAllSignalLogs()
    val securityAlertsFlow: Flow<List<SecurityAlertEntity>> = database.securityAlertDao().getAllAlerts()
    val unacknowledgedAlertsCount: Flow<Int> = database.securityAlertDao().getUnacknowledgedCount()

    val perimeterBtDevices: StateFlow<List<BtPerimeterDevice>> = btSentryScanner.perimeterDevices
    val isBtScanning: StateFlow<Boolean> = btSentryScanner.isScanning

    fun getLiveWifiState(): WifiConnectionState {
        return scannerService.getCurrentWifiState()
    }

    suspend fun getNearbyAccessPoints(): List<NearbyAccessPoint> {
        return scannerService.scanNearbyAccessPoints()
    }

    suspend fun discoverNetworkDevices(currentIp: String): List<DiscoveredDevice> {
        val known = knownDevicesFlow.first()
        val authorizedMacs = known.filter { it.isAuthorized }.map { it.macAddress.uppercase() }.toSet()
        val blockedMacs = known.filter { it.isBlocked }.map { it.macAddress.uppercase() }.toSet()

        // Synchronize firewall ACL enforcer with active blocked/quarantined devices
        networkAccessEnforcer?.syncBlockedDevices(
            blockedMacs,
            known.associate { it.macAddress.uppercase() to it.ipAddress }
        )

        val scanned = scannerService.discoverSubnetDevices(currentIp, authorizedMacs, blockedMacs)
        val subnetBase = currentIp.substringBeforeLast(".")
        val expectedGatewayIp = "$subnetBase.1"
        val expectedGatewayMac = scanned.find { it.isGateway }?.macAddress ?: "00:1A:2B:3C:4D:01"

        // Enforce Zero-Tolerance Duplication Guard (Gateways, Subnets, IPs, MACs)
        val (dedupedScanned, duplicationViolations) = com.example.service.ZeroToleranceDuplicationGuard.enforceZeroDuplication(
            devices = scanned,
            expectedSubnetBase = subnetBase,
            primaryGatewayIp = expectedGatewayIp,
            primaryGatewayMac = expectedGatewayMac
        )

        // Generate critical alerts for any duplication violations
        for (violation in duplicationViolations) {
            database.securityAlertDao().insert(
                SecurityAlertEntity(
                    title = when (violation.violationType) {
                        com.example.data.model.DuplicationViolationType.ROGUE_GATEWAY -> "Zero-Tolerance: Rogue Gateway Duplication Detected"
                        com.example.data.model.DuplicationViolationType.DUPLICATE_IP -> "Zero-Tolerance: IP Duplication / ARP Poisoning Alert"
                        com.example.data.model.DuplicationViolationType.DUPLICATE_MAC -> "Zero-Tolerance: MAC Clone Intrusion Detected"
                        com.example.data.model.DuplicationViolationType.ALIEN_SUBNET -> "Zero-Tolerance: Alien Subnet Boundary Violation"
                    },
                    description = violation.conflictingDetail,
                    deviceIp = violation.ip,
                    deviceMac = violation.macAddress,
                    severity = "CRITICAL",
                    confidencePercent = 100
                )
            )
        }

        // Store or update devices in Room
        val enriched = dedupedScanned.map { dev ->
            val existing = database.networkDeviceDao().getDeviceByMac(dev.macAddress)
            val isAuth = existing?.isAuthorized ?: dev.isAuthorized
            val isBlocked = existing?.isBlocked ?: dev.isBlocked || blockedMacs.contains(dev.macAddress.uppercase())
            val customName = existing?.customName?.takeIf { it.isNotBlank() } ?: dev.customName

            database.networkDeviceDao().insertOrUpdate(
                NetworkDeviceEntity(
                    macAddress = dev.macAddress,
                    ipAddress = dev.ip,
                    vendor = dev.vendor,
                    customName = customName,
                    isAuthorized = isAuth,
                    isBlocked = isBlocked,
                    firstSeen = existing?.firstSeen ?: System.currentTimeMillis(),
                    lastSeen = System.currentTimeMillis()
                )
            )

            // Check if device is unauthorized and generate security alert if not recognized
            if (!isAuth && !dev.isSelf && !dev.isGateway) {
                // Raise alert if it's new
                if (existing == null) {
                    val isRandom = dev.isRandomizedMac
                    database.securityAlertDao().insert(
                        SecurityAlertEntity(
                            title = if (isRandom) "Unverified Host (Private MAC)" else "Zero-Tolerance: Unauthorized Host",
                            description = if (isRandom) {
                                "Host at ${dev.ip} (${dev.macAddress}) [${dev.vendor}] is using Private Wi-Fi MAC randomization. Zero-FP engine recommends fingerprint authorization."
                            } else {
                                "Zero-Tolerance alert: Intruder host detected at ${dev.ip} (${dev.macAddress}) [${dev.vendor}]. Unverified on subnet."
                            },
                            deviceIp = dev.ip,
                            deviceMac = dev.macAddress,
                            severity = if (isRandom) "WARNING" else "CRITICAL",
                            confidencePercent = dev.confidencePercent
                        )
                    )
                }
            }

            dev.copy(
                isAuthorized = isAuth && !isBlocked,
                isBlocked = isBlocked,
                responseTimeMs = if (isBlocked) 0L else dev.responseTimeMs,
                corroborationVector = if (isBlocked) "Firewall Isolation Active • Zero Network Access" else dev.corroborationVector,
                customName = customName,
                threatLevel = when {
                    isBlocked -> ThreatLevel.UNAUTHORIZED_INTRUDER
                    isAuth -> ThreatLevel.SAFE
                    else -> ThreatLevel.UNAUTHORIZED_INTRUDER
                }
            )
        }

        return enriched
    }

    suspend fun setDeviceAuthorized(mac: String, authorized: Boolean) {
        database.networkDeviceDao().setAuthorized(mac, authorized)
        if (authorized) {
            networkAccessEnforcer?.unblockDeviceNetworkAccess(mac)
        }
    }

    suspend fun setDeviceBlocked(mac: String, blocked: Boolean) {
        database.networkDeviceDao().setBlocked(mac, blocked)
        if (blocked) {
            networkAccessEnforcer?.blockDeviceNetworkAccess(mac)
        } else {
            networkAccessEnforcer?.unblockDeviceNetworkAccess(mac)
        }
    }

    suspend fun markAlertFalsePositive(alertId: Long, deviceMac: String) {
        database.securityAlertDao().markFalsePositive(alertId)
        database.networkDeviceDao().setAuthorized(deviceMac, true)
    }

    suspend fun suppressDeviceAsFalsePositive(mac: String) {
        database.securityAlertDao().markFalsePositiveByMac(mac)
        database.networkDeviceDao().setAuthorized(mac, true)
    }

    fun toggleBtTrust(address: String) {
        btSentryScanner.toggleTrustDevice(address)
    }

    fun dismissBtFalsePositive(address: String) {
        btSentryScanner.dismissBtFalsePositive(address)
    }

    suspend fun setDeviceCustomName(mac: String, name: String) {
        database.networkDeviceDao().setCustomName(mac, name)
    }

    suspend fun deleteDevice(mac: String) {
        database.networkDeviceDao().deleteDevice(mac)
    }

    suspend fun logSignalSample(locationName: String, state: WifiConnectionState) {
        database.signalLogDao().insert(
            SignalLogEntity(
                locationName = locationName,
                rssiDbm = state.rssi,
                speedMbps = state.linkSpeedMbps,
                frequencyMhz = state.frequencyMhz,
                channel = state.channel
            )
        )
    }

    suspend fun clearSignalLogs() {
        database.signalLogDao().clearAll()
    }

    suspend fun deleteSignalLog(id: Long) {
        database.signalLogDao().delete(id)
    }

    suspend fun acknowledgeAlert(id: Long) {
        database.securityAlertDao().acknowledgeAlert(id)
    }

    suspend fun acknowledgeAllAlerts() {
        database.securityAlertDao().acknowledgeAll()
    }

    suspend fun clearAlerts() {
        database.securityAlertDao().clearAll()
    }

    suspend fun generateAiOptimization(
        state: WifiConnectionState,
        devices: List<DiscoveredDevice>,
        accessPoints: List<NearbyAccessPoint>
    ): AiOptimizationReport {
        return geminiService.analyzeWifiAndOptimize(state, devices, accessPoints)
    }

    suspend fun askAiAdvisor(
        prompt: String,
        state: WifiConnectionState,
        devices: List<DiscoveredDevice>
    ): String {
        return geminiService.askAdvisor(prompt, state, devices)
    }

    fun startBtPerimeterScan(onThreat: (BtPerimeterDevice) -> Unit) {
        btSentryScanner.startPerimeterScan(onThreat)
    }

    fun stopBtPerimeterScan() {
        btSentryScanner.stopPerimeterScan()
    }

    fun toggleBtQuarantine(address: String) {
        btSentryScanner.toggleQuarantine(address)
    }

    fun injectSimulatedBtThreat(name: String, type: BtDeviceType, rssi: Int = -52): BtPerimeterDevice {
        return btSentryScanner.injectSimulatedThreat(name, type, rssi)
    }

    suspend fun recordBtIntrusionAlert(device: BtPerimeterDevice) {
        database.securityAlertDao().insert(
            SecurityAlertEntity(
                title = "ZERO-TOLERANCE: Bluetooth Device Detected",
                description = "Rogue BT Peripheral [${device.name}] detected at ${device.rssi} dBm (${device.proximity}) [MAC: ${device.address}]",
                deviceIp = "BLE Perimeter",
                deviceMac = device.address,
                severity = "CRITICAL"
            )
        )
    }

    suspend fun recordGatewayTransitionAlert(event: GatewayTransitionEvent) {
        database.securityAlertDao().insert(
            SecurityAlertEntity(
                title = "GATEWAY SHIFT: ${event.switchType.label}",
                description = event.details,
                deviceIp = event.newGatewayIp,
                deviceMac = event.newGatewayMac,
                severity = event.severity
            )
        )
    }

    suspend fun recordDuplicationAlert(title: String, description: String, ip: String, mac: String) {
        database.securityAlertDao().insert(
            SecurityAlertEntity(
                title = title,
                description = description,
                deviceIp = ip,
                deviceMac = mac,
                severity = "CRITICAL",
                confidencePercent = 100
            )
        )
    }
}
