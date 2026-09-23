package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AntivirusScannerState
import com.example.data.model.AppRiskLevel
import com.example.data.model.AppSecurityScanResult
import com.example.data.model.DailyScanScheduleSettings
import com.example.data.model.DeviceStorageMemoryMetrics
import com.example.data.model.JunkCategoryItem
import com.example.data.model.JunkCleanResult
import com.example.data.model.JunkType
import com.example.data.model.SystemSecurityAudit
import com.example.service.DailyScanScheduler
import com.example.service.JunkCleanerEngine
import com.example.service.SecurityNotificationDispatcher
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertRedGlow
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
import com.example.ui.theme.ShieldCyanGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AntivirusCleanerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Antivirus, 1: Cleaner, 2: System Audit

    val antivirusState by viewModel.antivirusState.collectAsState()
    val scannedApps by viewModel.scannedApps.collectAsState()
    val systemAudit by viewModel.systemSecurityAudit.collectAsState()
    val junkCategories by viewModel.junkCategories.collectAsState()
    val isCalculatingJunk by viewModel.isCalculatingJunk.collectAsState()
    val isCleaningJunk by viewModel.isCleaningJunk.collectAsState()
    val lastCleanResult by viewModel.lastCleanResult.collectAsState()
    val deviceMetrics by viewModel.deviceMetrics.collectAsState()
    val dailySchedule by viewModel.dailyScanSchedule.collectAsState()
    val isExecutingScheduledScan by viewModel.isExecutingScheduledScan.collectAsState()

    val totalThreats = scannedApps.count {
        it.riskLevel == AppRiskLevel.CRITICAL || it.riskLevel == AppRiskLevel.HIGH_RISK || it.riskLevel == AppRiskLevel.SUSPICIOUS
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Segmented Sub-Tab Switcher
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ANTIVIRUS & CLEANER",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Sentinel Device Shield",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Pulse Shield Status Chip
                    val isProtected = totalThreats == 0 && systemAudit.overallSecurityScore >= 80
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isProtected) CyberGreen.copy(alpha = 0.15f) else AlertRedGlow)
                            .border(1.dp, if (isProtected) CyberGreen else CyberRed, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isProtected) CyberGreen else CyberRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isProtected) "SHIELD ACTIVE" else "$totalThreats THREATS",
                                color = if (isProtected) CyberGreen else CyberRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sub-Tab Switcher (Antivirus / Cleaner / System Audit)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SubTabItem(
                        title = "Antivirus",
                        badge = if (totalThreats > 0) "$totalThreats" else null,
                        badgeColor = CyberRed,
                        icon = Icons.Default.Shield,
                        isSelected = activeSubTab == 0,
                        onClick = { activeSubTab = 0 },
                        modifier = Modifier.weight(1f).testTag("tab_antivirus")
                    )

                    val junkTotalBytes = junkCategories.filter { it.isSelected }.sumOf { it.sizeBytes }
                    SubTabItem(
                        title = "Cleaner",
                        badge = if (junkTotalBytes > 0) JunkCleanerEngine.formatBytes(junkTotalBytes).substringBefore(" ") else null,
                        badgeColor = CyberCyan,
                        icon = Icons.Default.CleaningServices,
                        isSelected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        modifier = Modifier.weight(1f).testTag("tab_cleaner")
                    )

                    SubTabItem(
                        title = "Schedule",
                        badge = if (dailySchedule.isEnabled) DailyScanScheduler.formatTime12Hour(dailySchedule.hour, dailySchedule.minute).substringBefore(" ") else null,
                        badgeColor = CyberGreen,
                        icon = Icons.Default.Alarm,
                        isSelected = activeSubTab == 2,
                        onClick = { activeSubTab = 2 },
                        modifier = Modifier.weight(1.05f).testTag("tab_schedule")
                    )

                    SubTabItem(
                        title = "Audit",
                        badge = if (systemAudit.activeIssuesCount > 0) "${systemAudit.activeIssuesCount}" else null,
                        badgeColor = CyberAmber,
                        icon = Icons.Default.Lock,
                        isSelected = activeSubTab == 3,
                        onClick = { activeSubTab = 3 },
                        modifier = Modifier.weight(0.95f).testTag("tab_audit")
                    )
                }
            }
        }

        // Sub Tab Content
        when (activeSubTab) {
            0 -> {
                // ==========================================
                // ANTIVIRUS SHIELD SUB-TAB
                // ==========================================
                item {
                    AntivirusRadarHeroCard(
                        state = antivirusState,
                        threatCount = totalThreats,
                        totalScanned = scannedApps.size,
                        onStartScan = { viewModel.startAntivirusScan() },
                        onCancelScan = { viewModel.cancelAntivirusScan() }
                    )
                }

                item {
                    DailyScanQuickBanner(
                        schedule = dailySchedule,
                        onOpenSchedule = { activeSubTab = 2 }
                    )
                }

                item {
                    AntivirusThreatSummaryRow(
                        scannedApps = scannedApps,
                        systemScore = systemAudit.overallSecurityScore
                    )
                }

                item {
                    ScannedAppsSection(
                        apps = scannedApps,
                        isScanning = antivirusState.isScanning,
                        onUninstall = { pkg -> viewModel.launchUninstallApp(context, pkg) },
                        onAppDetails = { pkg -> viewModel.launchAppSettings(context, pkg) },
                        onTrust = { pkg -> viewModel.whitelistPackage(pkg) },
                        onQuarantine = { pkg -> viewModel.quarantineApp(pkg) },
                        onUnquarantine = { pkg -> viewModel.unquarantineApp(pkg) }
                    )
                }
            }

            1 -> {
                // ==========================================
                // JUNK & STORAGE CLEANER SUB-TAB
                // ==========================================
                item {
                    StorageRamMetricsCard(
                        metrics = deviceMetrics,
                        onBoostRam = { viewModel.cleanSelectedJunk() }
                    )
                }

                item {
                    DailyScanQuickBanner(
                        schedule = dailySchedule,
                        onOpenSchedule = { activeSubTab = 2 }
                    )
                }

                item {
                    JunkCategoriesChecklistCard(
                        categories = junkCategories,
                        isCleaning = isCleaningJunk,
                        isCalculating = isCalculatingJunk,
                        lastCleanResult = lastCleanResult,
                        onToggleCategory = { viewModel.toggleJunkCategory(it) },
                        onSelectAll = { viewModel.selectAllJunk(it) },
                        onRefresh = { viewModel.refreshJunkStorage() },
                        onClean = { viewModel.cleanSelectedJunk() }
                    )
                }
            }

            2 -> {
                // ==========================================
                // DAILY AUTO-SCAN SCHEDULE SUB-TAB
                // ==========================================
                item {
                    DailyScanScheduleContent(
                        schedule = dailySchedule,
                        isExecuting = isExecutingScheduledScan,
                        onToggleEnabled = { viewModel.toggleDailyScanEnabled(it) },
                        onSetTime = { h, m -> viewModel.setDailyScanTime(h, m) },
                        onUpdateScope = { av, junk, autoClean -> viewModel.updateDailyScanScope(av, junk, autoClean) },
                        onRunNow = { viewModel.runScheduledScanNow() }
                    )
                }
            }

            3 -> {
                // ==========================================
                // SYSTEM AUDIT & HARDENING SUB-TAB
                // ==========================================
                item {
                    SystemAuditHeroCard(
                        audit = systemAudit,
                        onRefresh = { viewModel.refreshSystemAudit() }
                    )
                }

                item {
                    SystemHardeningChecksList(
                        audit = systemAudit,
                        onOpenSettings = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// SUB-TAB NAVIGATION ITEM
// ==========================================

@Composable
private fun SubTabItem(
    title: String,
    badge: String?,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) CyberSurfaceElevated else Color.Transparent
    val border = if (isSelected) CyberCyan else Color.Transparent
    val textColor = if (isSelected) CyberCyan else TextSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )

            if (badge != null) {
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// ANTIVIRUS RADAR HERO CARD
// ==========================================

@Composable
fun AntivirusRadarHeroCard(
    state: AntivirusScannerState,
    threatCount: Int,
    totalScanned: Int,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isScanning = state.isScanning

    val infiniteTransition = rememberInfiniteTransition(label = "radar_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        CyberSurfaceElevated,
                        CyberSurface
                    )
                )
            )
            .border(
                1.dp,
                if (isScanning) CyberCyan else if (threatCount > 0) CyberRed else CyberBorder,
                RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Radar / Shield Visual Center
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        if (isScanning) ShieldCyanGlow
                        else if (threatCount > 0) AlertRedGlow
                        else CyberGreen.copy(alpha = 0.12f)
                    )
                    .border(
                        2.dp,
                        if (isScanning) CyberCyan.copy(alpha = pulseGlow)
                        else if (threatCount > 0) CyberRed
                        else CyberGreen,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotation)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopCenter)
                                .clip(CircleShape)
                                .background(CyberCyan)
                        )
                    }
                }

                Icon(
                    imageVector = if (isScanning) Icons.Default.BugReport
                    else if (threatCount > 0) Icons.Default.Warning
                    else Icons.Default.Shield,
                    contentDescription = "Shield Status",
                    tint = if (isScanning) CyberCyan
                    else if (threatCount > 0) CyberRed
                    else CyberGreen,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Headline
            Text(
                text = if (isScanning) "SCANNING PACKAGES..."
                else if (threatCount > 0) "$threatCount POTENTIAL RISKS IDENTIFIED"
                else "DEVICE REPOSITORY CLEAN",
                color = if (isScanning) CyberCyan
                else if (threatCount > 0) CyberRed
                else CyberGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isScanning) {
                    if (state.currentlyScanningApp.isNotBlank()) "Auditing: ${state.currentlyScanningApp}" else "Inspecting application permissions & signatures..."
                } else if (threatCount > 0) {
                    "Review suspicious permissions & quarantine unverified apps below."
                } else {
                    "All $totalScanned installed apps verified safe. Real-time heuristics enabled."
                },
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Progress bar when scanning
            if (isScanning) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { state.scanProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CyberCyan,
                    trackColor = CyberSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(state.scanProgress * 100).toInt()}% Completed",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${state.scannedCount} / ${state.totalCount} packages",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Scan / Cancel Action Button
            if (isScanning) {
                OutlinedButton(
                    onClick = onCancelScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_cancel_scan"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Deep Scan", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onStartScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_start_scan"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = CyberBackground
                    )
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = "Deep Scan", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deep Scan Device Apps", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ==========================================
// THREAT SUMMARY PILLS ROW
// ==========================================

@Composable
fun AntivirusThreatSummaryRow(
    scannedApps: List<AppSecurityScanResult>,
    systemScore: Int
) {
    val criticalCount = scannedApps.count { it.riskLevel == AppRiskLevel.CRITICAL }
    val suspiciousCount = scannedApps.count { it.riskLevel == AppRiskLevel.HIGH_RISK || it.riskLevel == AppRiskLevel.SUSPICIOUS }
    val safeCount = scannedApps.count { it.riskLevel == AppRiskLevel.SAFE }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatMetricBox(
            label = "CRITICAL",
            value = "$criticalCount",
            color = if (criticalCount > 0) CyberRed else CyberGreen,
            modifier = Modifier.weight(1f)
        )
        StatMetricBox(
            label = "SUSPICIOUS",
            value = "$suspiciousCount",
            color = if (suspiciousCount > 0) CyberAmber else CyberCyan,
            modifier = Modifier.weight(1f)
        )
        StatMetricBox(
            label = "SAFE APPS",
            value = "$safeCount",
            color = CyberGreen,
            modifier = Modifier.weight(1f)
        )
        StatMetricBox(
            label = "SYSTEM",
            value = "$systemScore%",
            color = if (systemScore >= 80) CyberCyan else CyberAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatMetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = color,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==========================================
// SCANNED APPS LIST SECTION
// ==========================================

@Composable
fun ScannedAppsSection(
    apps: List<AppSecurityScanResult>,
    isScanning: Boolean,
    onUninstall: (String) -> Unit,
    onAppDetails: (String) -> Unit,
    onTrust: (String) -> Unit,
    onQuarantine: (String) -> Unit,
    onUnquarantine: (String) -> Unit
) {
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Threats, 2: Sideloaded, 3: Safe

    val filteredApps = remember(apps, selectedFilter) {
        when (selectedFilter) {
            1 -> apps.filter { it.riskLevel != AppRiskLevel.SAFE }
            2 -> apps.filter { it.installerSource.contains("Sideloaded") || it.installerSource.contains("Unknown") }
            3 -> apps.filter { it.riskLevel == AppRiskLevel.SAFE }
            else -> apps
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INSTALLED PACKAGES (${filteredApps.size})",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("All", "Risks", "APK").forEachIndexed { idx, label ->
                    val isSel = selectedFilter == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else CyberSurface)
                            .border(1.dp, if (isSel) CyberCyan else CyberBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedFilter = idx }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) CyberCyan else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Safe",
                        tint = CyberGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No applications match this filter.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredApps.forEach { app ->
                    AppSecurityCard(
                        app = app,
                        onUninstall = { onUninstall(app.packageName) },
                        onAppDetails = { onAppDetails(app.packageName) },
                        onTrust = { onTrust(app.packageName) },
                        onQuarantine = { onQuarantine(app.packageName) },
                        onUnquarantine = { onUnquarantine(app.packageName) }
                    )
                }
            }
        }
    }
}

// ==========================================
// INDIVIDUAL APP SECURITY CARD
// ==========================================

@Composable
fun AppSecurityCard(
    app: AppSecurityScanResult,
    onUninstall: () -> Unit,
    onAppDetails: () -> Unit,
    onTrust: () -> Unit,
    onQuarantine: () -> Unit,
    onUnquarantine: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val riskColor = when (app.riskLevel) {
        AppRiskLevel.CRITICAL -> CyberRed
        AppRiskLevel.HIGH_RISK -> CyberAmber
        AppRiskLevel.SUSPICIOUS -> CyberAmber.copy(alpha = 0.8f)
        AppRiskLevel.SAFE -> CyberGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSurface)
            .border(
                1.dp,
                if (app.isQuarantined) CyberRed else if (app.riskLevel != AppRiskLevel.SAFE) riskColor.copy(alpha = 0.5f) else CyberBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon Placeholder
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (app.isSystemApp) Icons.Default.Settings else Icons.Default.Android,
                        contentDescription = app.appName,
                        tint = if (app.riskLevel != AppRiskLevel.SAFE) riskColor else CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.appName,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Risk Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(riskColor.copy(alpha = 0.15f))
                                .border(1.dp, riskColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (app.isQuarantined) "QUARANTINED" else if (app.isWhitelisted) "TRUSTED" else app.riskLevel.label.uppercase(),
                                color = if (app.isQuarantined) CyberRed else if (app.isWhitelisted) CyberCyan else riskColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = app.packageName,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Source: ${app.installerSource}",
                            color = if (app.installerSource.contains("Play")) CyberCyan else CyberAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (app.appSizeBytes > 0) {
                            Text(
                                text = "•  ${JunkCleanerEngine.formatBytes(app.appSizeBytes)}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Expandable details (reasons, dangerous permissions, action buttons)
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CyberBorder.copy(alpha = 0.6f))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Risk Reasons
                    if (app.riskReasons.isNotEmpty()) {
                        Text(
                            text = "Security Findings:",
                            color = CyberAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        app.riskReasons.forEach { reason ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("⚠️ ", fontSize = 10.sp)
                                Text(
                                    text = reason,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Dangerous Permissions Chips
                    if (app.dangerousPermissions.isNotEmpty()) {
                        Text(
                            text = "Sensitive Permissions Requested:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            app.dangerousPermissions.forEach { perm ->
                                val shortName = perm.substringAfterLast(".")
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyberSurfaceElevated)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "• $shortName",
                                        color = CyberCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!app.isSystemApp) {
                            Button(
                                onClick = onUninstall,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberRed.copy(alpha = 0.2f),
                                    contentColor = CyberRed
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Uninstall", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Uninstall", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onAppDetails,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CyberBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Details", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("App Info", fontSize = 11.sp)
                        }

                        if (app.isQuarantined) {
                            Button(
                                onClick = onUnquarantine,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberGreen.copy(alpha = 0.2f),
                                    contentColor = CyberGreen
                                )
                            ) {
                                Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onQuarantine,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber)
                            ) {
                                Text("Quarantine", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// JUNK & RAM METRICS CARD
// ==========================================

@Composable
fun StorageRamMetricsCard(
    metrics: DeviceStorageMemoryMetrics,
    onBoostRam: () -> Unit
) {
    val totalRamMb = (metrics.totalRamBytes / (1024 * 1024)).coerceAtLeast(2048L)
    val availRamMb = (metrics.availableRamBytes / (1024 * 1024)).coerceAtLeast(512L)
    val usedRamMb = totalRamMb - availRamMb

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        CyberSurfaceElevated,
                        CyberSurface
                    )
                )
            )
            .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SYSTEM MEMORY & PERFORMANCE",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Device Health Telemetry",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.dp, CyberCyan, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${metrics.ramUsagePercent}% RAM",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RAM Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Active RAM Utilization", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        text = "$usedRamMb MB / $totalRamMb MB",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (metrics.ramUsagePercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (metrics.ramUsagePercent > 80) CyberRed else CyberCyan,
                    trackColor = CyberSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Available RAM", color = TextMuted, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$availRamMb MB Free",
                            color = CyberGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Storage Free", color = TextMuted, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = JunkCleanerEngine.formatBytes(metrics.freeStorageBytes),
                            color = CyberCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// JUNK CATEGORIES CHECKLIST CARD
// ==========================================

@Composable
fun JunkCategoriesChecklistCard(
    categories: List<JunkCategoryItem>,
    isCleaning: Boolean,
    isCalculating: Boolean,
    lastCleanResult: JunkCleanResult?,
    onToggleCategory: (JunkType) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onClean: () -> Unit
) {
    val totalSelectedBytes = categories.filter { it.isSelected }.sumOf { it.sizeBytes }
    val allSelected = categories.isNotEmpty() && categories.all { it.isSelected }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STORAGE & JUNK RECLAMATION",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Select Items to Purge",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Last clean celebration banner
            if (lastCleanResult != null && !isCleaning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberGreen.copy(alpha = 0.15f))
                        .border(1.dp, CyberGreen, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = CyberGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Reclaimed ${JunkCleanerEngine.formatBytes(lastCleanResult.totalBytesCleaned)} Storage!",
                                color = CyberGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (lastCleanResult.ramFreedMb > 0) {
                                Text(
                                    text = "RAM Heap Optimized (+${lastCleanResult.ramFreedMb} MB Available)",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Select All Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectAll(!allSelected) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = { onSelectAll(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = CyberCyan,
                        checkmarkColor = CyberBackground
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Select All Categories",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = JunkCleanerEngine.formatBytes(totalSelectedBytes),
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Items
            categories.forEach { category ->
                JunkCategoryRow(
                    category = category,
                    onToggle = { onToggleCategory(category.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Clean Button
            Button(
                onClick = onClean,
                enabled = totalSelectedBytes > 0 && !isCleaning && !isCalculating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_clean_junk"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = CyberBackground,
                    disabledContainerColor = CyberSurfaceElevated,
                    disabledContentColor = TextMuted
                )
            ) {
                if (isCleaning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = CyberBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Purging Junk Buffers...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = "Clean",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (totalSelectedBytes > 0) "Deep Clean ${JunkCleanerEngine.formatBytes(totalSelectedBytes)}" else "System Clean",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun JunkCategoryRow(
    category: JunkCategoryItem,
    onToggle: () -> Unit
) {
    val icon = when (category.id) {
        JunkType.APP_CACHE -> Icons.Default.Storage
        JunkType.TEMP_FILES -> Icons.Default.FolderZip
        JunkType.LOG_BUFFERS -> Icons.Default.Info
        JunkType.MEMORY_CACHE -> Icons.Default.Memory
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceElevated)
            .border(
                1.dp,
                if (category.isSelected) CyberCyan.copy(alpha = 0.5f) else CyberBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onToggle)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = category.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyberCyan,
                    checkmarkColor = CyberBackground
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.title,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = category.description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = JunkCleanerEngine.formatBytes(category.sizeBytes),
                color = if (category.isSelected) CyberCyan else TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==========================================
// SYSTEM AUDIT & HARDENING HERO CARD
// ==========================================

@Composable
fun SystemAuditHeroCard(
    audit: SystemSecurityAudit,
    onRefresh: () -> Unit
) {
    val scoreColor = when {
        audit.overallSecurityScore >= 85 -> CyberGreen
        audit.overallSecurityScore >= 65 -> CyberAmber
        else -> CyberRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        CyberSurfaceElevated,
                        CyberSurface
                    )
                )
            )
            .border(1.dp, scoreColor.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OS INTEGRITY & HARDENING",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "System Environment Audit",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Score Dial
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.12f))
                        .border(2.dp, scoreColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${audit.overallSecurityScore}",
                            color = scoreColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "/ 100",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (audit.overallSecurityScore >= 85) "HARDENED POSTURE" else "${audit.activeIssuesCount} Vulnerabilities Detected",
                        color = scoreColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (audit.overallSecurityScore >= 85) {
                            "Android SELinux sandboxing intact. No unauthorized root binaries found."
                        } else {
                            "Device debugging or permissions require hardening to mitigate threat vectors."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// SYSTEM HARDENING CHECKS LIST
// ==========================================

@Composable
fun SystemHardeningChecksList(
    audit: SystemSecurityAudit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "HARDENING AUDIT LEDGER",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        AuditCheckRow(
            title = "Root Binary Sentry",
            description = if (audit.isRootDetected) "Root / 'su' binaries detected on file system" else "No root binaries found (Sandbox intact)",
            isPassed = !audit.isRootDetected,
            passedText = "VERIFIED SECURE",
            failedText = "ROOTED (CRITICAL)"
        )

        AuditCheckRow(
            title = "USB Debugging (ADB)",
            description = if (audit.isUsbDebuggingEnabled) "ADB debugging is actively enabled over USB" else "USB debugging interface is disabled",
            isPassed = !audit.isUsbDebuggingEnabled,
            passedText = "LOCKED",
            failedText = "ADB ACTIVE"
        )

        AuditCheckRow(
            title = "Developer Options",
            description = if (audit.isDeveloperOptionsEnabled) "Android developer mode is toggled on" else "Developer mode is deactivated",
            isPassed = !audit.isDeveloperOptionsEnabled,
            passedText = "INACTIVE",
            failedText = "ENABLED"
        )

        AuditCheckRow(
            title = "Lock Screen Protection",
            description = if (audit.isLockScreenSecure) "PIN / Password / Biometric lock configured" else "Device lock screen is unsecured",
            isPassed = audit.isLockScreenSecure,
            passedText = "PROTECTED",
            failedText = "UNLOCKED"
        )

        AuditCheckRow(
            title = "Unknown Sources Verification",
            description = if (audit.isUnknownSourcesEnabled) "Sideloading unknown apps is allowed" else "External APK installs restricted",
            isPassed = !audit.isUnknownSourcesEnabled,
            passedText = "RESTRICTED",
            failedText = "PERMITTED"
        )

        AuditCheckRow(
            title = "Mobile Admin Apps Sentry",
            description = if (audit.unauthorizedDeviceAdmins.isEmpty()) "Zero unverified apps hold Device Admin persistence locks" else "${audit.unauthorizedDeviceAdmins.size} unauthorized app(s) hold Device Admin privileges",
            isPassed = audit.unauthorizedDeviceAdmins.isEmpty(),
            passedText = "ZERO ROGUE (SECURE)",
            failedText = "${audit.unauthorizedDeviceAdmins.size} ROGUE ADMIN"
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Open Security Settings shortcut button
        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberSurfaceElevated,
                contentColor = CyberCyan
            ),
            border = BorderStroke(1.dp, CyberBorder)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Android Security Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AuditCheckRow(
    title: String,
    description: String,
    isPassed: Boolean,
    passedText: String,
    failedText: String
) {
    val statusColor = if (isPassed) CyberGreen else CyberRed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, if (isPassed) CyberBorder else CyberRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = title,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isPassed) passedText else failedText,
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ==========================================
// DAILY AUTO-SCAN QUICK BANNER (For Tabs 0 & 1)
// ==========================================

@Composable
fun DailyScanQuickBanner(
    schedule: DailyScanScheduleSettings,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTime = DailyScanScheduler.formatTime12Hour(schedule.hour, schedule.minute)
    val nextMillis = DailyScanScheduler.calculateNextTriggerMillis(schedule.hour, schedule.minute)
    val countdown = DailyScanScheduler.formatTimeRemaining(nextMillis)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(
                1.dp,
                if (schedule.isEnabled) CyberCyan.copy(alpha = 0.5f) else CyberBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onOpenSchedule)
            .padding(12.dp)
            .testTag("banner_daily_scan_quick")
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (schedule.isEnabled) CyberCyan.copy(alpha = 0.15f) else CyberSurfaceVariant)
                        .border(1.dp, if (schedule.isEnabled) CyberCyan else CyberBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Schedule",
                        tint = if (schedule.isEnabled) CyberCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Daily Auto-Scan",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (schedule.isEnabled) CyberGreen.copy(alpha = 0.15f) else CyberSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (schedule.isEnabled) "ACTIVE ($formattedTime)" else "OFF",
                                color = if (schedule.isEnabled) CyberGreen else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (schedule.isEnabled) "Next trigger $countdown" else "Tap to configure automatic daily scans",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberCyan.copy(alpha = 0.12f))
                    .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "CONFIG",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ==========================================
// DAILY AUTO-SCAN SCHEDULE TAB CONTENT
// ==========================================

@Composable
fun DailyScanScheduleContent(
    schedule: DailyScanScheduleSettings,
    isExecuting: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onSetTime: (hour: Int, minute: Int) -> Unit,
    onUpdateScope: (scanAv: Boolean, scanJunk: Boolean, autoClean: Boolean) -> Unit,
    onRunNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTimeDialog by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(SecurityNotificationDispatcher.areNotificationsAllowed(context))
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val nextTriggerMillis = DailyScanScheduler.calculateNextTriggerMillis(schedule.hour, schedule.minute)
    val formattedTime = DailyScanScheduler.formatTime12Hour(schedule.hour, schedule.minute)
    val countdown = DailyScanScheduler.formatTimeRemaining(nextTriggerMillis)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Master Enable/Disable Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurface)
                .border(
                    1.dp,
                    if (schedule.isEnabled) CyberCyan else CyberBorder,
                    RoundedCornerShape(16.dp)
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (schedule.isEnabled) CyberCyan.copy(alpha = 0.15f) else CyberSurfaceVariant)
                                .border(1.5.dp, if (schedule.isEnabled) CyberCyan else CyberBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Schedule Master",
                                tint = if (schedule.isEnabled) CyberCyan else TextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "AUTOMATIC DAILY SCAN",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (schedule.isEnabled) "Schedule Active" else "Schedule Paused",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Switch(
                        checked = schedule.isEnabled,
                        onCheckedChange = { onToggleEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBackground,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberSurfaceVariant
                        ),
                        modifier = Modifier.testTag("switch_daily_schedule")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Runs autonomous daily security audits and junk storage analysis at your chosen time. Alarms persist across device restarts.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // 2. Scan Time Configuration Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SCHEDULED RUN TIME",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Daily Trigger Time",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { showTimeDialog = true },
                        border = BorderStroke(1.dp, CyberCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_custom_scan_time")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Set Time",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Custom Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big Digital Clock Readout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formattedTime,
                            color = if (schedule.isEnabled) CyberCyan else TextMuted,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (schedule.hour in 0..5) "Overnight Scan (Recommended — low device usage)"
                            else if (schedule.hour in 6..11) "Morning Routine"
                            else if (schedule.hour in 12..17) "Afternoon Sweep"
                            else "Evening Maintenance",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fast Preset Chips
                Text(
                    text = "Quick Presets:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                val presets = listOf(
                    Triple(2, 0, "02:00 AM (Overnight)"),
                    Triple(6, 0, "06:00 AM (Morning)"),
                    Triple(12, 0, "12:00 PM (Noon)"),
                    Triple(21, 0, "09:00 PM (Nightly)")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.take(2).forEach { (h, m, label) ->
                        val isCurrent = schedule.hour == h && schedule.minute == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) CyberCyan.copy(alpha = 0.15f) else CyberSurfaceElevated)
                                .border(1.dp, if (isCurrent) CyberCyan else CyberBorder, RoundedCornerShape(8.dp))
                                .clickable { onSetTime(h, m) }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isCurrent) CyberCyan else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.drop(2).forEach { (h, m, label) ->
                        val isCurrent = schedule.hour == h && schedule.minute == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) CyberCyan.copy(alpha = 0.15f) else CyberSurfaceElevated)
                                .border(1.dp, if (isCurrent) CyberCyan else CyberBorder, RoundedCornerShape(8.dp))
                                .clickable { onSetTime(h, m) }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isCurrent) CyberCyan else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 3. Scan Scope Configuration Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = "AUTOMATION SCAN SCOPE",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Included Daily Security Tasks",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Item 1: Antivirus
                DailyScanScopeRow(
                    title = "Deep Antivirus Malware Audit",
                    description = "Inspects installed apps, flags sideloaded APKs, and analyzes high-risk permissions.",
                    icon = Icons.Default.Shield,
                    iconColor = CyberCyan,
                    isChecked = schedule.scanAntivirus,
                    onCheckedChange = { checked ->
                        onUpdateScope(checked, schedule.scanJunkCleaner, schedule.autoCleanSafeJunk)
                    },
                    testTag = "checkbox_scope_antivirus"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Item 2: Junk Cleaner
                DailyScanScopeRow(
                    title = "Junk Storage & Cache Analysis",
                    description = "Evaluates application cache directories, obsolete APKs, and temporary system clutter.",
                    icon = Icons.Default.CleaningServices,
                    iconColor = CyberTeal,
                    isChecked = schedule.scanJunkCleaner,
                    onCheckedChange = { checked ->
                        onUpdateScope(schedule.scanAntivirus, checked, schedule.autoCleanSafeJunk)
                    },
                    testTag = "checkbox_scope_junk"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Item 3: Auto-Clean Safe Junk
                DailyScanScopeRow(
                    title = "Auto-Purge Safe Cache Clutter",
                    description = "Silently deletes temporary app cache and harmless residual files without requiring confirmation.",
                    icon = Icons.Default.Delete,
                    iconColor = CyberPurple,
                    isChecked = schedule.autoCleanSafeJunk,
                    enabled = schedule.scanJunkCleaner,
                    onCheckedChange = { checked ->
                        onUpdateScope(schedule.scanAntivirus, schedule.scanJunkCleaner, checked)
                    },
                    testTag = "checkbox_scope_autoclean"
                )
            }
        }

        // 4. Execution Telemetry & Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SCHEDULE TELEMETRY",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Autonomous Status",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (schedule.isEnabled) CyberGreen.copy(alpha = 0.15f) else CyberSurfaceVariant)
                            .border(1.dp, if (schedule.isEnabled) CyberGreen else CyberBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (schedule.isEnabled) "NEXT SCAN ARMED" else "SCHEDULE PAUSED",
                            color = if (schedule.isEnabled) CyberGreen else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Next Run Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceElevated)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = if (schedule.isEnabled) CyberCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (schedule.isEnabled) "Next Scheduled Trigger" else "Next Scan Paused",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (schedule.isEnabled)
                                "${DailyScanScheduler.formatTimestamp(nextTriggerMillis)} ($countdown)"
                            else
                                "Turn on the schedule toggle to arm automated scans.",
                            color = if (schedule.isEnabled) CyberCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Last Run Information
                if (schedule.lastRunTimestamp > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberSurfaceElevated)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last Completed Scan",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = DailyScanScheduler.formatTimestamp(schedule.lastRunTimestamp),
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Apps Scanned
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberCyan.copy(alpha = 0.1f))
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${schedule.lastScannedAppsCount} APPS",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Threats
                            val hasThreats = schedule.lastDetectedThreatsCount > 0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (hasThreats) AlertRedGlow else CyberGreen.copy(alpha = 0.1f))
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (hasThreats) "${schedule.lastDetectedThreatsCount} THREATS" else "0 THREATS",
                                    color = if (hasThreats) CyberRed else CyberGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Junk Purged
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberPurple.copy(alpha = 0.1f))
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${JunkCleanerEngine.formatBytes(schedule.lastCleanedBytes)} FREED",
                                    color = CyberPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (schedule.lastRunSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = schedule.lastRunSummary,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberSurfaceElevated)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "No scheduled scans recorded yet. You can wait for the alarm time or test run it below.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 5. Notification Warning (if permission missing)
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberAmber.copy(alpha = 0.1f))
                    .border(1.dp, CyberAmber, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notification Warning",
                        tint = CyberAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notifications Required",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Grant notification permission to receive scan completion reports and threat alerts.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_grant_notif_permission")
                    ) {
                        Text("Grant", color = CyberBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 6. Test Run Scheduled Routine Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = "VERIFY ROUTINE",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Immediate Test Execution",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Runs the exact automated daily scan pipeline right now, updates statistics, and delivers a completion notification.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onRunNow,
                    enabled = !isExecuting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_test_run_now"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        disabledContainerColor = CyberCyan.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            color = CyberBackground,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Executing Scheduled Routine...",
                            color = CyberBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test Run",
                            tint = CyberBackground,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test Run Scheduled Scan Now",
                            color = CyberBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    // Time Picker Dialog
    if (showTimeDialog) {
        CyberpunkTimePickerDialog(
            currentHour = schedule.hour,
            currentMinute = schedule.minute,
            onConfirm = { h, m ->
                onSetTime(h, m)
                showTimeDialog = false
            },
            onDismiss = { showTimeDialog = false }
        )
    }
}

// ==========================================
// SCOPE SELECTION ROW COMPONENT
// ==========================================

@Composable
private fun DailyScanScopeRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceElevated)
            .border(
                1.dp,
                if (isChecked && enabled) iconColor.copy(alpha = 0.4f) else CyberBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled) { onCheckedChange(!isChecked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = if (enabled) 0.15f else 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) iconColor else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) TextPrimary else TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = if (enabled) TextSecondary else TextMuted.copy(alpha = 0.6f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = iconColor,
                uncheckedColor = TextMuted,
                checkmarkColor = CyberBackground
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

// ==========================================
// CYBERPUNK TIME PICKER DIALOG
// ==========================================

@Composable
fun CyberpunkTimePickerDialog(
    currentHour: Int,
    currentMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Determine initial 12-hour values
    var selectedHour12 by remember {
        val h12 = when {
            currentHour == 0 -> 12
            currentHour > 12 -> currentHour - 12
            else -> currentHour
        }
        mutableIntStateOf(h12)
    }

    var selectedMinute by remember { mutableIntStateOf(currentMinute) }
    var isPm by remember { mutableStateOf(currentHour >= 12) }

    val formattedPreview = remember(selectedHour12, selectedMinute, isPm) {
        val hour24 = when {
            isPm && selectedHour12 < 12 -> selectedHour12 + 12
            !isPm && selectedHour12 == 12 -> 0
            else -> selectedHour12
        }
        DailyScanScheduler.formatTime12Hour(hour24, selectedMinute)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        titleContentColor = CyberCyan,
        textContentColor = TextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SELECT SCAN TIME",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formattedPreview,
                        color = CyberCyan,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AM / PM Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isPm) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceElevated)
                            .border(1.dp, if (!isPm) CyberCyan else CyberBorder, RoundedCornerShape(8.dp))
                            .clickable { isPm = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AM (Morning)",
                            color = if (!isPm) CyberCyan else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPm) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceElevated)
                            .border(1.dp, if (isPm) CyberCyan else CyberBorder, RoundedCornerShape(8.dp))
                            .clickable { isPm = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PM (Afternoon/Night)",
                            color = if (isPm) CyberCyan else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hour Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hour:",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                selectedHour12 = if (selectedHour12 == 1) 12 else selectedHour12 - 1
                            },
                            border = BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-", color = CyberCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = String.format("%02d", selectedHour12),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                selectedHour12 = if (selectedHour12 == 12) 1 else selectedHour12 + 1
                            },
                            border = BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", color = CyberCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Minute Stepper (by 5 mins)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Minute:",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                selectedMinute = if (selectedMinute <= 0) 55 else selectedMinute - 5
                            },
                            border = BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-", color = CyberCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = String.format("%02d", selectedMinute),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                selectedMinute = if (selectedMinute >= 55) 0 else selectedMinute + 5
                            },
                            border = BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", color = CyberCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Minute Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 15, 30, 45).forEach { min ->
                        val isSelected = selectedMinute == min
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceElevated)
                                .border(1.dp, if (isSelected) CyberCyan else CyberBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedMinute = min }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ":${String.format("%02d", min)}",
                                color = if (isSelected) CyberCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hour24 = when {
                        isPm && selectedHour12 < 12 -> selectedHour12 + 12
                        !isPm && selectedHour12 == 12 -> 0
                        else -> selectedHour12
                    }
                    onConfirm(hour24, selectedMinute)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_schedule_time")
            ) {
                Text("Confirm Time", color = CyberBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
