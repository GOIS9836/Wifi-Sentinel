package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SignalGrade
import com.example.data.model.WifiConnectionState
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SignalGauge(
    wifiState: WifiConnectionState,
    modifier: Modifier = Modifier
) {
    val animatedPercent by animateFloatAsState(
        targetValue = wifiState.signalPercent / 100f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "signal_gauge_arc"
    )

    val gaugeColor = when {
        wifiState.rssi >= -50 -> CyberGreen
        wifiState.rssi >= -65 -> CyberCyan
        wifiState.rssi >= -75 -> CyberAmber
        else -> CyberRed
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // SSID and Band Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wi-Fi Icon",
                        tint = gaugeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(
                        text = wifiState.ssid,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${wifiState.securityProtocol} • ${wifiState.bssid}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = wifiState.band,
                    color = CyberTeal,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Circular Gauge Canvas
        Box(
            modifier = Modifier
                .size(220.dp)
                .testTag("signal_gauge_circle"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val strokeWidth = 16.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)
                val startAngle = 140f
                val sweepTotal = 260f

                // Track Arc (Background)
                drawArc(
                    color = Color(0xFF162137),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active Gradient Arc
                val currentSweep = sweepTotal * animatedPercent
                if (currentSweep > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to CyberRed,
                            0.35f to CyberAmber,
                            0.7f to CyberCyan,
                            1.0f to CyberGreen
                        ),
                        startAngle = startAngle,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Minor tick marks
                val tickCount = 13
                for (i in 0 until tickCount) {
                    val angleDeg = startAngle + (sweepTotal / (tickCount - 1)) * i
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val innerR = radius - 16.dp.toPx()
                    val outerR = radius - 24.dp.toPx()
                    val start = Offset(
                        (centerOffset.x + innerR * cos(angleRad)).toFloat(),
                        (centerOffset.y + innerR * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        (centerOffset.x + outerR * cos(angleRad)).toFloat(),
                        (centerOffset.y + outerR * sin(angleRad)).toFloat()
                    )
                    drawCircle(
                        color = if (i <= tickCount * animatedPercent) gaugeColor.copy(alpha = 0.8f) else Color(0xFF263553),
                        radius = 2.dp.toPx(),
                        center = end
                    )
                }
            }

            // Central Metric Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${wifiState.rssi}",
                    color = TextPrimary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "dBm",
                    color = gaugeColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(gaugeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${wifiState.signalGrade.label} (${wifiState.signalPercent}%)",
                        color = gaugeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Three Telemetry Badges (Channel, Link Speed, Frequency)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryTile(
                icon = Icons.Default.Tune,
                title = "Channel",
                value = "CH ${wifiState.channel}",
                subtitle = "${wifiState.frequencyMhz} MHz",
                accentColor = CyberCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                icon = Icons.Default.Speed,
                title = "Link Speed",
                value = "${wifiState.linkSpeedMbps}",
                subtitle = "Mbps PHY",
                accentColor = CyberTeal,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                icon = Icons.Default.Wifi,
                title = "IP Gateway",
                value = wifiState.gatewayIp.substringAfterLast("."),
                subtitle = wifiState.gatewayIp,
                accentColor = CyberGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TelemetryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
