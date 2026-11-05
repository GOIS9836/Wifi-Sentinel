package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SignalLogEntity
import com.example.data.model.ChannelCongestion
import com.example.data.model.NearbyAccessPoint
import com.example.ui.MainViewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChannelRadarScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val congestionList by viewModel.channelCongestionList.collectAsState()
    val nearbyAPs by viewModel.nearbyAccessPoints.collectAsState()
    val signalLogs by viewModel.signalLogs.collectAsState()
    val wifiState by viewModel.wifiState.collectAsState()
    val isStrictNearbyDistrust by viewModel.isStrictNearbyDistrustActive.collectAsState()
    val blockedNearbyBssids by viewModel.blockedNearbyBssids.collectAsState()
    val trustedNearbyBssids by viewModel.trustedNearbyBssids.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val btDevices by viewModel.perimeterBtDevices.collectAsState()

    var selectedBandTab by remember { mutableStateOf(0) } // 0 = 2.4 GHz, 1 = 5 GHz
    val currentBandFilter = if (selectedBandTab == 0) "2.4 GHz" else "5 GHz"
    val filteredCongestion = congestionList.filter { it.band == currentBandFilter }

    val blockedLanPeersCount = discoveredDevices.count { it.isBlocked }
    val quarantinedBtCount = btDevices.count { it.isQuarantined }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Channel Spectrum Card
        item {
            ChannelSpectrumCard(
                selectedTab = selectedBandTab,
                onTabSelect = { selectedBandTab = it },
                channels = filteredCongestion,
                currentChannel = wifiState.channel,
                onRefresh = { viewModel.refreshNearbyAPs() }
            )
        }

        // Zero-Trust Nearby AP & Device Isolation Card
        item {
            ZeroTrustNearbyPerimeterCard(
                isStrictDistrustActive = isStrictNearbyDistrust,
                isolatedApCount = blockedNearbyBssids.size,
                blockedLanCount = blockedLanPeersCount,
                quarantinedBleCount = quarantinedBtCount,
                onToggleStrictDistrust = { viewModel.toggleStrictNearbyDistrust(it) },
                onLockdownAllNearby = { viewModel.enforceStrictNearbyDistrustLockdown() }
            )
        }

        // Nearby Access Points Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Access Points (${nearbyAPs.size})",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isStrictNearbyDistrust) "Zero-Trust RF Isolation Active" else "Signal Overlap",
                    color = if (isStrictNearbyDistrust) CyberRed else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (isStrictNearbyDistrust) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        items(nearbyAPs) { ap ->
            val normBssid = ap.bssid.uppercase().trim()
            val isCurrent = normBssid.equals(wifiState.bssid.trim(), ignoreCase = true)
            val isTrusted = isCurrent || trustedNearbyBssids.contains(normBssid)
            val isBlocked = !isCurrent && (blockedNearbyBssids.contains(normBssid) || (isStrictNearbyDistrust && !isTrusted))
            AccessPointItem(
                ap = ap,
                isCurrent = isCurrent,
                isTrusted = isTrusted,
                isBlocked = isBlocked,
                currentChannel = wifiState.channel,
                onToggleBlock = { viewModel.toggleBlockNearbyAp(ap) },
                onToggleTrust = { viewModel.toggleTrustNearbyAp(ap) }
            )
        }

        // Saved Spatial Walk Signal Log History
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Coverage Spot Logs (${signalLogs.size})",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (signalLogs.isNotEmpty()) {
                    Text(
                        text = "Clear All",
                        color = CyberRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.clearAllSignalLogs() }
                    )
                }
            }
        }

        if (signalLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceVariant)
                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No spot samples saved yet. Use 'Spatial Walk Mode' on the Signal screen to log room coverage!",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(signalLogs) { log ->
                SignalLogItem(log = log, onDelete = { viewModel.deleteSignalLog(log.id) })
            }
        }
    }
}

@Composable
private fun ChannelSpectrumCard(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    channels: List<ChannelCongestion>,
    currentChannel: Int,
    onRefresh: () -> Unit
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
            Column {
                Text(
                    text = "RF Channel Interference Radar",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Co-channel congestion & free spectrum",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh APs",
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Band Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CyberSurfaceElevated,
            contentColor = CyberCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberCyan
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelect(0) },
                text = { Text("2.4 GHz Band", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelect(1) },
                text = { Text("5 GHz Band", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Channel spectrum bars
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            channels.forEach { ch ->
                val barProgress by animateFloatAsState(
                    targetValue = (ch.apCount * 0.25f).coerceIn(0.05f, 1f),
                    label = "bar_progress"
                )

                val barColor = when {
                    ch.apCount == 0 -> CyberGreen
                    ch.apCount <= 2 -> CyberCyan
                    ch.apCount <= 4 -> CyberAmber
                    else -> CyberRed
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(55.dp)) {
                        Text(
                            text = "CH ${ch.channel}",
                            color = if (ch.isCurrentChannel) CyberCyan else TextPrimary,
                            fontWeight = if (ch.isCurrentChannel) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberSurfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(barProgress)
                                .clip(RoundedCornerShape(6.dp))
                                .background(barColor)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.CenterEnd) {
                        if (ch.isCurrentChannel) {
                            Text(
                                text = "CURRENT",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else if (ch.isRecommended) {
                            Text(
                                text = "CLEAN (0 AP)",
                                color = CyberGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "${ch.apCount} APs",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZeroTrustNearbyPerimeterCard(
    isStrictDistrustActive: Boolean,
    isolatedApCount: Int,
    blockedLanCount: Int,
    quarantinedBleCount: Int,
    onToggleStrictDistrust: (Boolean) -> Unit,
    onLockdownAllNearby: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isStrictDistrustActive) Color(0xFF1B0C14) else CyberSurfaceVariant)
            .border(
                1.dp,
                if (isStrictDistrustActive) CyberRed.copy(alpha = 0.75f) else CyberBorder,
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
            .testTag("zero_trust_nearby_perimeter_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isStrictDistrustActive) CyberRed.copy(alpha = 0.2f) else CyberSurfaceElevated)
                        .border(1.dp, if (isStrictDistrustActive) CyberRed else CyberBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isStrictDistrustActive) CyberRed else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Zero-Trust Nearby Device Isolation",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isStrictDistrustActive) {
                            "All unwhitelisted nearby APs, LAN peers & BLE devices distrusted"
                        } else {
                            "Standard perimeter monitoring active"
                        },
                        color = if (isStrictDistrustActive) CyberRed else TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = isStrictDistrustActive,
                onCheckedChange = onToggleStrictDistrust,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyberRed,
                    checkedTrackColor = CyberRed.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = CyberSurfaceElevated
                ),
                modifier = Modifier.testTag("strict_nearby_distrust_switch")
            )
        }

        // Perimeter Isolation Counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$isolatedApCount",
                        color = CyberRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "AP BSSIDs Isolated",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$blockedLanCount",
                        color = CyberAmber,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "LAN Hosts Blocked",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$quarantinedBleCount",
                        color = CyberCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "BLE Quarantined",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Button(
            onClick = onLockdownAllNearby,
            colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("lockdown_all_nearby_button")
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Isolate & Block All Nearby Untrusted Devices",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AccessPointItem(
    ap: NearbyAccessPoint,
    isCurrent: Boolean,
    isTrusted: Boolean = false,
    isBlocked: Boolean = false,
    currentChannel: Int = 0,
    onToggleBlock: () -> Unit = {},
    onToggleTrust: () -> Unit = {}
) {
    val isSameChannel = !isCurrent && ap.channel == currentChannel && currentChannel > 0
    val isOpenOrWeak = ap.security.contains("Open", ignoreCase = true) || ap.security.contains("WEP", ignoreCase = true)

    val cardBg = when {
        isCurrent -> CyberCyan.copy(alpha = 0.1f)
        isBlocked -> Color(0xFF220B12)
        isTrusted -> CyberGreen.copy(alpha = 0.08f)
        else -> CyberSurfaceVariant
    }
    val cardBorder = when {
        isCurrent -> CyberCyan
        isBlocked -> CyberRed.copy(alpha = 0.8f)
        isTrusted -> CyberGreen.copy(alpha = 0.7f)
        else -> CyberBorder
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> CyberCyan.copy(alpha = 0.2f)
                                    isBlocked -> CyberRed.copy(alpha = 0.25f)
                                    isTrusted -> CyberGreen.copy(alpha = 0.2f)
                                    else -> CyberSurfaceElevated
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBlocked) Icons.Default.Block else if (isTrusted) Icons.Default.CheckCircle else Icons.Default.Wifi,
                            contentDescription = null,
                            tint = when {
                                isCurrent -> CyberCyan
                                isBlocked -> CyberRed
                                isTrusted -> CyberGreen
                                else -> TextSecondary
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = ap.ssid,
                            color = if (isBlocked) CyberRed else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${ap.band} • CH ${ap.channel} • ${ap.bssid} • ${ap.security}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${ap.rssi} dBm",
                        color = if (ap.rssi >= -60) CyberGreen else if (ap.rssi >= -75) CyberAmber else CyberRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isCurrent -> "CONNECTED AP"
                            isBlocked -> "UNTRUSTED • BLOCKED"
                            isTrusted -> "TRUSTED BSSID"
                            else -> "NEARBY AP"
                        },
                        color = when {
                            isCurrent -> CyberCyan
                            isBlocked -> CyberRed
                            isTrusted -> CyberGreen
                            else -> TextMuted
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (!isCurrent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isSameChannel) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberAmber.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CH $currentChannel OVERLAP",
                                    color = CyberAmber,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        if (isOpenOrWeak) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberRed.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "UNENCRYPTED / WEAK RF",
                                    color = CyberRed,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onToggleBlock,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isBlocked) CyberAmber else CyberRed
                            ),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = if (isBlocked) "Unblock BSSID" else "Isolate BSSID",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onToggleTrust,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isTrusted) TextMuted else CyberTeal
                            ),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = if (isTrusted) "Untrust" else "Whitelist AP",
                                fontSize = 10.sp,
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
private fun SignalLogItem(
    log: SignalLogEntity,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(log.timestamp))
    val logColor = when {
        log.rssiDbm >= -55 -> CyberGreen
        log.rssiDbm >= -70 -> CyberCyan
        log.rssiDbm >= -80 -> CyberAmber
        else -> CyberRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
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
                        .background(logColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = logColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = log.locationName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$dateStr • CH ${log.channel} • ${log.speedMbps} Mbps",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${log.rssiDbm} dBm",
                    color = logColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
