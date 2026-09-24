package com.example.wifisentinel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wifisentinel.data.local.SentinelDeviceEntity

// Dark Mode Tactical SOC Color Palette
private val SocDarkBackground = Color(0xFF0D1117)
private val SocCardBackground = Color(0xFF161B22)
private val SocBorder = Color(0xFF30363D)
private val SocGreen = Color(0xFF238636)
private val SocRed = Color(0xFFDA3633)
private val SocCyan = Color(0xFF58A6FF)
private val SocPurple = Color(0xFFA371F7)
private val SocTextPrimary = Color(0xFFC9D1D9)
private val SocTextSecondary = Color(0xFF8B949E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentinelScreen(
    viewModel: SentinelViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SocDarkBackground)
            .padding(16.dp)
    ) {
        // SOC Tactical Header
        Surface(
            color = SocCardBackground,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SocBorder, RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "[GOIS-SIEM // NODES-PERGAMUS // BENEDICTUS]",
                    color = SocGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "WiFi Sentinel • Zero-Trust Perimeter & POTRAZ Guard",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
                Text(
                    text = uiState.complianceStatus,
                    color = SocPurple,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricBadge("TOTAL", "${uiState.devices.size}", SocCyan, Modifier.weight(1f))
            MetricBadge("SAFE", "${uiState.whitelistedDevices.size}", SocGreen, Modifier.weight(1f))
            MetricBadge("BLOCKED", "${uiState.blockedDevices.size}", SocRed, Modifier.weight(1f))
            MetricBadge("ALERTS", "${uiState.unacknowledgedAlertsCount}", SocPurple, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Autonomous Configuration & Quarantine Removal Control Card
        Surface(
            color = SocCardBackground,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (uiState.isAutonomousQuarantineRemovalActive) SocRed.copy(alpha = 0.5f) else SocBorder, RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AUTONOMOUS QUARANTINE REMOVAL",
                            color = if (uiState.isAutonomousQuarantineRemovalActive) SocRed else SocTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (uiState.isAutonomousQuarantineRemovalActive)
                                "Active: Auto-evicts & severs quarantined devices from network"
                            else
                                "Suspended: Quarantined devices remain in drop state only",
                            color = SocTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = uiState.isAutonomousQuarantineRemovalActive,
                        onCheckedChange = { viewModel.setAutonomousQuarantineRemoval(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SocRed,
                            uncheckedThumbColor = SocTextSecondary,
                            uncheckedTrackColor = SocCardBackground
                        )
                    )
                }

                if (uiState.blockedDevices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.removeQuarantinedDevicesFromNetwork() },
                        colors = ButtonDefaults.buttonColors(containerColor = SocRed),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REMOVE QUARANTINED FROM NETWORK (${uiState.blockedDevices.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        if (uiState.lastEvictionMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = SocRed.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SocRed.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = uiState.lastEvictionMessage,
                    color = SocRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Tabs Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterButton("ALL", uiState.selectedFilter == DeviceFilter.ALL) {
                viewModel.setFilter(DeviceFilter.ALL)
            }
            FilterButton("WHITELIST", uiState.selectedFilter == DeviceFilter.WHITELISTED) {
                viewModel.setFilter(DeviceFilter.WHITELISTED)
            }
            FilterButton("BLOCKED", uiState.selectedFilter == DeviceFilter.BLOCKED) {
                viewModel.setFilter(DeviceFilter.BLOCKED)
            }
            FilterButton("LAA-RANDOM", uiState.selectedFilter == DeviceFilter.RANDOMIZED_LAA) {
                viewModel.setFilter(DeviceFilter.RANDOMIZED_LAA)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Device Inventory List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.devices, key = { it.macAddress }) { device ->
                SentinelDeviceCard(
                    device = device,
                    onToggleAuth = { viewModel.toggleAuthorization(device) },
                    onToggleBlock = { viewModel.toggleBlock(device) },
                    onRemoveFromNetwork = { viewModel.removeQuarantinedDevice(device) }
                )
            }
        }
    }
}

@Composable
private fun MetricBadge(label: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = SocCardBackground,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.border(1.dp, SocBorder, RoundedCornerShape(6.dp))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = SocTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(text = count, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun FilterButton(title: String, selected: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) SocGreen else SocCardBackground,
            contentColor = if (selected) Color.White else SocTextSecondary
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = title, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SentinelDeviceCard(
    device: SentinelDeviceEntity,
    onToggleAuth: () -> Unit,
    onToggleBlock: () -> Unit,
    onRemoveFromNetwork: () -> Unit = {}
) {
    Surface(
        color = SocCardBackground,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when {
                    device.isBlocked -> SocRed
                    device.isAuthorized -> SocGreen
                    else -> SocBorder
                },
                RoundedCornerShape(8.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (device.customName.isNotEmpty()) device.customName else device.ipAddress,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${device.vendor} • ${device.macAddress}",
                        color = SocTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Status Tag
                when {
                    device.isBlocked -> StatusBadge("QUARANTINED", SocRed)
                    device.isAuthorized -> StatusBadge("WHITELISTED", SocGreen)
                    else -> StatusBadge("PROBATION", SocCyan)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Privacy & LAA Tags
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (device.isRandomizedMac) {
                    StatusBadge("LAA PRIVATE MAC", SocPurple)
                } else {
                    StatusBadge("BURNED-IN HW", SocCyan)
                }
                StatusBadge("POTRAZ SHA-256 SEALED", Color(0xFF2EA043))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (device.isBlocked) {
                    OutlinedButton(
                        onClick = onRemoveFromNetwork,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SocRed),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = SocRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Evict Net", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onToggleBlock,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (device.isBlocked) SocCyan else SocRed),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = if (device.isBlocked) "Unblock" else "Block / Drop", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onToggleAuth,
                    colors = ButtonDefaults.buttonColors(containerColor = if (device.isAuthorized) SocBorder else SocGreen),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = if (device.isAuthorized) "Revoke Trust" else "Whitelist", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
