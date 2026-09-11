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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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

    var selectedBandTab by remember { mutableStateOf(0) } // 0 = 2.4 GHz, 1 = 5 GHz
    val currentBandFilter = if (selectedBandTab == 0) "2.4 GHz" else "5 GHz"
    val filteredCongestion = congestionList.filter { it.band == currentBandFilter }

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
                    text = "Signal Overlap",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        items(nearbyAPs) { ap ->
            AccessPointItem(ap = ap, isCurrent = ap.bssid.equals(wifiState.bssid, ignoreCase = true))
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
private fun AccessPointItem(
    ap: NearbyAccessPoint,
    isCurrent: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) CyberCyan.copy(alpha = 0.1f) else CyberSurfaceVariant)
            .border(1.dp, if (isCurrent) CyberCyan else CyberBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
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
                        .background(CyberSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (isCurrent) CyberCyan else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = ap.ssid,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${ap.band} • CH ${ap.channel} • ${ap.bssid}",
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
                    text = if (isCurrent) "Connected" else "Nearby",
                    color = if (isCurrent) CyberCyan else TextMuted,
                    fontSize = 10.sp
                )
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
