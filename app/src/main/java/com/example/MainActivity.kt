package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.ChannelRadarScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.SignalScreen
import com.example.ui.theme.AlertRedGlow
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                SentinelApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SentinelApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val wifiState by viewModel.wifiState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val btDevices by viewModel.perimeterBtDevices.collectAsState()
    val activeGatewayAlert by viewModel.activeGatewayAlert.collectAsState()

    val unauthorizedHostCount = discoveredDevices.count { !it.isAuthorized && !it.isSelf && !it.isGateway }
    val btThreatCount = btDevices.count { it.isZeroToleranceFlagged && !it.isQuarantined }
    val totalThreatCount = unauthorizedHostCount + btThreatCount + (if (activeGatewayAlert != null) 1 else 0)

    // Runtime Permission Request for Wi-Fi and Bluetooth scanning
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshSubnetDevices()
        viewModel.refreshNearbyAPs()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberSurface,
        topBar = {
            SentinelTopBar(
                ssid = wifiState.ssid,
                unauthorizedCount = totalThreatCount
            )
        },
        bottomBar = {
            SentinelBottomNav(
                selectedTab = selectedTab,
                unauthorizedCount = totalThreatCount,
                onTabSelect = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> SignalScreen(
                    viewModel = viewModel,
                    onNavigateToSecurity = { selectedTab = 1 }
                )
                1 -> SecurityScreen(viewModel = viewModel)
                2 -> AiAdvisorScreen(viewModel = viewModel)
                3 -> ChannelRadarScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SentinelTopBar(
    ssid: String,
    unauthorizedCount: Int
) {
    val isThreat = unauthorizedCount > 0
    val statusColor = if (isThreat) CyberRed else CyberGreen

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
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
                                listOf(CyberCyan, CyberTeal)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "App Logo",
                        tint = CyberSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "WiFi Sentinel",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "AI",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = "Real-time Signal & Intruder Guard",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Live Network Health Status Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isThreat) AlertRedGlow else CyberSurfaceElevated)
                    .border(1.dp, if (isThreat) CyberRed else CyberBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isThreat) "INTRUDER" else "SECURE",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SentinelBottomNav(
    selectedTab: Int,
    unauthorizedCount: Int,
    onTabSelect: (Int) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(1.dp, CyberBorder.copy(alpha = 0.6f)),
        containerColor = CyberSurface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelect(0) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Signal Optimizer",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("Signal", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberSurface,
                selectedTextColor = CyberCyan,
                indicatorColor = CyberCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_signal_tab")
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelect(1) },
            icon = {
                BadgedBox(
                    badge = {
                        if (unauthorizedCount > 0) {
                            Badge(
                                containerColor = CyberRed,
                                contentColor = TextPrimary
                            ) {
                                Text("$unauthorizedCount")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Intruder Radar",
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            label = { Text("Security", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberSurface,
                selectedTextColor = if (unauthorizedCount > 0) CyberRed else CyberCyan,
                indicatorColor = if (unauthorizedCount > 0) CyberRed else CyberCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_security_tab")
        )

        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelect(2) },
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Advisor",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("AI Advisor", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberSurface,
                selectedTextColor = CyberCyan,
                indicatorColor = CyberCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_ai_tab")
        )

        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelect(3) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Channel Radar",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("Channels", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberSurface,
                selectedTextColor = CyberCyan,
                indicatorColor = CyberCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_channels_tab")
        )
    }
}
