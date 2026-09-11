package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberTeal

@Composable
fun WavePulseRadar(
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_2"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isScanning) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.minDimension / 2

                // Ripple 1
                val r1 = maxRadius * pulse1
                val a1 = (1f - pulse1).coerceIn(0f, 1f) * 0.6f
                drawCircle(
                    color = CyberCyan.copy(alpha = a1),
                    radius = r1,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Ripple 2
                val r2 = maxRadius * pulse2
                val a2 = (1f - pulse2).coerceIn(0f, 1f) * 0.6f
                drawCircle(
                    color = CyberTeal.copy(alpha = a2),
                    radius = r2,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
        content()
    }
}
