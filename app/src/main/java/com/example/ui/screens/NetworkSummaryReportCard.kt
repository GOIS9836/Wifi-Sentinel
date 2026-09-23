package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.NetworkHealthGrade
import com.example.data.model.NetworkSummaryReport
import com.example.data.model.ReportIncidentItem
import com.example.ui.theme.CyberAmber
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
import com.example.util.TimeUtils

@Composable
fun NetworkSummaryReportCard(
    report: NetworkSummaryReport,
    isGenerating: Boolean,
    onRefreshReport: () -> Unit,
    onExportReport: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToWhitelist: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showFullDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val gradeColor = when (report.healthGrade) {
        NetworkHealthGrade.EXCELLENT -> CyberGreen
        NetworkHealthGrade.GOOD -> CyberCyan
        NetworkHealthGrade.FAIR -> CyberAmber
        NetworkHealthGrade.CRITICAL -> CyberRed
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_network_summary_report")
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title, Tag, and Actions
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
                            .background(
                                Brush.linearGradient(
                                    listOf(CyberCyan.copy(alpha = 0.25f), CyberTeal.copy(alpha = 0.1f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Report Icon",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HEALTH & INCIDENT AUDIT",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(gradeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${report.overallHealthScore}/100",
                                    color = gradeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Generated ${TimeUtils.formatRelativeTime(report.generatedAt)}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${String.format(java.util.Locale.US, "%.2fs", report.scanRundownDurationMs / 1000f)}",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• TTL ${report.scoreValidityDurationSec / 60}m",
                                color = CyberGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefreshReport,
                        enabled = !isGenerating,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("btn_refresh_summary_report")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyberCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate Report",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onExportReport,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("btn_export_summary_report")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Report",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showFullDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("btn_expand_report_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Expand Full Report",
                            tint = CyberTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score Banner & Status Callout
            HealthScoreBanner(
                score = report.overallHealthScore,
                grade = report.healthGrade,
                gradeColor = gradeColor,
                ssid = report.ssid,
                incidentCount = report.incidents.totalCount,
                unknownCount = report.unknownDevicesCount,
                scanRundownDurationMs = report.scanRundownDurationMs,
                scoreValidityDurationSec = report.scoreValidityDurationSec
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4-Quadrant Quick Telemetry Grid
            TelemetryQuadrantGrid(report = report)

            Spacer(modifier = Modifier.height(12.dp))

            // Security Incidents Header & Counter Bar
            IncidentsCounterBar(
                incidents = report.incidents,
                whitelistedCount = report.whitelistedDevicesCount,
                unknownCount = report.unknownDevicesCount
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Incident Ledger inside the Card
            Text(
                text = "SECURITY INCIDENT LEDGER (${report.incidents.totalCount})",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable incident container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceVariant.copy(alpha = 0.6f))
                    .border(BorderStroke(1.dp, CyberBorder.copy(alpha = 0.6f)), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                if (report.recentIncidents.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Zero Security Incidents Logged",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No ARP spoofing or rogue intrusions detected",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        report.recentIncidents.forEach { incident ->
                            IncidentReportItemRow(incident = incident)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Collapsible Key Findings & Recommendations
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide Detailed Findings" else "Show Findings & Action Items (${report.keyFindings.size + report.actionableRecommendations.size})",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (report.keyFindings.isNotEmpty()) {
                        Text(
                            text = "KEY TELEMETRY FINDINGS",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        report.keyFindings.forEach { finding ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("• ", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(finding, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }

                    if (report.actionableRecommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ACTIONABLE RECOMMENDATIONS",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        report.actionableRecommendations.forEach { rec ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("▶ ", color = CyberAmber, fontSize = 11.sp)
                                Text(rec, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRefreshReport,
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_recalculate_report")
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = CyberSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGenerating) "Auditing..." else "Generate Report",
                        color = CyberSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onExportReport,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CyberBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CyberSurfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_share_report_summary")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Export / Share",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Expand Full Scrollable Dialog
    if (showFullDialog) {
        FullNetworkReportDialog(
            report = report,
            onDismiss = { showFullDialog = false },
            onExport = {
                showFullDialog = false
                onExportReport()
            }
        )
    }
}

@Composable
private fun HealthScoreBanner(
    score: Int,
    grade: NetworkHealthGrade,
    gradeColor: Color,
    ssid: String,
    incidentCount: Int,
    unknownCount: Int,
    scanRundownDurationMs: Long = 2100L,
    scoreValidityDurationSec: Long = 900L
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(gradeColor.copy(alpha = 0.12f), CyberSurfaceVariant)
                )
            )
            .border(BorderStroke(1.dp, gradeColor.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${grade.label.uppercase()} POSTURE",
                    color = gradeColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (unknownCount > 0 || incidentCount > 0) {
                        "$unknownCount unlisted host(s) • $incidentCount incident(s) on '$ssid'"
                    } else {
                        "Perimeter secure • Subnet operating at optimal health"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SCAN: ${String.format(java.util.Locale.US, "%.2fs", scanRundownDurationMs / 1000f)}",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TTL: ${scoreValidityDurationSec / 60}m (${scoreValidityDurationSec}s)",
                            color = CyberGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Circular Score Dial
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(CyberSurface)
                    .border(BorderStroke(2.dp, gradeColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        color = gradeColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SCORE",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryQuadrantGrid(report: NetworkSummaryReport) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportMiniMetricCard(
                icon = Icons.Default.Wifi,
                iconColor = CyberCyan,
                label = "RF SIGNAL & LINK",
                value = "${report.rssiDbm} dBm (${report.signalPercent}%)",
                subValue = "${report.linkSpeedMbps} Mbps • ${report.band}",
                modifier = Modifier.weight(1f)
            )

            ReportMiniMetricCard(
                icon = Icons.Default.Speed,
                iconColor = CyberTeal,
                label = "CHANNEL CONGESTION",
                value = "Ch ${report.channel}",
                subValue = report.channelCongestionLevel,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportMiniMetricCard(
                icon = Icons.Default.Security,
                iconColor = if (report.isGatewayVerified) CyberGreen else CyberRed,
                label = "GATEWAY INTEGRITY",
                value = report.gatewayIp.ifEmpty { "192.168.1.1" },
                subValue = if (report.isGatewayVerified) "Verified & Match" else "Unverified / Risk",
                modifier = Modifier.weight(1f)
            )

            ReportMiniMetricCard(
                icon = Icons.Default.VpnKey,
                iconColor = if (report.unknownDevicesCount == 0) CyberGreen else CyberAmber,
                label = "WHITELIST SENTRY",
                value = "${report.whitelistedDevicesCount} Whitelisted",
                subValue = "${report.unknownDevicesCount} Unknown Hosts",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReportMiniMetricCard(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    subValue: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceVariant)
            .border(BorderStroke(1.dp, CyberBorder.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subValue,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IncidentsCounterBar(
    incidents: com.example.data.model.IncidentTally,
    whitelistedCount: Int,
    unknownCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IncidentPillStat(label = "TOTAL INCIDENTS", count = incidents.totalCount, color = TextPrimary)
        IncidentPillStat(label = "CRITICAL", count = incidents.criticalCount, color = CyberRed)
        IncidentPillStat(label = "UNRESOLVED", count = incidents.unacknowledgedCount, color = CyberAmber)
        IncidentPillStat(label = "UNKNOWN HOSTS", count = unknownCount, color = if (unknownCount > 0) CyberRed else CyberGreen)
    }
}

@Composable
private fun IncidentPillStat(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IncidentReportItemRow(incident: ReportIncidentItem) {
    val severityColor = when (incident.severity.uppercase()) {
        "CRITICAL" -> CyberRed
        "HIGH" -> CyberAmber
        "MEDIUM", "WARNING" -> CyberCyan
        else -> TextSecondary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CyberSurface)
            .border(BorderStroke(1.dp, CyberBorder.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
            .padding(8.dp)
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
                            .clip(RoundedCornerShape(3.dp))
                            .background(severityColor.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = incident.severity.uppercase(),
                            color = severityColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = incident.title,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Text(
                    text = TimeUtils.formatRelativeTime(incident.timestamp),
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = incident.description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )

            if (incident.deviceIp.isNotEmpty() || incident.deviceMac.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${incident.deviceIp} • ${incident.deviceMac}",
                    color = CyberCyan.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun FullNetworkReportDialog(
    report: NetworkSummaryReport,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(16.dp)),
            color = CyberSurface,
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "COMPLETE AUDIT REPORT",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Generated ${TimeUtils.formatDateTime(report.generatedAt)} • Scan: ${String.format(java.util.Locale.US, "%.2fs", report.scanRundownDurationMs / 1000f)} • TTL: ${report.scoreValidityDurationSec / 60}m",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HealthScoreBanner(
                        score = report.overallHealthScore,
                        grade = report.healthGrade,
                        gradeColor = when (report.healthGrade) {
                            NetworkHealthGrade.EXCELLENT -> CyberGreen
                            NetworkHealthGrade.GOOD -> CyberCyan
                            NetworkHealthGrade.FAIR -> CyberAmber
                            NetworkHealthGrade.CRITICAL -> CyberRed
                        },
                        ssid = report.ssid,
                        incidentCount = report.incidents.totalCount,
                        unknownCount = report.unknownDevicesCount,
                        scanRundownDurationMs = report.scanRundownDurationMs,
                        scoreValidityDurationSec = report.scoreValidityDurationSec
                    )

                    TelemetryQuadrantGrid(report = report)

                    IncidentsCounterBar(
                        incidents = report.incidents,
                        whitelistedCount = report.whitelistedDevicesCount,
                        unknownCount = report.unknownDevicesCount
                    )

                    // Key Findings
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "TELEMETRY FINDINGS",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            report.keyFindings.forEach { finding ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("• ", color = CyberCyan, fontWeight = FontWeight.Bold)
                                    Text(finding, color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Actionable Recommendations
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "RECOMMENDED MITIGATIONS",
                                color = CyberAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            report.actionableRecommendations.forEach { rec ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("▶ ", color = CyberAmber)
                                    Text(rec, color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Security Incidents
                    Text(
                        text = "RECORDED INCIDENTS (${report.recentIncidents.size})",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (report.recentIncidents.isEmpty()) {
                        Text(
                            text = "No recorded security incidents on this network.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    } else {
                        report.recentIncidents.forEach { incident ->
                            IncidentReportItemRow(incident = incident)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        Text("Close", color = TextPrimary)
                    }

                    Button(
                        onClick = onExport,
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = CyberSurface, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Full Report", color = CyberSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
