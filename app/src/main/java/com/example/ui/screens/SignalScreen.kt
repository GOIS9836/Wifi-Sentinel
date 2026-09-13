package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.WifiTethering
import com.example.data.local.SignalLogEntity
import com.example.ui.MainViewModel
import com.example.ui.components.IntruderAlertBanner
import com.example.ui.components.SignalGauge
import com.example.ui.components.SignalRssiChart
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
import com.example.util.TimeUtils
import com.example.util.rememberLiveCurrentTime

@Composable
fun SignalScreen(
    viewModel: MainViewModel,
    onNavigateToSecurity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wifiState by viewModel.wifiState.collectAsState()
    val rssiHistory by viewModel.rssiHistory.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val signalLogs by viewModel.signalLogs.collectAsState()
    val summaryReport by viewModel.networkSummaryReport.collectAsState()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()
    val liveNow by rememberLiveCurrentTime()
    val unauthorizedCount = discoveredDevices.count { !it.isAuthorized && !it.isSelf && !it.isGateway }

    var showWalkDialog by remember { mutableStateOf(false) }
    var locationInput by remember { mutableStateOf("Living Room") }
    var showRecordedSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Intruder Banner if threats exist
        if (unauthorizedCount > 0) {
            item {
                IntruderAlertBanner(
                    unauthorizedCount = unauthorizedCount,
                    onViewIntruders = onNavigateToSecurity
                )
            }
        }

        // Real-Time Signal & RF Telemetry Clock Banner
        item {
            RealtimeSignalClockBanner(
                liveNow = liveNow,
                linkSpeedMbps = wifiState.linkSpeedMbps,
                band = wifiState.band,
                frequencyMhz = wifiState.frequencyMhz
            )
        }

        // Live Signal Gauge
        item {
            SignalGauge(wifiState = wifiState)
        }

        // Rolling Live RSSI Timeline
        item {
            SignalRssiChart(history = rssiHistory)
        }

        // Spatial Walk Mode (Spot Signal Tester)
        item {
            WalkModeCard(
                onOpenWalkDialog = { showWalkDialog = true }
            )
        }

        // Recorded Spatial Walk Signals History
        if (signalLogs.isNotEmpty()) {
            item {
                SpatialSignalHistoryCard(
                    logs = signalLogs,
                    liveNow = liveNow,
                    onDeleteLog = { viewModel.deleteSignalLog(it) },
                    onClearAll = { viewModel.clearAllSignalLogs() }
                )
            }
        }

        // Signal Quality Assessment Breakdown
        item {
            SignalAssessmentCard(
                wifiState = wifiState,
                liveNow = liveNow
            )
        }

        // Network Health & Security Incident Summary Report (Clean Scrollable Card Format)
        item {
            val context = LocalContext.current
            NetworkSummaryReportCard(
                report = summaryReport,
                isGenerating = isGeneratingReport,
                onRefreshReport = { viewModel.generateSummaryReport() },
                onExportReport = { viewModel.exportSummaryReport(context) },
                onNavigateToWhitelist = onNavigateToSecurity
            )
        }
    }

    if (showWalkDialog) {
        AlertDialog(
            onDismissRequest = { showWalkDialog = false },
            containerColor = CyberSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Spatial Walk-Through Test",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Walk to a specific spot in your home or office. Record the live RSSI (-${-wifiState.rssi} dBm) to compare coverage across rooms.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val quickLocations = listOf("Living Room", "Office Desk", "Kitchen", "Bedroom", "Patio / Garden")
                    Text(
                        text = "Quick Presets:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickLocations.take(3).forEach { loc ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (locationInput == loc) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceElevated)
                                    .border(1.dp, if (locationInput == loc) CyberCyan else CyberBorder, RoundedCornerShape(8.dp))
                                    .clickable { locationInput = loc }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = loc,
                                    color = if (locationInput == loc) CyberCyan else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = locationInput,
                        onValueChange = { locationInput = it },
                        label = { Text("Custom Spot Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("walk_location_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recordLocationSignal(locationInput.ifBlank { "Unlabeled Spot" })
                        showWalkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier.testTag("save_signal_spot_button")
                ) {
                    Text(text = "Save Spot Sample", color = CyberSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWalkDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun WalkModeCard(
    onOpenWalkDialog: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenWalkDialog)
            .padding(18.dp)
            .testTag("walk_mode_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.dp, CyberCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Spatial Coverage Walk Mode",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sample and log signal quality spot-by-spot across rooms",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberCyan)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Record",
                    color = CyberSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SignalAssessmentCard(
    wifiState: com.example.data.model.WifiConnectionState,
    liveNow: Long = System.currentTimeMillis()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = null,
                    tint = CyberTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connection Quality Benchmark",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Live: ${TimeUtils.formatTime(liveNow)}",
                color = CyberCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        QualityRow(
            title = "Physical Layer Link Speed",
            value = "${wifiState.linkSpeedMbps} Mbps",
            status = if (wifiState.linkSpeedMbps >= 200) "Excellent" else "Adequate",
            isGood = wifiState.linkSpeedMbps >= 100
        )

        QualityRow(
            title = "RF Frequency Band",
            value = "${wifiState.band} (${wifiState.frequencyMhz} MHz)",
            status = if (wifiState.band == "5 GHz" || wifiState.band == "6 GHz") "Low Latency" else "Long Range",
            isGood = true
        )

        QualityRow(
            title = "Security Protocol",
            value = wifiState.securityProtocol,
            status = "Strong",
            isGood = true
        )

        QualityRow(
            title = "Channel Assignment",
            value = "Channel ${wifiState.channel}",
            status = if (wifiState.channel in listOf(1, 6, 11, 36, 40, 44, 48, 149)) "Optimal" else "Overlapping",
            isGood = wifiState.channel in listOf(1, 6, 11, 36, 40, 44, 48, 149)
        )
    }
}

@Composable
private fun QualityRow(
    title: String,
    value: String,
    status: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = TextSecondary, fontSize = 12.sp)
            Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isGood) CyberGreen.copy(alpha = 0.15f) else CyberBorder)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = status,
                color = if (isGood) CyberGreen else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RealtimeSignalClockBanner(
    liveNow: Long,
    linkSpeedMbps: Int,
    band: String,
    frequencyMhz: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("realtime_signal_clock_banner")
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
                        .background(CyberGreen)
                )
                Column {
                    Text(
                        text = "REAL-TIME RF TELEMETRY",
                        color = TextMuted,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${TimeUtils.formatTime(liveNow)} (Sync: <1s)",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$linkSpeedMbps Mbps",
                        color = CyberCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$band ($frequencyMhz MHz)",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SpatialSignalHistoryCard(
    logs: List<SignalLogEntity>,
    liveNow: Long,
    onDeleteLog: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("spatial_signal_history_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Spatial Spot Signals (${logs.size})",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time location sample history",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            TextButton(
                onClick = onClearAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Clear All",
                    color = CyberRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        logs.take(5).forEach { log ->
            SpatialLogItemRow(
                log = log,
                liveNow = liveNow,
                onDelete = { onDeleteLog(log.id) }
            )
        }
    }
}

@Composable
private fun SpatialLogItemRow(
    log: SignalLogEntity,
    liveNow: Long,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = log.locationName,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${log.rssiDbm} dBm",
                        color = if (log.rssiDbm >= -65) CyberGreen else if (log.rssiDbm >= -78) CyberAmber else CyberRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${log.speedMbps} Mbps • Ch ${log.channel}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• ${TimeUtils.formatRealtimeBadge(log.timestamp, liveNow)}",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Spot Sample",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
