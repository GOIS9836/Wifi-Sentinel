package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SentinelColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004E5B),
    onPrimaryContainer = Color(0xFFB5F4FF),

    secondary = CyberTeal,
    onSecondary = Color(0xFF00382E),
    secondaryContainer = Color(0xFF005144),
    onSecondaryContainer = Color(0xFF73FBD3),

    tertiary = CyberGreen,
    onTertiary = Color(0xFF00391E),
    tertiaryContainer = Color(0xFF00532E),
    onTertiaryContainer = Color(0xFF86FBB3),

    error = CyberRed,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = CyberBackground,
    onBackground = TextPrimary,

    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = CyberBorder,
    outlineVariant = Color(0xFF1F2E4A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep high-tech cyberpunk security theme uniform
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SentinelColorScheme,
        typography = Typography,
        content = content
    )
}
