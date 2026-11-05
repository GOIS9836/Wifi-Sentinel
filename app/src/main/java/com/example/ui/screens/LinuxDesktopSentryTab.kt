package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LinuxEndpointAlert
import com.example.data.model.LinuxEndpointNode
import com.example.data.model.LinuxHubServerState
import com.example.data.model.NodeClusterStatus
import com.example.service.LinuxSentryManager
import com.example.ui.theme.AlertRedGlow
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LinuxDesktopSentryTab(
    nodes: List<LinuxEndpointNode>,
    hubState: LinuxHubServerState,
    alerts: List<LinuxEndpointAlert>,
    onToggleHubServer: () -> Unit,
    onToggleIsolateNode: (String) -> Unit,
    onRemoveNode: (String) -> Unit,
    onAddManualNode: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    val activeThreatCount = nodes.sumOf { it.activeThreatsCount }
    val deployCmd = "curl -sSL http://${hubState.serverIp}:${hubState.port}/agent/install.sh | bash"
    val manualRunCmd = "python3 wifi_sentinel_desktop.py --hub ${hubState.serverIp} --gui"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card: Linux Desktop Defender & Telemetry Ingestion Hub
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(CyberSurfaceElevated, CyberSurface)
                        )
                    )
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DesktopWindows,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Linux Desktop Defender",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Apex Ingestion Hub (:8721)",
                                    color = CyberCyan,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (hubState.isRunning) CyberGreen.copy(alpha = 0.2f)
                                    else CyberRed.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (hubState.isRunning) "HUB ACTIVE" else "HUB OFFLINE",
                                color = if (hubState.isRunning) CyberGreen else CyberRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Real-time Wi-Fi perimeter defense, rogue listener socket audit, and remote iptables firewall isolation for connected Linux workstations, servers, and audit rigs.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinuxStatBadge(
                            label = "Linux Endpoints",
                            value = "${nodes.size} Hosts",
                            color = CyberCyan,
                            modifier = Modifier.weight(1f)
                        )
                        LinuxStatBadge(
                            label = "Security Posture",
                            value = if (activeThreatCount == 0) "BENEDICTUS" else "$activeThreatCount RISKS",
                            color = if (activeThreatCount == 0) CyberGreen else CyberRed,
                            modifier = Modifier.weight(1f)
                        )
                        LinuxStatBadge(
                            label = "Hub Payloads",
                            value = "${hubState.ingestedPayloadCount} Syncs",
                            color = CyberPurple,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hub Control Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onToggleHubServer,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hubState.isRunning) CyberRed.copy(alpha = 0.2f) else CyberGreen.copy(alpha = 0.2f),
                                contentColor = if (hubState.isRunning) CyberRed else CyberGreen
                            )
                        ) {
                            Icon(
                                imageVector = if (hubState.isRunning) Icons.Default.Stop else Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hubState.isRunning) "Stop Hub Server" else "Start Hub Server",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Node", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Deployment / Run Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Deploy to Linux Machine (One-Line Curl)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = deployCmd,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Deploy Command", deployCmd))
                                    Toast.makeText(context, "Copied Linux install command!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Or run standalone: $manualRunCmd",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Section Title: Active Linux Endpoints
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PROTECTED LINUX MACHINES (${nodes.size})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "POTRAZ Ch. 12:07 Compliant",
                    color = CyberPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Node Cards
        items(nodes, key = { it.nodeId }) { node ->
            LinuxNodeCard(
                node = node,
                onToggleIsolate = { onToggleIsolateNode(node.nodeId) },
                onRemove = { onRemoveNode(node.nodeId) }
            )
        }

        // Section Title: Recent Linux Security Alerts
        if (alerts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ENDPOINT SECURITY ALERTS (${alerts.size})",
                    color = AlertRedGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            items(alerts, key = { it.id }) { alert ->
                LinuxAlertCard(alert = alert)
            }
        }
    }

    if (showAddDialog) {
        AddLinuxNodeDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { h, ip, d ->
                onAddManualNode(h, ip, d)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun LinuxStatBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = TextMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LinuxNodeCard(
    node: LinuxEndpointNode,
    onToggleIsolate: () -> Unit,
    onRemove: () -> Unit
) {
    val isClean = node.activeThreatsCount == 0 && !node.isIsolated
    val borderColor = when {
        node.isIsolated -> CyberRed
        !isClean -> CyberYellow
        else -> CyberBorder
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("linux_node_${node.nodeId}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (node.isWifi) CyberCyan.copy(alpha = 0.2f) else CyberPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (node.isWifi) Icons.Default.Wifi else Icons.Default.Lan,
                            contentDescription = null,
                            tint = if (node.isWifi) CyberCyan else CyberPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = node.hostname,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${node.ipAddress} • ${node.activeIface}",
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                node.isIsolated -> CyberRed.copy(alpha = 0.2f)
                                !isClean -> CyberYellow.copy(alpha = 0.2f)
                                else -> CyberGreen.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when {
                            node.isIsolated -> "ISOLATED"
                            !isClean -> "${node.activeThreatsCount} THREAT"
                            else -> "BENEDICTUS"
                        },
                        color = when {
                            node.isIsolated -> CyberRed
                            !isClean -> CyberYellow
                            else -> CyberGreen
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Distribution & Kernel info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurface)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "🐧 ${node.distro}", color = TextSecondary, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurface)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "Kernel: ${node.kernelVersion}", color = TextMuted, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // RF / Network Telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (node.isWifi) "SSID: ${node.wifiSsid} (${node.wifiRssi} dBm)" else "Gigabit Wired Ethernet",
                    color = CyberTeal,
                    fontSize = 11.sp
                )
                Text(
                    text = "Seen: ${node.formattedLastSeen}",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            if (node.rogueListeners.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberRed.copy(alpha = 0.15f))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "🚨 Rogue Listener Alert:",
                            color = CyberRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        for (r in node.rogueListeners) {
                            Text(text = "• $r", color = AlertRedGlow, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions: Isolate / Lockdown / Remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleIsolate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (node.isIsolated) CyberGreen else CyberRed
                    )
                ) {
                    Icon(
                        imageVector = if (node.isIsolated) Icons.Default.CheckCircle else Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (node.isIsolated) "Un-Isolate Node" else "Lockdown / Isolate",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = CyberRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LinuxAlertCard(alert: LinuxEndpointAlert) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = CyberRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = alert.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = alert.hostname,
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = alert.detail,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun AddLinuxNodeDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var hostname by remember { mutableStateOf("Linux-Desktop-Ubuntu") }
    var ip by remember { mutableStateOf("192.168.1.150") }
    var distro by remember { mutableStateOf("Ubuntu 24.04 LTS") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurfaceElevated,
        title = {
            Text("Register Linux Machine Node", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = hostname,
                    onValueChange = { hostname = it },
                    label = { Text("Hostname") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )
                OutlinedTextField(
                    value = distro,
                    onValueChange = { distro = it },
                    label = { Text("Linux Distribution") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(hostname.trim(), ip.trim(), distro.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberSurface)
            ) {
                Text("Register Node", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
