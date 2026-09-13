package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NetworkDeviceEntity
import com.example.data.model.DeviceWhitelistAuditResult
import com.example.data.model.DiscoveredDevice
import com.example.data.model.WhitelistAuditStatus
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTeal
import com.example.util.TimeUtils

@Composable
fun KnownDeviceWhitelistCard(
    auditResult: DeviceWhitelistAuditResult,
    whitelistedEntities: List<NetworkDeviceEntity>,
    isAutoNotifyEnabled: Boolean,
    liveNow: Long,
    onRunAudit: () -> Unit,
    onToggleAutoNotify: (Boolean) -> Unit,
    onSimulateUnknownIntruder: () -> Unit,
    onWhitelistDevice: (DiscoveredDevice) -> Unit,
    onRemoveFromWhitelist: (String) -> Unit,
    onBlockDevice: (DiscoveredDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    var showWhitelistDialog by remember { mutableStateOf(false) }

    val statusBorderColor = when (auditResult.status) {
        WhitelistAuditStatus.ALERT_TRIGGERED -> CyberRed
        WhitelistAuditStatus.UNKNOWN_DETECTED -> CyberAmber
        WhitelistAuditStatus.ALL_WHITELISTED -> CyberCyan.copy(alpha = 0.5f)
    }

    val statusBgColor = when (auditResult.status) {
        WhitelistAuditStatus.ALERT_TRIGGERED -> CyberRed.copy(alpha = 0.08f)
        WhitelistAuditStatus.UNKNOWN_DETECTED -> CyberAmber.copy(alpha = 0.06f)
        WhitelistAuditStatus.ALL_WHITELISTED -> CyberGreen.copy(alpha = 0.04f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, statusBorderColor, RoundedCornerShape(16.dp))
            .testTag("known_device_whitelist_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title and Live Status Badge
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
                            .clip(CircleShape)
                            .background(
                                when (auditResult.status) {
                                    WhitelistAuditStatus.ALERT_TRIGGERED -> CyberRed.copy(alpha = 0.2f)
                                    WhitelistAuditStatus.UNKNOWN_DETECTED -> CyberAmber.copy(alpha = 0.2f)
                                    WhitelistAuditStatus.ALL_WHITELISTED -> CyberCyan.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (auditResult.status) {
                                WhitelistAuditStatus.ALERT_TRIGGERED -> Icons.Default.Warning
                                WhitelistAuditStatus.UNKNOWN_DETECTED -> Icons.Default.Shield
                                WhitelistAuditStatus.ALL_WHITELISTED -> Icons.Default.CheckCircle
                            },
                            contentDescription = "Whitelist Sentry Icon",
                            tint = when (auditResult.status) {
                                WhitelistAuditStatus.ALERT_TRIGGERED -> CyberRed
                                WhitelistAuditStatus.UNKNOWN_DETECTED -> CyberAmber
                                WhitelistAuditStatus.ALL_WHITELISTED -> CyberTeal
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "WHITELIST & INTRUSION SENTRY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Automated Connected Host vs Whitelist Verification",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Logic Flow Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusBgColor)
                    .border(1.dp, statusBorderColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (auditResult.status) {
                                        WhitelistAuditStatus.ALERT_TRIGGERED -> CyberRed
                                        WhitelistAuditStatus.UNKNOWN_DETECTED -> CyberAmber
                                        WhitelistAuditStatus.ALL_WHITELISTED -> CyberGreen
                                    }
                                )
                        )
                        Column {
                            Text(
                                text = when (auditResult.status) {
                                    WhitelistAuditStatus.ALERT_TRIGGERED -> "🚨 INTRUSION ALERT: UNKNOWN HOST DETECTED"
                                    WhitelistAuditStatus.UNKNOWN_DETECTED -> "⚠️ UNKNOWN HOST CONNECTED (UNLISTED)"
                                    WhitelistAuditStatus.ALL_WHITELISTED -> "🛡️ ALL CONNECTED HOSTS WHITELISTED"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = when (auditResult.status) {
                                    WhitelistAuditStatus.ALERT_TRIGGERED -> CyberRed
                                    WhitelistAuditStatus.UNKNOWN_DETECTED -> CyberAmber
                                    WhitelistAuditStatus.ALL_WHITELISTED -> CyberGreen
                                }
                            )
                            Text(
                                text = when (auditResult.status) {
                                    WhitelistAuditStatus.ALERT_TRIGGERED -> "System notification triggered to alert network owner immediately."
                                    WhitelistAuditStatus.UNKNOWN_DETECTED -> "${auditResult.unknownCount} host(s) do not match the trusted whitelist baseline."
                                    WhitelistAuditStatus.ALL_WHITELISTED -> "Zero unknown devices. Subnet strictly matches known whitelist."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = TimeUtils.formatRelativeTime(auditResult.auditTimestamp, liveNow),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4-Quadrant Statistics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuditStatItem(
                    title = "Connected",
                    value = "${auditResult.totalConnected}",
                    color = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                AuditStatItem(
                    title = "Whitelisted",
                    value = "${auditResult.whitelistedCount}",
                    color = CyberGreen,
                    modifier = Modifier.weight(1f)
                )
                AuditStatItem(
                    title = "Unknowns",
                    value = "${auditResult.unknownCount}",
                    color = if (auditResult.unknownCount > 0) CyberRed else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
                AuditStatItem(
                    title = "Alerts Sent",
                    value = "${auditResult.notificationsDispatched}",
                    color = if (auditResult.notificationsDispatched > 0) CyberAmber else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Auto-Notification Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceVariant.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isAutoNotifyEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = "Notification Icon",
                        tint = if (isAutoNotifyEnabled) CyberCyan else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Auto-Notify on Unknown Device",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isAutoNotifyEnabled) "High-priority push alert fired when unlisted device connects" else "Notifications muted (in-app display only)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isAutoNotifyEnabled,
                    onCheckedChange = onToggleAutoNotify,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("auto_notify_switch")
                )
            }

            // Unknown Devices List (if any present)
            if (auditResult.unknownDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "🚨 UNKNOWN HOSTS DETECTED (${auditResult.unknownDevices.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberRed,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    auditResult.unknownDevices.forEach { dev ->
                        UnknownDeviceRow(
                            device = dev,
                            onWhitelist = { onWhitelistDevice(dev) },
                            onBlock = { onBlockDevice(dev) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Audit Now, Whitelist DB, and Intruder Test Simulation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRunAudit,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_audit_subnet_now")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Audit Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showWhitelistDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CyberSurfaceVariant),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("btn_manage_whitelist")
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Whitelist (${whitelistedEntities.size})", color = Color.White, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onSimulateUnknownIntruder,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberRed.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CyberRed.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_simulate_unknown_device")
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CyberRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Alert", color = CyberRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }

    if (showWhitelistDialog) {
        WhitelistManagementDialog(
            whitelistedEntities = whitelistedEntities,
            onRemove = { onRemoveFromWhitelist(it) },
            onDismiss = { showWhitelistDialog = false }
        )
    }
}

@Composable
private fun AuditStatItem(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun UnknownDeviceRow(
    device: DiscoveredDevice,
    onWhitelist: () -> Unit,
    onBlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("unknown_device_row_${device.macAddress.replace(":", "")}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = device.ip,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberRed,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberRed.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("UNLISTED", color = CyberRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Text(
                    text = "${device.vendor} • ${device.macAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onWhitelist,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trust", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onBlock,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberRed),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = CyberRed, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun WhitelistManagementDialog(
    whitelistedEntities: List<NetworkDeviceEntity>,
    onRemove: (String) -> Unit,
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = CyberGreen)
                    Text(
                        text = "Known-Device Whitelist",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                Text(
                    text = "Hosts in this database are recognized as legitimate infrastructure or trusted user devices. Any host not matching this list automatically triggers intrusion alerts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (whitelistedEntities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No whitelisted devices registered yet.", color = Color.White.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(whitelistedEntities, key = { it.macAddress }) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberSurfaceVariant)
                                    .border(1.dp, CyberBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.customName.ifBlank { item.vendor.ifBlank { "Known Host" } },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberGreen
                                        )
                                        Text(
                                            text = "${item.ipAddress} • ${item.macAddress}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    IconButton(
                                        onClick = { onRemove(item.macAddress) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove from Whitelist",
                                            tint = CyberRed.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
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
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
