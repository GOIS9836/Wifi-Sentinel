package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.security.GatewayDiagnosticsResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import com.example.data.local.TrustedGateway
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.example.security.GatewayAlert
import com.example.security.SecurityDashboardUiState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GatewayFilterCategory
import com.example.data.model.GatewayRouterNode
import com.example.data.model.GatewayRouterType
import com.example.data.model.NetworkRiskAssessment
import com.example.data.model.ThreatVectorBreakdown
import com.example.data.model.UnethicalDevice
import com.example.data.model.UnethicalThreatType
import com.example.ui.MainViewModel
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHostRadar: () -> Unit = {},
    onNavigateToSentry: () -> Unit = {}
) {
    val quarantinedCount by viewModel.totalQuarantinedCount.collectAsStateWithLifecycle()
    val scanFrequency by viewModel.scanFrequencySeconds.collectAsStateWithLifecycle()
    val riskAssessment by viewModel.networkRiskAssessment.collectAsStateWithLifecycle()
    val isCalculatingRisk by viewModel.isCalculatingRiskScore.collectAsStateWithLifecycle()
    val unethicalDevices by viewModel.unethicalDevices.collectAsStateWithLifecycle()
    val isScanningUnethicals by viewModel.isScanningForUnethicals.collectAsStateWithLifecycle()
    val gateways by viewModel.filteredGateways.collectAsStateWithLifecycle()
    val allGateways by viewModel.managedGateways.collectAsStateWithLifecycle()
    val selectedGatewayFilter by viewModel.selectedGatewayFilter.collectAsStateWithLifecycle()
    val wifiState by viewModel.wifiState.collectAsStateWithLifecycle()
    val isShieldActive by viewModel.isRealTimeShieldActive.collectAsStateWithLifecycle()
    val guardUiState by viewModel.guardUiState.collectAsStateWithLifecycle()
    val trustedGateways by viewModel.trustedGateways.collectAsStateWithLifecycle()
    val primaryTrustedGateway by viewModel.primaryTrustedGateway.collectAsStateWithLifecycle()
    val gatewayDiagnostics by viewModel.gatewayDiagnostics.collectAsStateWithLifecycle()
    val isDiagnosingGateway by viewModel.isDiagnosingGateway.collectAsStateWithLifecycle()

    var showAddGatewayDialog by remember { mutableStateOf(false) }
    var showAddTrustedGatewayDialog by remember { mutableStateOf(false) }
    var showSimulateUnethicalMenu by remember { mutableStateOf(false) }
    var showQuarantineBreakdownDialog by remember { mutableStateOf(false) }
    var showExportConfigDialog by remember { mutableStateOf(false) }
    var showImportConfigDialog by remember { mutableStateOf(false) }
    var exportedConfigJson by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. High-Priority Gateway Anomaly Alert (Compliant Self-Defense)
        guardUiState.activeAlert?.let { alert ->
            item {
                CompliantGatewayAnomalyAlertBanner(
                    alert = alert,
                    onTriggerLockdown = { viewModel.triggerSafeLockdown() },
                    onTrustNewGateway = { viewModel.trustAndAddDetectedGateway(it, "Trusted Discovered Node", false) },
                    onSetPrimary = {
                        viewModel.setPrimaryTrustedGateway(it)
                        viewModel.dismissGuardGatewayAlert()
                    },
                    onDismiss = { viewModel.dismissGuardGatewayAlert() }
                )
            }
        }

        // 1. Dashboard Header Banner
        item {
            DashboardHeroHeader(
                ssid = wifiState.ssid,
                isShieldActive = isShieldActive,
                onRefreshAll = {
                    viewModel.refreshNetworkRiskScore()
                    viewModel.scanForUnethicals()
                }
            )
        }

        // 2. Real-Time Threat Metrics Grid
        item {
            RealTimeThreatMetricsGrid(
                quarantinedCount = quarantinedCount,
                scanFrequency = scanFrequency,
                riskAssessment = riskAssessment,
                isCalculatingRisk = isCalculatingRisk,
                onFrequencyChange = { viewModel.setScanFrequency(it) },
                onRefreshRisk = { viewModel.refreshNetworkRiskScore() },
                onInspectQuarantine = { showQuarantineBreakdownDialog = true }
            )
        }

        // 2.5 Compliant Self-Defense Engine & Privacy-Safe Guard
        item {
            CompliantSelfDefenseGuardCard(
                guardState = guardUiState,
                onTriggerLockdown = { viewModel.triggerSafeLockdown() },
                onReleaseLockdown = { viewModel.releaseSafeLockdown() },
                onToggleFpFilter = { viewModel.setFpFilterEnabled(it) },
                onSimulateAnomaly = { viewModel.simulateGatewayAnomalyForTesting() }
            )
        }

        // 3. Gemini API Network Risk Score & Threat Vectors Card
        item {
            GeminiNetworkRiskCard(
                riskAssessment = riskAssessment,
                isCalculatingRisk = isCalculatingRisk,
                onRecalculate = { viewModel.refreshNetworkRiskScore() }
            )
        }

        // 4. Wifi Sentinel AI: Scan for Unethicals Section Header & Controls
        item {
            WifiSentinelUnethicalsHeader(
                unethicalCount = unethicalDevices.size,
                isScanning = isScanningUnethicals,
                onScan = { viewModel.scanForUnethicals() },
                onOpenSimulateMenu = { showSimulateUnethicalMenu = true }
            )
        }

        // Unethicals Threat Cards
        if (unethicalDevices.isEmpty()) {
            item {
                EmptyUnethicalsCard(onSimulateTest = {
                    viewModel.simulateUnethicalThreat(UnethicalThreatType.ARP_POISONER)
                })
            }
        } else {
            items(unethicalDevices, key = { it.id }) { threat ->
                UnethicalDeviceCard(
                    threat = threat,
                    onQuarantine = { viewModel.quarantineUnethicalDevice(threat.id) },
                    onUnquarantine = { viewModel.unquarantineUnethicalDevice(threat.id) }
                )
            }
        }

        // 5. Multi-Gateways & Routers Management Filters Section
        item {
            MultiGatewaysManagementHeader(
                allGateways = allGateways,
                selectedFilter = selectedGatewayFilter,
                onSelectFilter = { viewModel.setGatewayFilter(it) },
                onAddGateway = { showAddGatewayDialog = true },
                onSimulateRogue = { viewModel.simulateRogueGatewayBreach() }
            )
        }

        // Filtered Gateway Cards
        if (gateways.isEmpty()) {
            item {
                EmptyGatewaysCard(selectedFilter = selectedGatewayFilter)
            }
        } else {
            items(gateways, key = { it.id }) { node ->
                GatewayRouterCard(
                    node = node,
                    onSetPrimary = { viewModel.setPrimaryGateway(node.id) },
                    onToggleLock = { viewModel.toggleGatewayLock(node.id) },
                    onQuarantine = { viewModel.quarantineGateway(node.id) },
                    onUnquarantine = { viewModel.unquarantineGateway(node.id) }
                )
            }
        }

        // 5.5 Room-Persisted Trusted Gateways (trusted_gateways table)
        item {
            TrustedGatewaysPersistenceCard(
                trustedGateways = trustedGateways,
                primaryGateway = primaryTrustedGateway,
                diagnosticsResult = gatewayDiagnostics,
                isDiagnosing = isDiagnosingGateway,
                onRunDiagnostics = { viewModel.runGatewayDiagnostics() },
                onExportConfig = {
                    exportedConfigJson = viewModel.exportSentinelSecurityConfigJson()
                    showExportConfigDialog = true
                },
                onImportConfig = { showImportConfigDialog = true },
                onAddGateway = { showAddTrustedGatewayDialog = true },
                onSetPrimary = { viewModel.setPrimaryTrustedGateway(it) },
                onDelete = { viewModel.removeTrustedGateway(it) }
            )
        }

        // Bottom Navigation Shortcuts
        item {
            DashboardShortcutsRow(
                onNavigateToHostRadar = onNavigateToHostRadar,
                onNavigateToSentry = onNavigateToSentry
            )
        }
    }

    // Dialogs
    if (showAddGatewayDialog) {
        AddGatewayDialog(
            onDismiss = { showAddGatewayDialog = false },
            onAdd = { ip, mac, ssid, type, vendor ->
                viewModel.addManagedGateway(ip, mac, ssid, type, vendor)
                showAddGatewayDialog = false
            }
        )
    }

    if (showAddTrustedGatewayDialog) {
        AddTrustedGatewayDialog(
            onDismiss = { showAddTrustedGatewayDialog = false },
            onAdd = { ip, bssid, ssid, subnetMask, label, isPrimary ->
                viewModel.addTrustedGateway(ip, bssid, ssid, subnetMask, label, isPrimary)
                showAddTrustedGatewayDialog = false
            }
        )
    }

    if (showQuarantineBreakdownDialog) {
        QuarantinedDevicesBreakdownDialog(
            quarantinedCount = quarantinedCount,
            unethicalQuarantined = unethicalDevices.count { it.isQuarantined },
            gatewayQuarantined = allGateways.count { it.isQuarantined },
            onDismiss = { showQuarantineBreakdownDialog = false }
        )
    }

    // Simulate Dropdown
    if (showSimulateUnethicalMenu) {
        SimulateUnethicalDialog(
            onDismiss = { showSimulateUnethicalMenu = false },
            onSelect = { type ->
                viewModel.simulateUnethicalThreat(type)
                showSimulateUnethicalMenu = false
            }
        )
    }

    if (showExportConfigDialog) {
        ExportConfigDialog(
            jsonConfig = exportedConfigJson,
            onDismiss = { showExportConfigDialog = false }
        )
    }

    if (showImportConfigDialog) {
        ImportConfigDialog(
            onDismiss = { showImportConfigDialog = false },
            onImport = { json ->
                viewModel.importSentinelSecurityConfigJson(json)
                showImportConfigDialog = false
            }
        )
    }
}

@Composable
private fun DashboardHeroHeader(
    ssid: String,
    isShieldActive: Boolean,
    onRefreshAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_hero_header"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberCyan, CyberTeal)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .border(1.dp, CyberCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Dashboard",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SECURITY DASHBOARD",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 0.8.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isShieldActive) CyberGreen else CyberAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isShieldActive) "WiFi Sentinel AI Active • $ssid" else "Monitoring Paused",
                                color = if (isShieldActive) CyberGreen else CyberAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onRefreshAll,
                    modifier = Modifier.testTag("dashboard_refresh_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Dashboard",
                        tint = CyberCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun RealTimeThreatMetricsGrid(
    quarantinedCount: Int,
    scanFrequency: Int,
    riskAssessment: NetworkRiskAssessment,
    isCalculatingRisk: Boolean,
    onFrequencyChange: (Int) -> Unit,
    onRefreshRisk: () -> Unit,
    onInspectQuarantine: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "REAL-TIME THREAT METRICS",
            color = CyberCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metric 1: Quarantined Devices
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_quarantined_devices_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(
                        listOf(
                            if (quarantinedCount > 0) CyberRed else CyberBorder,
                            CyberSurfaceElevated
                        )
                    )
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUARANTINED",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (quarantinedCount > 0) CyberRed else CyberGreen)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$quarantinedCount",
                        color = if (quarantinedCount > 0) CyberRed else CyberGreen,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = if (quarantinedCount > 0) "Zero-Leakage ACL Drop" else "Perimeter Clean",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onInspectQuarantine,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .testTag("inspect_quarantine_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (quarantinedCount > 0) CyberRed else CyberCyan
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(CyberBorder, CyberCyan))
                        )
                    ) {
                        Text("Breakdown", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Metric 2: Scan Frequency
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_scan_frequency_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(listOf(CyberCyan.copy(alpha = 0.6f), CyberSurfaceElevated))
                )
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SCAN FREQUENCY",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .alpha(pulseAlpha)
                                .background(CyberCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${scanFrequency}s",
                        color = CyberCyan,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = when (scanFrequency) {
                            5 -> "Hyper-Watch Active"
                            10 -> "Optimal Shield"
                            20 -> "Balanced Sentinel"
                            else -> "Eco Interval"
                        },
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Frequency Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(5, 10, 20, 30).forEach { sec ->
                            val isSelected = scanFrequency == sec
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberCyan else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("freq_chip_${sec}s"),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.text.ClickableText(
                                    text = androidx.compose.ui.text.AnnotatedString("${sec}s"),
                                    style = androidx.compose.ui.text.TextStyle(
                                        color = if (isSelected) CyberCyan else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    onClick = { onFrequencyChange(sec) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiNetworkRiskCard(
    riskAssessment: NetworkRiskAssessment,
    isCalculatingRisk: Boolean,
    onRecalculate: () -> Unit
) {
    val score = riskAssessment.riskScore
    val scoreColor = when {
        score >= 80 -> CyberRed
        score >= 60 -> CyberAmber
        score >= 40 -> Color(0xFFFBBF24)
        score >= 20 -> CyberTeal
        else -> CyberGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gemini_network_risk_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(scoreColor.copy(alpha = 0.6f), CyberBorder))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(scoreColor.copy(alpha = 0.15f))
                            .border(1.dp, scoreColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Risk Assessment",
                            tint = scoreColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NETWORK RISK SCORE",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (riskAssessment.isGeminiLive) "Provided by Gemini 3.5 Flash" else "Heuristic Engine Baseline",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (isCalculatingRisk) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CyberCyan
                    )
                } else {
                    OutlinedButton(
                        onClick = onRecalculate,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("recalculate_risk_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(CyberCyan, CyberTeal))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Gauge & Level Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$score",
                        color = scoreColor,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "RISK INDEX / 100",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1.8f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = scoreColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor)
                    ) {
                        Text(
                            text = "STATUS: ${riskAssessment.riskLevel}",
                            color = scoreColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = riskAssessment.summary,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Threat Vector Breakdown Progress Bars
            if (riskAssessment.threatVectors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "THREAT VECTOR BREAKDOWN",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                riskAssessment.threatVectors.forEach { vector ->
                    val vectorColor = when (vector.severity) {
                        "CRITICAL" -> CyberRed
                        "WARNING" -> CyberAmber
                        else -> CyberGreen
                    }
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = vector.vectorName, color = TextPrimary, fontSize = 11.sp)
                            Text(
                                text = "${vector.score}% (${vector.severity})",
                                color = vectorColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { (vector.score / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = vectorColor,
                            trackColor = CyberSurfaceElevated
                        )
                    }
                }
            }

            // AI Recommendations
            if (riskAssessment.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "AI DEFENSIVE DIRECTIVES",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                riskAssessment.recommendations.take(3).forEach { rec ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("• ", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = rec, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiSentinelUnethicalsHeader(
    unethicalCount: Int,
    isScanning: Boolean,
    onScan: () -> Unit,
    onOpenSimulateMenu: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WIFI SENTINEL AI: UNETHICALS",
                    color = if (unethicalCount > 0) CyberRed else CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Detecting ARP poisoners, promiscuous sniffers & deauth nodes",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (unethicalCount > 0) CyberRed.copy(alpha = 0.2f) else CyberGreen.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (unethicalCount > 0) CyberRed else CyberGreen
                )
            ) {
                Text(
                    text = if (unethicalCount > 0) "$unethicalCount DETECTED" else "0 THREATS",
                    color = if (unethicalCount > 0) CyberRed else CyberGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onScan,
                enabled = !isScanning,
                modifier = Modifier
                    .weight(1.5f)
                    .height(38.dp)
                    .testTag("scan_for_unethicals_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = CyberBackground
                )
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CyberBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scanning Subnet...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SCAN FOR UNETHICALS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }

            OutlinedButton(
                onClick = onOpenSimulateMenu,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("simulate_unethical_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(listOf(CyberAmber, CyberRed))
                )
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Simulate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyUnethicalsCard(onSimulateTest: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_unethicals_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CyberGreen.copy(alpha = 0.4f), CyberSurfaceElevated)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CyberGreen.copy(alpha = 0.15f))
                    .border(1.dp, CyberGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CyberGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Perimeter Clean: Zero Unethical Behaviors",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "No promiscuous sniffers, ARP poisoners, or rogue cloner beacons observed on this subnet.",
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSimulateTest,
                modifier = Modifier
                    .height(30.dp)
                    .testTag("simulate_test_unethical_button"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber)
            ) {
                Text("Simulate MITM Actor for Testing", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun UnethicalDeviceCard(
    threat: UnethicalDevice,
    onQuarantine: () -> Unit,
    onUnquarantine: () -> Unit
) {
    val isQuarantined = threat.isQuarantined
    val severityColor = when (threat.severity) {
        "CRITICAL" -> CyberRed
        "HIGH" -> CyberAmber
        else -> Color(0xFFFBBF24)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("unethical_card_${threat.mac}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isQuarantined) CyberSurfaceElevated.copy(alpha = 0.7f) else CyberSurface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (isQuarantined) CyberGreen.copy(alpha = 0.6f) else CyberRed,
                    CyberBorder
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Threat Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                (if (isQuarantined) CyberGreen else severityColor).copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (isQuarantined) CyberGreen else severityColor,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isQuarantined) Icons.Default.Lock else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isQuarantined) CyberGreen else severityColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = threat.threatType.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = threat.threatType.category,
                            color = severityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isQuarantined) CyberGreen.copy(alpha = 0.15f) else CyberRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isQuarantined) CyberGreen else CyberRed
                    )
                ) {
                    Text(
                        text = if (isQuarantined) "QUARANTINED" else "ACTIVE THREAT",
                        color = if (isQuarantined) CyberGreen else CyberRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Signature Details
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CyberSurfaceElevated.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = threat.signatureDetail,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "IP: ${threat.ip}",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "MAC: ${threat.mac}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${threat.packetAnomalyCount} Anomaly Frames",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (isQuarantined) {
                    OutlinedButton(
                        onClick = onUnquarantine,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("unquarantine_unethical_${threat.mac}"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Restore Access", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = onQuarantine,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("quarantine_unethical_${threat.mac}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberRed,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("1-TAP QUARANTINE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiGatewaysManagementHeader(
    allGateways: List<GatewayRouterNode>,
    selectedFilter: GatewayFilterCategory,
    onSelectFilter: (GatewayFilterCategory) -> Unit,
    onAddGateway: () -> Unit,
    onSimulateRogue: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MULTI-GATEWAYS & ROUTERS",
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Topology filters & rogue gateway containment",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onSimulateRogue,
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("simulate_rogue_gateway_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(CyberRed, CyberAmber))
                    )
                ) {
                    Text("Simulate Rogue", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddGateway,
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("add_gateway_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = CyberBackground
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Add Node", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Horizontal Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GatewayFilterCategory.values().forEach { cat ->
                val isSelected = selectedFilter == cat
                val count = when (cat) {
                    GatewayFilterCategory.ALL -> allGateways.size
                    GatewayFilterCategory.PRIMARY -> allGateways.count { it.type == GatewayRouterType.PRIMARY_DEFAULT }
                    GatewayFilterCategory.MESH_NODES -> allGateways.count { it.type == GatewayRouterType.MESH_SATELLITE }
                    GatewayFilterCategory.SECONDARY -> allGateways.count { it.type == GatewayRouterType.SECONDARY_GATEWAY }
                    GatewayFilterCategory.VIRTUAL -> allGateways.count { it.type == GatewayRouterType.VIRTUAL_BRIDGE }
                    GatewayFilterCategory.ROGUE_DUPLICATE -> allGateways.count { it.type == GatewayRouterType.ROGUE_DUPLICATE }
                }

                val isRogueAlert = cat == GatewayFilterCategory.ROGUE_DUPLICATE && count > 0

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectFilter(cat) },
                    label = {
                        Text(
                            text = "${cat.label} ($count)",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected || isRogueAlert) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isRogueAlert) CyberRed.copy(alpha = 0.25f) else CyberCyan.copy(alpha = 0.2f),
                        selectedLabelColor = if (isRogueAlert) CyberRed else CyberCyan,
                        containerColor = CyberSurfaceElevated,
                        labelColor = if (isRogueAlert) CyberRed else TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isRogueAlert) CyberRed else if (isSelected) CyberCyan else Color.Transparent
                    ),
                    modifier = Modifier.testTag("filter_chip_${cat.name}")
                )
            }
        }
    }
}

@Composable
private fun EmptyGatewaysCard(selectedFilter: GatewayFilterCategory) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_gateways_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.WifiTethering,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No gateways matching ${selectedFilter.label}",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Adjust filters above or click '+ Add Node' to register routes.",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun GatewayRouterCard(
    node: GatewayRouterNode,
    onSetPrimary: () -> Unit,
    onToggleLock: () -> Unit,
    onQuarantine: () -> Unit,
    onUnquarantine: () -> Unit
) {
    val isRogue = node.type == GatewayRouterType.ROGUE_DUPLICATE
    val isQuarantined = node.isQuarantined
    val cardColor = when {
        isQuarantined -> CyberSurfaceElevated.copy(alpha = 0.7f)
        isRogue -> CyberSurface
        node.isCurrentActive -> CyberSurfaceElevated
        else -> CyberSurface
    }

    val borderColor = when {
        isQuarantined -> CyberRed
        isRogue -> CyberRed
        node.isCurrentActive -> CyberCyan
        else -> CyberBorder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gateway_card_${node.ip}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(borderColor, CyberBorder)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Icon, IP/SSID, Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(borderColor.copy(alpha = 0.15f))
                            .border(1.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (node.type) {
                                GatewayRouterType.PRIMARY_DEFAULT -> Icons.Default.Wifi
                                GatewayRouterType.MESH_SATELLITE -> Icons.Default.WifiTethering
                                GatewayRouterType.ROGUE_DUPLICATE -> Icons.Default.Warning
                                else -> Icons.Default.Security
                            },
                            contentDescription = null,
                            tint = borderColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = node.ip,
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (node.isCurrentActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CyberCyan.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "DEFAULT ACTIVE",
                                        color = CyberCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${node.vendor} • ${node.type.label}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isQuarantined -> CyberRed.copy(alpha = 0.2f)
                        isRogue -> CyberRed.copy(alpha = 0.2f)
                        else -> CyberTeal.copy(alpha = 0.15f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isQuarantined || isRogue) CyberRed else CyberTeal
                    )
                ) {
                    Text(
                        text = if (isQuarantined) "QUARANTINED" else if (isRogue) "ROGUE CLONE" else "VERIFIED",
                        color = if (isQuarantined || isRogue) CyberRed else CyberTeal,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subnet Metrics Row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CyberSurfaceElevated.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("MAC ADDRESS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(node.mac, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("SUBNET ROUTE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(node.routeSubnet, color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("LATENCY / HOP", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${node.latencyMs}ms • Hop ${node.hopMetric}", color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Management Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!node.isCurrentActive && !isQuarantined && !isRogue) {
                        OutlinedButton(
                            onClick = onSetPrimary,
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("set_primary_gw_${node.ip}"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                        ) {
                            Text("Set Active", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onToggleLock,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("lock_gw_${node.ip}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (node.isLocked) CyberGreen else TextSecondary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (node.isLocked) "ARP Locked" else "Unlock ARP", fontSize = 10.sp)
                    }
                }

                if (isQuarantined) {
                    OutlinedButton(
                        onClick = onUnquarantine,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("unquarantine_gw_${node.ip}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Unquarantine", fontSize = 10.sp)
                    }
                } else {
                    Button(
                        onClick = onQuarantine,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("quarantine_gw_${node.ip}"),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberRed,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Drop / Quarantine", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardShortcutsRow(
    onNavigateToHostRadar: () -> Unit,
    onNavigateToSentry: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onNavigateToHostRadar,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("shortcut_host_radar_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(listOf(CyberCyan, CyberTeal))
            )
        ) {
            Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Host Radar View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onNavigateToSentry,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("shortcut_sentry_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberPurple),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(listOf(CyberPurple, CyberCyan))
            )
        ) {
            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sentry & Whitelist", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddGatewayDialog(
    onDismiss: () -> Unit,
    onAdd: (ip: String, mac: String, ssid: String, type: GatewayRouterType, vendor: String) -> Unit
) {
    var ip by remember { mutableStateOf("192.168.1.2") }
    var mac by remember { mutableStateOf("00:1B:63:84:45:E6") }
    var ssid by remember { mutableStateOf("Enterprise-Secondary") }
    var vendor by remember { mutableStateOf("Ubiquiti EdgeRouter") }
    var selectedType by remember { mutableStateOf(GatewayRouterType.SECONDARY_GATEWAY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Gateway / Router Node",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("Gateway IP") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_gw_ip_input")
                )
                OutlinedTextField(
                    value = mac,
                    onValueChange = { mac = it },
                    label = { Text("MAC Address") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_gw_mac_input")
                )
                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("Vendor / Hardware") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_gw_vendor_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(ip, mac, ssid, selectedType, vendor) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBackground),
                modifier = Modifier.testTag("submit_add_gateway_button")
            ) {
                Text("Register Node", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CyberSurfaceElevated
    )
}

@Composable
private fun QuarantinedDevicesBreakdownDialog(
    quarantinedCount: Int,
    unethicalQuarantined: Int,
    gatewayQuarantined: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyberRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quarantine Audit Ledger", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Total Devices Fully Isolated: $quarantinedCount",
                    color = CyberRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "All listed devices have 0% forward and input network traffic under kernel firewall rules.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Unethical Nodes Isolated:", color = TextSecondary, fontSize = 12.sp)
                    Text("$unethicalQuarantined", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Rogue Gateways Dropped:", color = TextSecondary, fontSize = 12.sp)
                    Text("$gatewayQuarantined", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Subnet Intruders Quarantined:", color = TextSecondary, fontSize = 12.sp)
                    Text("${quarantinedCount - unethicalQuarantined - gatewayQuarantined}", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBackground)
            ) {
                Text("Dismiss", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CyberSurfaceElevated
    )
}

@Composable
private fun SimulateUnethicalDialog(
    onDismiss: () -> Unit,
    onSelect: (UnethicalThreatType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Simulate Unethical Actor", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Inject realistic signature to test autonomous defenses:", color = TextMuted, fontSize = 12.sp)
                UnethicalThreatType.values().forEach { type ->
                    OutlinedButton(
                        onClick = { onSelect(type) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(CyberBorder, CyberAmber))
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                            Text(type.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAmber)
                            Text(type.category, fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CyberSurfaceElevated
    )
}

@Composable
private fun CompliantGatewayAnomalyAlertBanner(
    alert: GatewayAlert,
    onTriggerLockdown: () -> Unit,
    onTrustNewGateway: (String) -> Unit,
    onSetPrimary: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isKnown = alert.isKnownInDatabase
    val borderColor = if (isKnown) CyberAmber else CyberRed
    val bgColor = if (isKnown) CyberAmber.copy(alpha = 0.16f) else CyberRed.copy(alpha = 0.18f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("banner_gateway_anomaly_alert"),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Icon(
                        imageVector = if (isKnown) Icons.Default.Info else Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = borderColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isKnown) "RECOGNIZED MESH NODE ROAMING" else "CRITICAL ROGUE GATEWAY ANOMALY",
                        color = borderColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = if (isKnown) {
                    "Device transitioned from ${alert.oldGateway} to registered node '${alert.matchedLabel}' (${alert.newGateway}). Would you like to lock this node as your active primary baseline?"
                } else {
                    "Default Gateway shifted unexpectedly from ${alert.oldGateway} to unrecognized ${alert.newGateway}. This matches ARP cache poisoning, gateway hijacking, or a rogue AP impersonation vector."
                },
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isKnown) {
                    Button(
                        onClick = { onSetPrimary(alert.newGateway) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("btn_banner_set_primary_mesh")
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set as Primary Baseline", color = CyberBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Keep Baseline", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = onTriggerLockdown,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("btn_banner_lockdown")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Safe Lockdown", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onTrustNewGateway(alert.newGateway) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_banner_trust_gateway")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberBackground)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Trust Node", color = CyberBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        Text("Dismiss", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompliantSelfDefenseGuardCard(
    guardState: SecurityDashboardUiState,
    onTriggerLockdown: () -> Unit,
    onReleaseLockdown: () -> Unit,
    onToggleFpFilter: (Boolean) -> Unit,
    onSimulateAnomaly: () -> Unit
) {
    val isLockdown = guardState.isLockdownActive
    val borderColor = if (isLockdown) CyberRed else CyberCyan.copy(alpha = 0.5f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .testTag("card_compliant_self_defense"),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLockdown) CyberRed.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLockdown) Icons.Default.Lock else Icons.Default.Security,
                            contentDescription = "Self Defense",
                            tint = if (isLockdown) CyberRed else CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Compliant Self-Defense Engine",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "NetworkGuardManager • Policy Safe",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isLockdown) CyberRed.copy(alpha = 0.2f) else CyberGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isLockdown) CyberRed else CyberGreen
                    )
                ) {
                    Text(
                        text = if (isLockdown) "HOST ISOLATED" else "SYSTEM ARMED",
                        color = if (isLockdown) CyberRed else CyberGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Architecture details block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Baseline Gateway:", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = guardState.lockedGatewayBaseline ?: "192.168.1.1 (Monitoring)",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Host Routing State:", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = if (isLockdown) "Unbound (bindProcessToNetwork: null)" else "Connected & Monitored",
                        color = if (isLockdown) CyberRed else CyberGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Store Policy Guarantee:", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = "100% Google Play Compliant (No Deauth/Injection)",
                        color = CyberTeal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Primary Action Button (Safe Lockdown)
            if (!isLockdown) {
                Button(
                    onClick = onTriggerLockdown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_trigger_safe_lockdown"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Execute Safe Lockdown (Self-Isolation)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            } else {
                Button(
                    onClick = onReleaseLockdown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_release_safe_lockdown"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CyberSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Release Lockdown & Restore Sockets",
                        color = CyberSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Secondary Controls: Simulate Anomaly
            OutlinedButton(
                onClick = onSimulateAnomaly,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_simulate_gateway_shift"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(listOf(CyberBorder, CyberCyan.copy(alpha = 0.6f)))
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = CyberCyan
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simulate Gateway Anomaly (Test Isolation)", fontSize = 11.sp, color = CyberCyan)
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CyberBorder.copy(alpha = 0.4f))
            )

            // Privacy-Safe Corroborator (MacSanitizer)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacy-Safe False-Positive Filter (MacSanitizer)",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Suppresses randomized MACs (IEEE 802 LAA) to prevent flagging benign personal devices.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                    Switch(
                        checked = guardState.fpFilterEnabled,
                        onCheckedChange = onToggleFpFilter,
                        modifier = Modifier.testTag("switch_fp_filter"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberSurface
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Filtered Randomized MACs: ${guardState.fpFilteredCount}",
                        color = CyberAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "SHA-256 Hashed Audits: ${guardState.totalHashedAuditsCount}",
                        color = CyberTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustedGatewaysPersistenceCard(
    trustedGateways: List<TrustedGateway>,
    primaryGateway: TrustedGateway?,
    diagnosticsResult: GatewayDiagnosticsResult?,
    isDiagnosing: Boolean,
    onRunDiagnostics: () -> Unit,
    onExportConfig: () -> Unit,
    onImportConfig: () -> Unit,
    onAddGateway: () -> Unit,
    onSetPrimary: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trusted_gateways_persistence_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Trusted Gateways", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Room DB • Anomaly Self-Defense", color = TextMuted, fontSize = 11.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onRunDiagnostics,
                        enabled = !isDiagnosing,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_run_gateway_diagnostics"),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.18f), contentColor = CyberCyan),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (isDiagnosing) "Probing..." else "Probe", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onAddGateway,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_open_add_trusted_gw"),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBackground),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Add Node", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (primaryGateway != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Active Baseline: ${primaryGateway.label} (${primaryGateway.gatewayIp})",
                        color = CyberGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Diagnostic Progress Indicator
            if (isDiagnosing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberBackground.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Diagnosing Socket RTT, DNS & Admin Ports...", color = CyberCyan, fontSize = 11.sp)
                        }
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = CyberCyan,
                            trackColor = CyberBorder
                        )
                    }
                }
            }

            // Diagnostics Result Panel
            diagnosticsResult?.let { diag ->
                val gradeColor = when {
                    diag.healthGrade.startsWith("A") -> CyberGreen
                    diag.healthGrade.startsWith("B") -> CyberCyan
                    diag.healthGrade.startsWith("WARN") -> CyberAmber
                    else -> CyberRed
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_gateway_diagnostics_result"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberBackground.copy(alpha = 0.8f)),
                    border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = gradeColor, modifier = Modifier.size(14.dp))
                                Text("Gateway Integrity Audit", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .background(gradeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(diag.healthGrade, color = gradeColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("RTT / Jitter", color = TextMuted, fontSize = 10.sp)
                                Text("${diag.avgLatencyMs} ms (±${diag.jitterMs}ms)", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("DNS Resolver", color = TextMuted, fontSize = 10.sp)
                                Text(
                                    if (diag.isDnsTampered) "TAMPERED (${diag.dnsServerIp})" else "Clean (${diag.dnsServerIp})",
                                    color = if (diag.isDnsTampered) CyberRed else CyberGreen,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column {
                                Text("Admin Interface", color = TextMuted, fontSize = 10.sp)
                                Text(
                                    if (diag.hasEncryptedAdmin) "Port 443 (HTTPS)" else "Standard / Closed",
                                    color = if (diag.hasEncryptedAdmin) CyberGreen else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = diag.diagnosticSummary,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            if (trustedGateways.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No trusted gateways registered in Room database.", color = TextSecondary, fontSize = 12.sp)
                }
            } else {
                trustedGateways.forEach { gw ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trusted_gw_item_${gw.gatewayIp}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberBackground.copy(alpha = 0.7f)),
                        border = BorderStroke(
                            1.dp,
                            if (gw.isPrimary) CyberGreen.copy(alpha = 0.5f) else CyberBorder.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(gw.label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (gw.isPrimary) {
                                        Box(
                                            modifier = Modifier
                                                .background(CyberGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text("PRIMARY", color = CyberGreen, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                                Text(
                                    text = "IP: ${gw.gatewayIp} • Subnet: ${gw.subnetMask}",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "SSID: ${gw.ssid} • MAC (yechipangidzo): ${gw.bssid ?: "None/Auto"}",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!gw.isPrimary) {
                                    OutlinedButton(
                                        onClick = { onSetPrimary(gw.gatewayIp) },
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("btn_make_primary_${gw.gatewayIp}"),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                                    ) {
                                        Text("Set Primary", fontSize = 10.sp)
                                    }
                                }
                                IconButton(
                                    onClick = { onDelete(gw.gatewayIp) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("btn_delete_trusted_gw_${gw.gatewayIp}")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CyberRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Export / Import Backup Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportConfig,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .testTag("btn_export_sentinel_json"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                ) {
                    Text("Export Config JSON", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }

                OutlinedButton(
                    onClick = onImportConfig,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .testTag("btn_import_sentinel_json"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f))
                ) {
                    Text("Import Backup JSON", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun AddTrustedGatewayDialog(
    onDismiss: () -> Unit,
    onAdd: (ip: String, bssid: String?, ssid: String, subnetMask: String, label: String, isPrimary: Boolean) -> Unit
) {
    var ip by remember { mutableStateOf("192.168.1.1") }
    var bssid by remember { mutableStateOf("00:11:22:33:44:55") }
    var ssid by remember { mutableStateOf("Office_Mesh_Node1") }
    var subnetMask by remember { mutableStateOf("255.255.255.0") }
    var label by remember { mutableStateOf("AP Upstairs") }
    var isPrimary by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Trusted Gateway (Room)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("Gateway IP (Primary Key)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_trusted_gw_ip")
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. Main Router Lounge)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_trusted_gw_label")
                )
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("SSID / Network Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_trusted_gw_ssid")
                )
                OutlinedTextField(
                    value = bssid,
                    onValueChange = { bssid = it },
                    label = { Text("BSSID / MAC yechipangidzo") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_trusted_gw_bssid")
                )
                OutlinedTextField(
                    value = subnetMask,
                    onValueChange = { subnetMask = it },
                    label = { Text("Subnet Mask") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_trusted_gw_subnet")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set as Primary Baseline", color = TextSecondary, fontSize = 12.sp)
                    Switch(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it },
                        modifier = Modifier.testTag("switch_trusted_gw_is_primary"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(ip, bssid.ifBlank { null }, ssid, subnetMask, label, isPrimary)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBackground),
                modifier = Modifier.testTag("btn_confirm_add_trusted_gateway")
            ) {
                Text("Save to Room DB", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CyberSurfaceElevated
    )
}

@Composable
private fun ExportConfigDialog(
    jsonConfig: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Sentinel Configuration", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "This JSON configuration bundles your Room-persisted trusted gateways and whitelisted MAC identifiers.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberBackground),
                    border = BorderStroke(1.dp, CyberBorder)
                ) {
                    Text(
                        text = jsonConfig,
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
                if (isCopied) {
                    Text("✓ Configuration copied to clipboard!", color = CyberGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(jsonConfig))
                    isCopied = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBackground)
            ) {
                Text("Copy JSON", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = CyberSurfaceElevated
    )
}

@Composable
private fun ImportConfigDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyberGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Sentinel Configuration", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste a previously exported Sentinel JSON backup below to restore trusted gateways and MAC rules into Room DB:",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {
                        jsonText = it
                        errorMessage = null
                    },
                    placeholder = { Text("{\n  \"version\": \"Sentinel-4.2\",\n  \"trustedGateways\": [...] \n}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberGreen,
                        unfocusedBorderColor = CyberBorder
                    )
                )
                errorMessage?.let {
                    Text(it, color = CyberRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jsonText.isBlank()) {
                        errorMessage = "Please enter or paste valid JSON."
                    } else {
                        onImport(jsonText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberBackground)
            ) {
                Text("Restore Config", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CyberSurfaceElevated
    )
}
