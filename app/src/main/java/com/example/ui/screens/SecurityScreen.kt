package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import com.example.data.model.BackgroundDetectionStatus
import com.example.data.model.BtDeviceType
import com.example.data.model.BtPerimeterDevice
import com.example.data.model.DuplicationGuardStatus
import com.example.data.model.DuplicationViolationType
import com.example.data.model.GatewaySwitchType
import com.example.data.model.GatewayTransitionEvent
import com.example.data.model.NetworkHardeningRecommendation
import com.example.util.TimeUtils
import com.example.util.rememberLiveCurrentTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiscoveredDevice
import com.example.data.model.ThreatLevel
import com.example.ui.MainViewModel
import com.example.ui.components.WavePulseRadar
import com.example.ui.theme.AlertRedGlow
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SecurityScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isSubnetScanning.collectAsState()
    val isShieldActive by viewModel.isRealTimeShieldActive.collectAsState()
    val alerts by viewModel.securityAlerts.collectAsState()

    val wifiState by viewModel.wifiState.collectAsState()
    val btDevices by viewModel.perimeterBtDevices.collectAsState()
    val isBtShieldActive by viewModel.isBtZeroToleranceShieldActive.collectAsState()
    val activeGatewayAlert by viewModel.activeGatewayAlert.collectAsState()
    val gatewayTransitions by viewModel.recentGatewayTransitions.collectAsState()
    val isGatewayLockdownActive by viewModel.gatewayLockdownActive.collectAsState()
    val lockedGatewayIp by viewModel.lockedGatewayIp.collectAsState()
    val lockedGatewayMac by viewModel.lockedGatewayMac.collectAsState()

    val isZeroTolerancePolicyActive by viewModel.isZeroTolerancePolicyActive.collectAsState()
    val isZeroFpEngineActive by viewModel.isZeroFpEngineActive.collectAsState()
    val falsePositivesSuppressedCount by viewModel.falsePositivesSuppressedCount.collectAsState()
    val totalPacketsDropped by viewModel.totalPacketsDropped.collectAsState()
    val totalBytesBlocked by viewModel.totalBytesBlocked.collectAsState()

    val duplicationStatus by viewModel.duplicationGuardStatus.collectAsState()
    val isZeroToleranceDuplicationActive by viewModel.isZeroToleranceDuplicationActive.collectAsState()

    val backgroundStatus by viewModel.backgroundDetectionStatus.collectAsState()
    val hardeningRecommendations by viewModel.networkHardeningRecommendations.collectAsState()
    val selectedHardeningRecommendation by viewModel.selectedHardeningRecommendation.collectAsState()
    val whitelistAudit by viewModel.whitelistAuditResult.collectAsState()
    val whitelistedEntities by viewModel.whitelistedDevices.collectAsState()
    val isAutoNotifyEnabled by viewModel.isAutoNotifyEnabled.collectAsState()
    val summaryReport by viewModel.networkSummaryReport.collectAsState()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()
    val liveNow by rememberLiveCurrentTime()

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, UNAUTHORIZED, FP_SUPPRESSED, AUTHORIZED
    var editingDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var aliasInput by remember { mutableStateOf("") }
    var showAlertsDialog by remember { mutableStateOf(false) }
    var showGatewayTransitionsDialog by remember { mutableStateOf(false) }

    val unauthorizedList = devices.filter { !it.isAuthorized && !it.isSelf && !it.isGateway && !it.isFalsePositiveSuppressed }
    val fpSuppressedList = devices.filter { it.isFalsePositiveSuppressed || (it.isRandomizedMac && it.isAuthorized) }
    val authorizedList = devices.filter { (it.isAuthorized || it.isSelf || it.isGateway) && !it.isFalsePositiveSuppressed && !(it.isRandomizedMac && it.isAuthorized) }
    val blockedCount = devices.count { it.isBlocked } + btDevices.count { it.isQuarantined }
    val filteredDevices = when (selectedFilter) {
        "UNAUTHORIZED" -> unauthorizedList
        "FP_SUPPRESSED" -> fpSuppressedList
        "AUTHORIZED" -> authorizedList
        else -> devices
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live Real-Time Telemetry & Timestamp Clock Banner
        item {
            RealtimeTelemetryClockBanner(
                currentTimeMillis = liveNow,
                isShieldActive = isShieldActive,
                isScanning = isScanning,
                lastScanTimestamp = backgroundStatus.lastScanTimestamp
            )
        }

        // High-Priority Active Gateway Transition Alert Banner
        activeGatewayAlert?.let { alert ->
            item {
                ActiveGatewayAlertBanner(
                    alert = alert,
                    onDismiss = { viewModel.dismissActiveGatewayAlert() }
                )
            }
        }

        // Network Health and Security Incident Summary Report (Scrollable Card Format)
        item {
            val context = LocalContext.current
            NetworkSummaryReportCard(
                report = summaryReport,
                isGenerating = isGeneratingReport,
                onRefreshReport = { viewModel.generateSummaryReport() },
                onExportReport = { viewModel.exportSummaryReport(context) }
            )
        }

        // Known-Device Whitelist Verification & Real-Time Intrusion Alert Sentry Card
        item {
            KnownDeviceWhitelistCard(
                auditResult = whitelistAudit,
                whitelistedEntities = whitelistedEntities,
                isAutoNotifyEnabled = isAutoNotifyEnabled,
                liveNow = liveNow,
                onRunAudit = { viewModel.runManualWhitelistComparison() },
                onToggleAutoNotify = { viewModel.toggleAutoNotification(it) },
                onSimulateUnknownIntruder = { viewModel.simulateUnknownDeviceIntrusion() },
                onWhitelistDevice = { viewModel.addDeviceToWhitelist(it) },
                onRemoveFromWhitelist = { viewModel.removeDeviceFromWhitelist(it) },
                onBlockDevice = { viewModel.quarantineDevice(it) }
            )
        }

        // Zero-Tolerance & False-Positive Sentry Card (CyberSec Hero)
        item {
            ZeroToleranceIntruderAndFpCard(
                isZeroToleranceActive = isZeroTolerancePolicyActive,
                isZeroFpActive = isZeroFpEngineActive,
                unauthorizedCount = unauthorizedList.size,
                blockedCount = blockedCount,
                suppressedFpCount = falsePositivesSuppressedCount,
                packetsDropped = totalPacketsDropped,
                bytesBlocked = totalBytesBlocked,
                liveNow = liveNow,
                lastScanTimestamp = backgroundStatus.lastScanTimestamp,
                onToggleZeroTolerance = { viewModel.toggleZeroTolerancePolicy(it) },
                onToggleZeroFp = { viewModel.toggleZeroFpEngine(it) },
                onSimulateBreach = { viewModel.simulateIntruderBreach() },
                onSimulateFalsePositive = { viewModel.simulateTransientFalsePositive() },
                onLockdownSubnet = { viewModel.lockdownAllUnauthorizedHosts() }
            )
        }

        // Zero-Tolerance to Duplications Guard Card (Gateways, Subnets, IPs, MACs)
        item {
            ZeroToleranceDuplicationGuardCard(
                status = duplicationStatus,
                isEnforced = isZeroToleranceDuplicationActive,
                liveNow = liveNow,
                onToggleEnforced = { viewModel.setZeroToleranceDuplicationActive(it) },
                onSimulateRogueGateway = { viewModel.simulateDuplicationBreach(DuplicationViolationType.ROGUE_GATEWAY) },
                onSimulateDuplicateIp = { viewModel.simulateDuplicationBreach(DuplicationViolationType.DUPLICATE_IP) },
                onSimulateDuplicateMac = { viewModel.simulateDuplicationBreach(DuplicationViolationType.DUPLICATE_MAC) },
                onSimulateAlienSubnet = { viewModel.simulateDuplicationBreach(DuplicationViolationType.ALIEN_SUBNET) }
            )
        }

        // Background Unknown Device Detection & Gemini AI Hardening Card
        item {
            BackgroundDetectionHardeningCard(
                status = backgroundStatus,
                recommendations = hardeningRecommendations,
                liveNow = liveNow,
                onToggleBackgroundDetection = { viewModel.toggleBackgroundDetection(it) },
                onTriggerAudit = { viewModel.triggerManualHardeningAudit() },
                onSimulateUnknownDevice = { viewModel.simulateUnknownDeviceIntrusion() },
                onSelectRecommendation = { viewModel.selectHardeningRecommendation(it) }
            )
        }

        // Gateway & Network Switch Guard Card
        item {
            GatewaySwitchGuardCard(
                currentGatewayIp = wifiState.gatewayIp,
                currentSsid = wifiState.ssid,
                currentBssid = wifiState.bssid,
                lockedIp = lockedGatewayIp,
                lockedMac = lockedGatewayMac,
                isLockdownActive = isGatewayLockdownActive,
                recentTransitionsCount = gatewayTransitions.size,
                onToggleLockdown = { viewModel.toggleGatewayLockdown(it) },
                onLockCurrentGateway = { viewModel.lockCurrentGateway() },
                onSimulateSwitch = { viewModel.simulateGatewaySwitch(isSpoofRogue = false) },
                onSimulateSpoof = { viewModel.simulateGatewaySwitch(isSpoofRogue = true) },
                onViewHistory = { showGatewayTransitionsDialog = true }
            )
        }

        // Zero-Tolerance Bluetooth Perimeter Sentry Card
        item {
            ZeroToleranceBtSentryCard(
                btDevices = btDevices,
                isShieldActive = isBtShieldActive,
                liveNow = liveNow,
                onToggleShield = { viewModel.toggleBtZeroToleranceShield(it) },
                onSimulateTracker = {
                    viewModel.simulateRogueBtDevice("Apple AirTag (Beacon in Range)", BtDeviceType.TRACKER)
                },
                onSimulateSniffer = {
                    viewModel.simulateRogueBtDevice("Rogue BLE RF Sniffer (Unpaired)", BtDeviceType.ROGUE_SNIFFER)
                },
                onToggleQuarantine = { address ->
                    viewModel.toggleBtQuarantine(address)
                },
                onToggleTrust = { address ->
                    viewModel.toggleBtDeviceTrust(address)
                },
                onDismissBtFalsePositive = { address ->
                    viewModel.dismissBtDeviceFalsePositive(address)
                }
            )
        }

        // Real-Time Sentinel Surveillance Card
        item {
            SurveillanceShieldCard(
                isShieldActive = isShieldActive,
                onToggleShield = { viewModel.toggleRealTimeShield(it) },
                unauthorizedCount = unauthorizedList.size,
                alertsCount = alerts.count { !it.isAcknowledged },
                onViewAlerts = { showAlertsDialog = true }
            )
        }

        // Radar Scan Bar
        item {
            RadarScanBar(
                isScanning = isScanning,
                totalCount = devices.size,
                unauthorizedCount = unauthorizedList.size,
                onScanClick = { viewModel.refreshSubnetDevices() }
            )
        }

        // Filters Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All Hosts (${devices.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                        selectedLabelColor = CyberCyan,
                        containerColor = CyberSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )

                FilterChip(
                    selected = selectedFilter == "UNAUTHORIZED",
                    onClick = { selectedFilter = "UNAUTHORIZED" },
                    label = { Text("Unauthorized (${unauthorizedList.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberRed.copy(alpha = 0.2f),
                        selectedLabelColor = CyberRed,
                        containerColor = CyberSurfaceElevated,
                        labelColor = if (unauthorizedList.isNotEmpty()) CyberRed else TextSecondary
                    )
                )

                FilterChip(
                    selected = selectedFilter == "FP_SUPPRESSED",
                    onClick = { selectedFilter = "FP_SUPPRESSED" },
                    label = { Text("Zero-FP Verified (${fpSuppressedList.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberTeal.copy(alpha = 0.2f),
                        selectedLabelColor = CyberTeal,
                        containerColor = CyberSurfaceElevated,
                        labelColor = if (fpSuppressedList.isNotEmpty()) CyberTeal else TextSecondary
                    )
                )

                FilterChip(
                    selected = selectedFilter == "AUTHORIZED",
                    onClick = { selectedFilter = "AUTHORIZED" },
                    label = { Text("Authorized (${authorizedList.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberGreen.copy(alpha = 0.2f),
                        selectedLabelColor = CyberGreen,
                        containerColor = CyberSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // Discovered Devices List
        if (filteredDevices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = when (selectedFilter) {
                                "UNAUTHORIZED" -> "No unauthorized intruders detected"
                                "FP_SUPPRESSED" -> "No false-positive suppressed devices"
                                "AUTHORIZED" -> "No authorized hosts matching filter"
                                else -> "No devices found"
                            },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (selectedFilter == "UNAUTHORIZED") "Zero-Tolerance policy active • 0 unverified threats." else "Network subnet is monitored under Zero-FP policy.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(filteredDevices, key = { it.macAddress }) { device ->
                val matchingRec = hardeningRecommendations.find { 
                    it.targetDeviceIp == device.ip || it.targetDeviceMac.equals(device.macAddress, ignoreCase = true) 
                }
                DeviceCard(
                    device = device,
                    liveNow = liveNow,
                    onToggleAuth = { viewModel.toggleDeviceAuthorization(device) },
                    onQuarantine = { viewModel.quarantineDevice(device) },
                    onDismissFalsePositive = { viewModel.dismissDeviceAsFalsePositive(device) },
                    onEditAlias = {
                        editingDevice = device
                        aliasInput = device.customName
                    },
                    onViewHardening = if (matchingRec != null) {
                        { viewModel.selectHardeningRecommendation(matchingRec) }
                    } else null
                )
            }
        }
    }

    // Gemini Network Hardening Recommendation Full Directive Dialog
    selectedHardeningRecommendation?.let { rec ->
        HardeningRecommendationDialog(
            recommendation = rec,
            liveNow = liveNow,
            onDismiss = { viewModel.selectHardeningRecommendation(null) }
        )
    }

    // Edit Device Friendly Name Dialog
    editingDevice?.let { dev ->
        AlertDialog(
            onDismissRequest = { editingDevice = null },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "Assign Device Alias",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set a friendly name for ${dev.ip} (${dev.macAddress}):",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = aliasInput,
                        onValueChange = { aliasInput = it },
                        placeholder = { Text("e.g. My MacBook Pro, Living Room TV") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setDeviceAlias(dev, aliasInput.trim())
                        editingDevice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Save Alias", color = CyberSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDevice = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Security Alert History Dialog
    if (showAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            containerColor = CyberSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Intrusion Alert Logs",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (alerts.isNotEmpty()) {
                        TextButton(onClick = { viewModel.acknowledgeAllAlerts() }) {
                            Text("Acknowledge All", color = CyberCyan, fontSize = 12.sp)
                        }
                    }
                }
            },
            text = {
                if (alerts.isEmpty()) {
                    Text(
                        text = "No security alerts recorded. Your network perimeter is clean.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(alerts) { alert ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (alert.isFalsePositive) CyberTeal.copy(alpha = 0.5f)
                                        else if (!alert.isAcknowledged) CyberRed
                                        else CyberBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = alert.title,
                                            color = if (alert.isFalsePositive) CyberTeal
                                            else if (!alert.isAcknowledged) CyberRed
                                            else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (alert.isFalsePositive) CyberTeal.copy(alpha = 0.2f)
                                                    else if (!alert.isAcknowledged) CyberRed.copy(alpha = 0.2f)
                                                    else CyberSurface
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (alert.isFalsePositive) "SUPPRESSED (FP)"
                                                else if (!alert.isAcknowledged) "ACTIVE ALERT (${alert.confidencePercent}%)"
                                                else "RESOLVED",
                                                color = if (alert.isFalsePositive) CyberTeal
                                                else if (!alert.isAcknowledged) CyberRed
                                                else TextMuted,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = alert.description,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Logged: ${TimeUtils.formatRealtimeBadge(alert.timestamp, liveNow)}",
                                            color = TextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        if (!alert.isFalsePositive && alert.deviceMac.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.markAlertAsFalsePositive(alert.id, alert.deviceMac)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("False Positive? Whitelist", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAlertsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Close", color = CyberSurface)
                }
            }
        )
    }

    // Gateway & Network Transitions History Dialog
    if (showGatewayTransitionsDialog) {
        GatewayTransitionsDialog(
            transitions = gatewayTransitions,
            liveNow = liveNow,
            onDismiss = { showGatewayTransitionsDialog = false }
        )
    }
}

@Composable
private fun RealtimeTelemetryClockBanner(
    currentTimeMillis: Long,
    isShieldActive: Boolean,
    isScanning: Boolean,
    lastScanTimestamp: Long
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF071219))
            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("realtime_telemetry_clock_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isShieldActive) CyberGreen else CyberAmber)
                )
                Text(
                    text = "REAL-TIME SENTINEL TELEMETRY",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = TimeUtils.formatTime(currentTimeMillis),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = when {
                        isScanning -> "• SWEEPING"
                        lastScanTimestamp > 0 -> "• ${TimeUtils.formatRelativeTime(lastScanTimestamp, currentTimeMillis)}"
                        else -> "• LIVE"
                    },
                    color = if (isScanning) CyberCyan else CyberTeal,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun SurveillanceShieldCard(
    isShieldActive: Boolean,
    onToggleShield: (Boolean) -> Unit,
    unauthorizedCount: Int,
    alertsCount: Int,
    onViewAlerts: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurfaceVariant)
            .border(
                1.dp,
                if (unauthorizedCount > 0) CyberRed else CyberBorder,
                RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
            .testTag("surveillance_shield_card")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isShieldActive) CyberCyan.copy(alpha = 0.15f) else CyberSurfaceElevated)
                            .border(1.dp, if (isShieldActive) CyberCyan else CyberBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isShieldActive) CyberCyan else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Real-Time Intrusion Shield",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isShieldActive) "Active continuous subnet surveillance" else "Surveillance Paused",
                            color = if (isShieldActive) CyberTeal else TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = isShieldActive,
                    onCheckedChange = onToggleShield,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("shield_toggle_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (unauthorizedCount > 0) AlertRedGlow else CyberGreen.copy(alpha = 0.15f))
                            .border(1.dp, if (unauthorizedCount > 0) CyberRed else CyberGreen, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (unauthorizedCount > 0) "$unauthorizedCount Unauthorized Intruders" else "0 Unauthorized Hosts",
                            color = if (unauthorizedCount > 0) CyberRed else CyberGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (alertsCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                            .clickable(onClick = onViewAlerts)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = CyberAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$alertsCount Alerts",
                                color = CyberAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarScanBar(
    isScanning: Boolean,
    totalCount: Int,
    unauthorizedCount: Int,
    onScanClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WavePulseRadar(isScanning = isScanning, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = if (isScanning) CyberCyan else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isScanning) "Probing Subnet ARP Table..." else "$totalCount Active Devices Online",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time ARP resolution & port ping",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onScanClick,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("scan_subnet_button")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        color = CyberSurface,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = CyberSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan", color = CyberSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: DiscoveredDevice,
    liveNow: Long = System.currentTimeMillis(),
    onToggleAuth: () -> Unit,
    onQuarantine: () -> Unit,
    onDismissFalsePositive: () -> Unit,
    onEditAlias: () -> Unit,
    onViewHardening: (() -> Unit)? = null
) {
    val isThreat = !device.isAuthorized && !device.isSelf && !device.isGateway && !device.isFalsePositiveSuppressed
    val borderColor = when {
        device.isBlocked -> CyberRed
        device.isFalsePositiveSuppressed -> CyberTeal.copy(alpha = 0.8f)
        isThreat -> CyberRed.copy(alpha = 0.8f)
        device.isGateway -> CyberCyan
        device.isSelf -> CyberTeal
        else -> CyberBorder
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    device.isBlocked -> Color(0xFF2E090F)
                    device.isFalsePositiveSuppressed -> Color(0xFF061E22)
                    isThreat -> Color(0xFF200E16)
                    else -> CyberSurfaceVariant
                }
            )
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("device_card_${device.macAddress.replace(":", "")}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = when {
                        device.isBlocked -> Icons.Default.Block
                        device.isFalsePositiveSuppressed -> Icons.Default.CheckCircle
                        device.isGateway -> Icons.Default.Router
                        device.isSelf -> Icons.Default.Smartphone
                        device.vendor.contains("Apple", ignoreCase = true) -> Icons.Default.Computer
                        else -> Icons.Default.Security
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    device.isBlocked -> CyberRed.copy(alpha = 0.3f)
                                    device.isFalsePositiveSuppressed -> CyberTeal.copy(alpha = 0.25f)
                                    isThreat -> AlertRedGlow
                                    else -> CyberSurfaceElevated
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    device.isBlocked || isThreat -> CyberRed
                                    device.isFalsePositiveSuppressed -> CyberTeal
                                    else -> CyberBorder
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = when {
                                device.isBlocked || isThreat -> CyberRed
                                device.isFalsePositiveSuppressed -> CyberTeal
                                else -> CyberCyan
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = device.displayName,
                                color = when {
                                    device.isBlocked || isThreat -> CyberRed
                                    device.isFalsePositiveSuppressed -> CyberTeal
                                    else -> TextPrimary
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (!device.isGateway && !device.isSelf) {
                                IconButton(
                                    onClick = onEditAlias,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Alias",
                                        tint = TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${device.ip} • ${device.macAddress}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Status Badge
                val badgeBg = when {
                    device.isBlocked -> CyberRed.copy(alpha = 0.25f)
                    device.isFalsePositiveSuppressed -> CyberTeal.copy(alpha = 0.25f)
                    isThreat -> CyberRed.copy(alpha = 0.2f)
                    device.isRandomizedMac && device.isAuthorized -> CyberTeal.copy(alpha = 0.2f)
                    else -> CyberGreen.copy(alpha = 0.15f)
                }
                val badgeColor = when {
                    device.isBlocked || isThreat -> CyberRed
                    device.isFalsePositiveSuppressed -> CyberTeal
                    device.isRandomizedMac && device.isAuthorized -> CyberTeal
                    else -> CyberGreen
                }
                val badgeText = when {
                    device.isRogueGateway -> "ROGUE GATEWAY • SEVERED"
                    device.isDuplicateIp -> "IP COLLISION • BLOCKED"
                    device.isDuplicateMac -> "MAC CLONE • ISOLATED"
                    device.isSubnetAnomaly -> "ALIEN SUBNET • QUARANTINED"
                    device.isBlocked -> "QUARANTINED • ZERO ACCESS"
                    device.isFalsePositiveSuppressed -> "ZERO-FP VERIFIED • SAFE"
                    isThreat -> "UNAUTHORIZED"
                    device.isGateway -> "GATEWAY"
                    device.isSelf -> "THIS HOST"
                    device.isRandomizedMac -> "VERIFIED (PRIVATE MAC)"
                    else -> "AUTHORIZED"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Fingerprint & Corroboration Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (device.isRandomizedMac) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberTeal.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Private MAC (LAA)",
                            color = CyberTeal,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberSurfaceElevated)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Confidence: ${device.confidencePercent}%",
                        color = if (device.confidencePercent >= 90) CyberCyan else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Vendor: ${device.vendor} (${device.responseTimeMs}ms)",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            if (device.corroborationVector.isNotBlank()) {
                Text(
                    text = device.corroborationVector,
                    color = if (isThreat) CyberAmber else TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Real-Time Timestamps & Activity Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyberSurfaceElevated.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "First: ${TimeUtils.formatRealtimeBadge(device.firstDetected, liveNow)}",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isRecent = (liveNow - device.lastSeen) < 30000L
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isRecent) CyberGreen else TextMuted)
                    )
                    Text(
                        text = "Seen: ${TimeUtils.formatRealtimeBadge(device.lastSeen, liveNow)}",
                        color = if (isRecent) CyberTeal else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Prominent Zero Network Access Banner for Blocked Devices
            if (device.isBlocked || !device.hasNetworkAccess) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberRed.copy(alpha = 0.2f))
                        .border(1.dp, CyberRed.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Zero Network Access",
                            tint = CyberRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Column {
                            Text(
                                text = "ZERO NETWORK ACCESS ENFORCED",
                                color = CyberRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Kernel firewall drop rule active. Ingress/egress ARP, IP & ICMP traffic dropped.",
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Zero-Tolerance Duplication Anomaly Detail Banner
            if (!device.duplicationAlertDetail.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberRed.copy(alpha = 0.18f))
                        .border(1.dp, CyberRed.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CyberRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Column {
                            Text(
                                text = "ZERO-TOLERANCE IDENTITY DUPLICATION VIOLATION",
                                color = CyberRed,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = device.duplicationAlertDetail,
                                color = TextSecondary,
                                fontSize = 8.5.sp
                            )
                        }
                    }
                }
            }

            // Gemini Zero-Trust Hardening Directive Banner
            if (device.isFlaggedUnknown || !device.aiHardeningNote.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCyan.copy(alpha = 0.08f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "GEMINI ZERO-TRUST HARDENING DIRECTIVE",
                                color = CyberCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (!device.aiHardeningNote.isNullOrBlank()) {
                            Text(
                                text = device.aiHardeningNote,
                                color = TextPrimary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }

                        if (onViewHardening != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberCyan.copy(alpha = 0.18f))
                                    .clickable { onViewHardening() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "VIEW FULL HARDENING PROTOCOL",
                                    color = CyberCyan,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            if (!device.isGateway && !device.isSelf) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (device.isBlocked) {
                        OutlinedButton(
                            onClick = onQuarantine,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                            modifier = Modifier.height(28.dp).weight(1f)
                        ) {
                            Text("Unblock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onToggleAuth,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                            modifier = Modifier.height(28.dp).weight(1f)
                        ) {
                            Text("Authorize", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else if (isThreat) {
                        OutlinedButton(
                            onClick = onQuarantine,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                            modifier = Modifier.height(28.dp).weight(1f)
                        ) {
                            Text("Quarantine", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onDismissFalsePositive,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal),
                            modifier = Modifier.height(28.dp).weight(1.3f)
                        ) {
                            Text("Suppress FP (Safe)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onToggleAuth,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                            modifier = Modifier.height(28.dp).weight(1f)
                        ) {
                            Text("Authorize", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else if (device.isFalsePositiveSuppressed) {
                        OutlinedButton(
                            onClick = onDismissFalsePositive,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal),
                            modifier = Modifier.height(28.dp).weight(1.2f)
                        ) {
                            Text("Zero-FP Verified", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onToggleAuth,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                            modifier = Modifier.height(28.dp).weight(1f)
                        ) {
                            Text("Revoke Trust", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onToggleAuth,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Revoke Trust", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZeroToleranceIntruderAndFpCard(
    isZeroToleranceActive: Boolean,
    isZeroFpActive: Boolean,
    unauthorizedCount: Int,
    blockedCount: Int,
    suppressedFpCount: Int,
    packetsDropped: Long = 0L,
    bytesBlocked: Long = 0L,
    liveNow: Long = System.currentTimeMillis(),
    lastScanTimestamp: Long = 0L,
    onToggleZeroTolerance: (Boolean) -> Unit,
    onToggleZeroFp: (Boolean) -> Unit,
    onSimulateBreach: () -> Unit,
    onSimulateFalsePositive: () -> Unit,
    onLockdownSubnet: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface)
            .border(
                1.dp,
                if (unauthorizedCount > 0) CyberRed else CyberCyan.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (unauthorizedCount > 0) CyberRed.copy(alpha = 0.15f) else CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Zero Tolerance Shield",
                            tint = if (unauthorizedCount > 0) CyberRed else CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Zero-Tolerance to False Positives",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "0.00% False-Positive Mandate • Dual-Pass Corroboration Engine",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Telemetry: ${TimeUtils.formatTime(liveNow)}",
                                color = CyberCyan,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            if (lastScanTimestamp > 0) {
                                Text(
                                    text = "• Sweep: ${TimeUtils.formatRelativeTime(lastScanTimestamp, liveNow)}",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Engine status badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.dp, if (isZeroToleranceActive) CyberRed.copy(alpha = 0.5f) else CyberBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "ZERO-TOLERANCE INTRUDER",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isZeroToleranceActive) CyberRed else TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isZeroToleranceActive) "ENFORCED (AUTO-BLOCK)" else "DISABLED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-blocks confirmed threats",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.dp, if (isZeroFpActive) CyberTeal.copy(alpha = 0.5f) else CyberBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "0% FALSE-POSITIVE POLICY",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isZeroFpActive) CyberTeal else TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isZeroFpActive) "STRICT ZERO-FP ENFORCED" else "BYPASS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "0 false alarms • Protects private MACs",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Real-Time Counter Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceElevated)
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$unauthorizedCount",
                        color = if (unauthorizedCount > 0) CyberRed else CyberGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Intruders", color = TextMuted, fontSize = 10.sp)
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(CyberBorder))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$blockedCount",
                        color = if (blockedCount > 0) CyberAmber else TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Quarantined", color = TextMuted, fontSize = 10.sp)
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(CyberBorder))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$suppressedFpCount",
                        color = CyberTeal,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("0% FP Filtered", color = TextMuted, fontSize = 10.sp)
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(CyberBorder))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "100.0%",
                        color = CyberCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Precision", color = TextMuted, fontSize = 10.sp)
                }
            }

            // Zero-Tolerance to False Positives Active Banner
            if (isZeroFpActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberTeal.copy(alpha = 0.12f))
                        .border(1.dp, CyberTeal.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(CyberTeal.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Zero-FP Active",
                                tint = CyberTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ZERO-TOLERANCE TO FALSE POSITIVES: ACTIVE",
                                color = CyberTeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Dual-pass corroboration validates randomized MACs (LAA) & traffic timing. Legitimate devices are never falsely isolated.",
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Firewall ACL Network Access Severed Banner
            if (blockedCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberRed.copy(alpha = 0.15f))
                        .border(1.dp, CyberRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CyberRed.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Firewall Active",
                                tint = CyberRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FIREWALL ACL ACTIVE • NETWORK ACCESS RESTRICTED",
                                color = CyberRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "$blockedCount quarantined devices have 0 network access • $packetsDropped dropped pkts (${bytesBlocked / 1024} KB blocked)",
                                color = TextSecondary,
                                fontSize = 9.5.sp
                            )
                        }
                    }
                }
            }

            // Policy Toggle Switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zero-Tolerance Auto-Block (Active Threats)",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Isolates unverified malicious attackers on sight (strictly zero false positive risk)",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = isZeroToleranceActive,
                    onCheckedChange = onToggleZeroTolerance,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberRed,
                        checkedTrackColor = CyberRed.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zero-Tolerance to False Positives (0% FP Policy)",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Dual-pass corroboration: protects benign randomized MACs & prevents false alarms",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = isZeroFpActive,
                    onCheckedChange = onToggleZeroFp,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberTeal,
                        checkedTrackColor = CyberTeal.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    )
                )
            }

            // Test Simulation and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulateBreach,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Text("Test Intruder", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSimulateFalsePositive,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1.2f).height(32.dp)
                ) {
                    Text("Test 0% FP Filter", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onLockdownSubnet,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1.1f).height(32.dp)
                ) {
                    Text("Lockdown", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ActiveGatewayAlertBanner(
    alert: GatewayTransitionEvent,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberRed.copy(alpha = 0.16f))
            .border(1.5.dp, CyberRed, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyberRed.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = CyberRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "NETWORK GATEWAY SWITCH DETECTED",
                            color = CyberRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = alert.switchType.label,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = alert.details,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SEVERITY: ${alert.severity}",
                    color = if (alert.severity == "CRITICAL") CyberRed else CyberAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Acknowledge", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GatewaySwitchGuardCard(
    currentGatewayIp: String,
    currentSsid: String,
    currentBssid: String,
    lockedIp: String,
    lockedMac: String,
    isLockdownActive: Boolean,
    recentTransitionsCount: Int,
    onToggleLockdown: (Boolean) -> Unit,
    onLockCurrentGateway: () -> Unit,
    onSimulateSwitch: () -> Unit,
    onSimulateSpoof: () -> Unit,
    onViewHistory: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface)
            .border(1.dp, if (isLockdownActive) CyberCyan.copy(alpha = 0.4f) else CyberBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Gateway Switch Monitor",
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Gateway & Network Switch Guard",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Real-time subnet & rogue ARP hopping detection",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isLockdownActive,
                    onCheckedChange = onToggleLockdown,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    )
                )
            }

            // Current Active vs Baseline Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ACTIVE GATEWAY",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isLockdownActive) "LOCKDOWN ACTIVE" else "MONITORING ONLY",
                            color = if (isLockdownActive) CyberGreen else CyberAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentGatewayIp.ifBlank { "192.168.1.1" },
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SSID: $currentSsid • BSSID: $currentBssid",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = onLockCurrentGateway,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCyan.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Baseline",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "Baseline locked: $lockedIp ($lockedMac)",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Action Simulation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulateSwitch,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Text("Test Switch", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onSimulateSpoof,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(34.dp)
                ) {
                    Text("Test Rogue ARP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onViewHistory,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Logs ($recentTransitionsCount)", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ZeroToleranceBtSentryCard(
    btDevices: List<BtPerimeterDevice>,
    isShieldActive: Boolean,
    liveNow: Long = System.currentTimeMillis(),
    onToggleShield: (Boolean) -> Unit,
    onSimulateTracker: () -> Unit,
    onSimulateSniffer: () -> Unit,
    onToggleQuarantine: (String) -> Unit,
    onToggleTrust: (String) -> Unit,
    onDismissBtFalsePositive: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface)
            .border(
                1.dp,
                if (btDevices.any { !it.isTrusted && !it.isFalsePositiveSuppressed }) CyberRed.copy(alpha = 0.6f) else CyberBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val activeThreatsCount = btDevices.count { !it.isTrusted && !it.isFalsePositiveSuppressed }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeThreatsCount > 0) CyberRed.copy(alpha = 0.15f) else CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                            contentDescription = "BT Sentry",
                            tint = if (activeThreatsCount > 0) CyberRed else CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Zero-Tolerance BT Sentry",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (activeThreatsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyberRed)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$activeThreatsCount THREATS",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Perimeter RF denial: Zero tolerance for rogue Bluetooth trackers & sniffers",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Live RF Monitor: ${TimeUtils.formatTime(liveNow)}",
                            color = CyberCyan,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Switch(
                    checked = isShieldActive,
                    onCheckedChange = onToggleShield,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberRed,
                        checkedTrackColor = CyberRed.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    )
                )
            }

            // Zero-Tolerance Notice Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberRed.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Zero Tolerance",
                        tint = CyberRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ZERO-TOLERANCE POLICY: Any unverified BLE beacon, tracker, or transmitter is intercepted. Legitimate peripherals can be trusted or suppressed as false-positives.",
                        color = CyberRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp
                    )
                }
            }

            // Simulation Trigger Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulateTracker,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                ) {
                    Text("+ Test AirTag Breach", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onSimulateSniffer,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                ) {
                    Text("+ Test Rogue Sniffer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // List of Detected Bluetooth Devices
            if (btDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceElevated)
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "BT Perimeter Clear",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "No unauthorized Bluetooth transmitters in proximity.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    btDevices.forEach { dev ->
                        BtDeviceItemRow(
                            device = dev,
                            liveNow = liveNow,
                            onToggleQuarantine = { onToggleQuarantine(dev.address) },
                            onToggleTrust = { onToggleTrust(dev.address) },
                            onDismissFalsePositive = { onDismissBtFalsePositive(dev.address) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BtDeviceItemRow(
    device: BtPerimeterDevice,
    liveNow: Long = System.currentTimeMillis(),
    onToggleQuarantine: () -> Unit,
    onToggleTrust: () -> Unit,
    onDismissFalsePositive: () -> Unit
) {
    val borderColor = when {
        device.isQuarantined -> CyberAmber.copy(alpha = 0.5f)
        device.isTrusted -> CyberGreen.copy(alpha = 0.5f)
        device.isFalsePositiveSuppressed -> CyberTeal.copy(alpha = 0.5f)
        else -> CyberRed.copy(alpha = 0.6f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val icon = when (device.deviceType) {
                        BtDeviceType.TRACKER -> Icons.Default.Sensors
                        BtDeviceType.AUDIO_HEADSET -> Icons.Default.Headphones
                        BtDeviceType.PHONE_PC -> Icons.Default.Smartphone
                        BtDeviceType.SMART_PERIPHERAL -> Icons.Default.Computer
                        BtDeviceType.ROGUE_SNIFFER -> Icons.Default.Warning
                    }
                    val iconTint = when {
                        device.isQuarantined -> CyberAmber
                        device.isTrusted -> CyberGreen
                        device.isFalsePositiveSuppressed -> CyberTeal
                        else -> CyberRed
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = device.deviceType.label,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = device.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${device.address} • ${device.signalGradeDescription}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Status Badge
                val (badgeText, badgeBg, badgeColor) = when {
                    device.isQuarantined -> Triple("QUARANTINED (NO ACCESS)", CyberAmber.copy(alpha = 0.25f), CyberAmber)
                    device.isTrusted -> Triple("TRUSTED", CyberGreen.copy(alpha = 0.25f), CyberGreen)
                    device.isFalsePositiveSuppressed -> Triple("FP SUPPRESSED", CyberTeal.copy(alpha = 0.25f), CyberTeal)
                    else -> Triple("BREACH (${device.confidencePercent}%)", CyberRed.copy(alpha = 0.25f), CyberRed)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (device.isQuarantined || !device.hasNetworkAccess) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberRed.copy(alpha = 0.2f))
                        .border(1.dp, CyberRed.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "No Network Access",
                            tint = CyberRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "NETWORK ACCESS SEVERED • ACL ISOLATION ENFORCED",
                            color = CyberRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Real-Time Activity Timestamps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyberSurface.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "First: ${TimeUtils.formatRealtimeBadge(device.firstDetected, liveNow)}",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Seen: ${TimeUtils.formatRealtimeBadge(device.lastSeen, liveNow)}",
                    color = CyberTeal,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Signal: ${device.rssi} dBm (${device.proximity})",
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!device.isTrusted && !device.isFalsePositiveSuppressed) {
                        OutlinedButton(
                            onClick = onDismissFalsePositive,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("False Pos?", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onToggleTrust,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (device.isTrusted) CyberAmber else CyberGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = if (device.isTrusted) "Untrust" else "Trust",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleQuarantine,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (device.isQuarantined) CyberGreen else CyberRed
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = if (device.isQuarantined) "Release" else "Quarantine",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GatewayTransitionsDialog(
    transitions: List<GatewayTransitionEvent>,
    liveNow: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Gateway Switch History",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        text = {
            if (transitions.isEmpty()) {
                Text(
                    text = "No gateway transitions recorded yet.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(transitions) { event ->
                        val isCritical = event.severity == "CRITICAL"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isCritical) CyberRed.copy(alpha = 0.5f) else CyberBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = event.switchType.label,
                                        color = if (isCritical) CyberRed else CyberCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = event.severity,
                                        color = if (isCritical) CyberRed else CyberAmber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Text(
                                    text = event.details,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Old: ${event.oldGatewayIp} (${event.oldGatewayMac}) -> New: ${event.newGatewayIp} (${event.newGatewayMac})",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Transitioned: ${TimeUtils.formatRealtimeBadge(event.timestamp, liveNow)}",
                                    color = CyberCyan,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text("Close", color = CyberSurface)
            }
        }
    )
}

@Composable
private fun ZeroToleranceDuplicationGuardCard(
    status: DuplicationGuardStatus,
    isEnforced: Boolean,
    liveNow: Long = System.currentTimeMillis(),
    onToggleEnforced: (Boolean) -> Unit,
    onSimulateRogueGateway: () -> Unit,
    onSimulateDuplicateIp: () -> Unit,
    onSimulateDuplicateMac: () -> Unit,
    onSimulateAlienSubnet: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface)
            .border(
                1.dp,
                if (status.totalViolations > 0) CyberRed else CyberCyan.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (status.totalViolations > 0) CyberRed.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Zero-Tolerance Duplication Guard",
                            tint = if (status.totalViolations > 0) CyberRed else CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Zero-Tolerance to Duplications",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Gateways • Subnets • IPs • MACs Deduplication Guard",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isEnforced,
                    onCheckedChange = onToggleEnforced,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    )
                )
            }

            // Status Banner
            if (status.totalViolations > 0 && status.lastViolationEvent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberRed.copy(alpha = 0.15f))
                        .border(1.dp, CyberRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = CyberRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "DUPLICATION VIOLATION INTERCEPTED & QUARANTINED",
                                color = CyberRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = status.lastViolationEvent.conflictingDetail,
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "Intercepted: ${TimeUtils.formatRealtimeBadge(status.lastViolationEvent.timestamp, liveNow)}",
                                color = CyberAmber,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberTeal.copy(alpha = 0.10f))
                        .border(1.dp, CyberTeal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CyberTeal,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "0 Duplicate Gateways • 0 Subnet Leaks • 0 IP Collisions • 0 MAC Clones",
                            color = CyberTeal,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 4-Pillar Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DuplicationMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "GATEWAYS",
                    value = if (status.duplicateGatewaysBlocked > 0) "${status.duplicateGatewaysBlocked} Blocked" else "1 Master",
                    sub = "0 Rogues Allowed",
                    isAlert = status.duplicateGatewaysBlocked > 0
                )
                DuplicationMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "SUBNETS",
                    value = if (status.subnetAnomaliesBlocked > 0) "${status.subnetAnomaliesBlocked} Leaks" else "Strict /24",
                    sub = "Boundary Isolation",
                    isAlert = status.subnetAnomaliesBlocked > 0
                )
                DuplicationMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "IP ADDRESSES",
                    value = if (status.duplicateIpsBlocked > 0) "${status.duplicateIpsBlocked} Collisions" else "0 Collisions",
                    sub = "ARP Poison Immune",
                    isAlert = status.duplicateIpsBlocked > 0
                )
                DuplicationMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "MAC ADDRESSES",
                    value = if (status.duplicateMacsDeduplicated > 0) "${status.duplicateMacsDeduplicated} Clones" else "100% Unique",
                    sub = "Layer-2 Unique",
                    isAlert = status.duplicateMacsDeduplicated > 0
                )
            }

            // Action Test Buttons
            Text(
                text = "VALIDATE ZERO-TOLERANCE DEFENSE AGAINST DUPLICATIONS:",
                color = TextMuted,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulateRogueGateway,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Test Rogue GW", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSimulateDuplicateIp,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Test IP Collision", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSimulateDuplicateMac,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Test MAC Clone", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSimulateAlienSubnet,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Test Alien Subnet", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DuplicationMetricItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String,
    isAlert: Boolean
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceElevated)
            .border(
                1.dp,
                if (isAlert) CyberRed.copy(alpha = 0.6f) else CyberBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = label,
                color = if (isAlert) CyberRed else TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = if (isAlert) CyberRed else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = sub,
                color = TextMuted,
                fontSize = 7.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BackgroundDetectionHardeningCard(
    status: BackgroundDetectionStatus,
    recommendations: List<NetworkHardeningRecommendation>,
    liveNow: Long = System.currentTimeMillis(),
    onToggleBackgroundDetection: (Boolean) -> Unit,
    onTriggerAudit: () -> Unit,
    onSimulateUnknownDevice: () -> Unit,
    onSelectRecommendation: (NetworkHardeningRecommendation) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface)
            .border(
                1.dp,
                if (status.unknownDevicesDetected > 0) CyberAmber.copy(alpha = 0.8f) else CyberCyan.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .testTag("background_detection_hardening_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.2f))
                            .border(1.dp, CyberCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "UNKNOWN DEVICE DETECTION & GEMINI HARDENING",
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Background Sentry & Automated Zero-Trust Directives",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Switch(
                    checked = status.isRunning,
                    onCheckedChange = onToggleBackgroundDetection,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("toggle_background_detection")
                )
            }

            // AI Analyzing State or Ready Status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (status.isAiAnalyzing) CyberCyan.copy(alpha = 0.12f)
                        else if (status.unknownDevicesDetected > 0) CyberAmber.copy(alpha = 0.12f)
                        else CyberTeal.copy(alpha = 0.08f)
                    )
                    .border(
                        1.dp,
                        if (status.isAiAnalyzing) CyberCyan.copy(alpha = 0.5f)
                        else if (status.unknownDevicesDetected > 0) CyberAmber.copy(alpha = 0.5f)
                        else CyberTeal.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (status.isAiAnalyzing) {
                        CircularProgressIndicator(
                            color = CyberCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Gemini 2.5 Flash synthesizing network hardening directives...",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    } else if (status.unknownDevicesDetected > 0) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CyberAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${status.unknownDevicesDetected} UNKNOWN HOSTS ACTIVE • HARDENING DIRECTIVES READY",
                            color = CyberAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CyberTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Background Sentry Active (every ${status.scanIntervalSeconds}s) • Zero-Trust Hardening Armed",
                            color = CyberTeal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DuplicationMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "UNKNOWN HOSTS",
                    value = "${status.unknownDevicesDetected}",
                    sub = "Flagged Threats",
                    isAlert = status.unknownDevicesDetected > 0
                )
                DuplicationMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "AI DIRECTIVES",
                    value = "${status.activeHardeningDirectives}",
                    sub = "Synthesized",
                    isAlert = false
                )
                DuplicationMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "SCAN INTERVAL",
                    value = "${status.scanIntervalSeconds}s",
                    sub = "IO Coroutines",
                    isAlert = false
                )
                DuplicationMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "AI ENGINE",
                    value = "Gemini 2.5",
                    sub = "Flash Model",
                    isAlert = false
                )
            }

            // Real-Time Subnet Sweep Telemetry Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyberSurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SURVEILLANCE SWEEP",
                    color = TextMuted,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Last: ${TimeUtils.formatRealtimeBadge(status.lastScanTimestamp, liveNow)}",
                    color = CyberCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTriggerAudit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("trigger_hardening_audit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Run Gemini Audit",
                        color = Color.Black,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                OutlinedButton(
                    onClick = onSimulateUnknownDevice,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("simulate_unknown_device_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CyberAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Simulate Intrusion",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Active AI Hardening Recommendations List
            if (recommendations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ACTIVE HARDENING DIRECTIVES (${recommendations.size})",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )

                    recommendations.take(3).forEach { rec ->
                        HardeningDirectiveSnippetCard(
                            recommendation = rec,
                            liveNow = liveNow,
                            onClick = { onSelectRecommendation(rec) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HardeningDirectiveSnippetCard(
    recommendation: NetworkHardeningRecommendation,
    liveNow: Long = System.currentTimeMillis(),
    onClick: () -> Unit
) {
    val riskColor = when (recommendation.riskLevel.uppercase()) {
        "CRITICAL" -> CyberRed
        "HIGH" -> CyberAmber
        else -> CyberCyan
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, riskColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(riskColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = recommendation.riskLevel.uppercase(),
                            color = riskColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "${recommendation.targetDeviceIp} • ${recommendation.vendor}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Inspect",
                        color = CyberCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Text(
                text = recommendation.threatAssessment,
                color = TextSecondary,
                fontSize = 9.5.sp,
                maxLines = 2,
                lineHeight = 13.sp
            )

            if (recommendation.firewallRules.isNotEmpty()) {
                Text(
                    text = "Rule: ${recommendation.firewallRules.first()}",
                    color = CyberCyan,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            Text(
                text = "Synthesized: ${TimeUtils.formatRealtimeBadge(recommendation.timestamp, liveNow)}",
                color = TextMuted,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun HardeningRecommendationDialog(
    recommendation: NetworkHardeningRecommendation,
    liveNow: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit
) {
    val riskColor = when (recommendation.riskLevel.uppercase()) {
        "CRITICAL" -> CyberRed
        "HIGH" -> CyberAmber
        else -> CyberCyan
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Gemini Hardening Directive",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${recommendation.targetDeviceIp} (${recommendation.targetDeviceMac})",
                        color = CyberCyan,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Synthesized: ${TimeUtils.formatRealtimeBadge(recommendation.timestamp, liveNow)}",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Risk Level & Threat Assessment
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(riskColor.copy(alpha = 0.12f))
                        .border(1.dp, riskColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "THREAT ASSESSMENT [${recommendation.riskLevel.uppercase()}]",
                                color = riskColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = recommendation.threatAssessment,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Firewall Directives
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "KERNEL / ROUTER FIREWALL DIRECTIVES",
                        color = CyberCyan,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF090D12))
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (rule in recommendation.firewallRules) {
                                Text(
                                    text = "$ $rule",
                                    color = CyberGreen,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Router Hardening Steps
                if (recommendation.routerHardeningSteps.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "RECOMMENDED ROUTER SETTINGS",
                            color = CyberAmber,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        for (step in recommendation.routerHardeningSteps) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "•", color = CyberAmber, fontSize = 11.sp)
                                Text(
                                    text = step,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                // Network Isolation & Zero-Trust Action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "IMMEDIATE ZERO-TRUST CONTAINMENT",
                            color = TextMuted,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = recommendation.zeroTrustAction,
                            color = CyberRed,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Isolation Target: ${recommendation.vlanOrIsolationAction}",
                            color = CyberTeal,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Acknowledge & Dismiss",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    )
}

