package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SignalRssiChart(
    history: List<Int>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Live Signal Fluctuation",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Past 35s • 1.5s interval",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurfaceElevated)
                .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                if (history.size < 2) {
                    // Placeholder line
                    drawLine(
                        color = Color(0xFF263553),
                        start = Offset(0f, h / 2),
                        end = Offset(w, h / 2),
                        strokeWidth = 2.dp.toPx()
                    )
                    return@Canvas
                }

                // Draw horizontal dBm guideline references
                // -40 dBm, -60 dBm, -80 dBm
                val minDbm = -90f
                val maxDbm = -30f
                val range = maxDbm - minDbm

                val guideLines = listOf(-40, -60, -80)
                for (guide in guideLines) {
                    val y = h - ((guide - minDbm) / range) * h
                    drawLine(
                        color = Color(0xFF1E2D4A),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Build line path
                val stepX = w / (history.size - 1)
                val path = Path()
                val fillPath = Path()

                fillPath.moveTo(0f, h)

                history.forEachIndexed { index, dbm ->
                    val clamped = dbm.coerceIn(minDbm.toInt(), maxDbm.toInt())
                    val normalizedY = (clamped - minDbm) / range
                    val x = index * stepX
                    val y = (h - (normalizedY * h)).coerceIn(0f, h)

                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(w, h)
                fillPath.close()

                // Draw translucent gradient under the curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = h
                    )
                )

                // Draw stroke line
                drawPath(
                    path = path,
                    color = CyberCyan,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw glowing pulse on the latest sample point
                val latestIndex = history.lastIndex
                val latestDbm = history.last().coerceIn(minDbm.toInt(), maxDbm.toInt())
                val latestX = latestIndex * stepX
                val latestY = (h - (((latestDbm - minDbm) / range) * h)).coerceIn(0f, h)

                drawCircle(
                    color = CyberTeal.copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = Offset(latestX, latestY)
                )
                drawCircle(
                    color = CyberCyan,
                    radius = 4.dp.toPx(),
                    center = Offset(latestX, latestY)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Low (-90 dBm)", color = TextMuted, fontSize = 10.sp)
            Text(text = "Optimal (-30 dBm)", color = TextMuted, fontSize = 10.sp)
        }
    }
}
