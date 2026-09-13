package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for formatting real-time timestamps and calculating live relative time.
 */
object TimeUtils {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun formatTime(millis: Long): String {
        if (millis <= 0) return "N/A"
        return synchronized(timeFormat) {
            timeFormat.format(Date(millis))
        }
    }

    fun formatDateTime(millis: Long): String {
        if (millis <= 0) return "N/A"
        return synchronized(dateTimeFormat) {
            dateTimeFormat.format(Date(millis))
        }
    }

    fun formatFullDateTime(millis: Long): String {
        if (millis <= 0) return "N/A"
        return synchronized(isoFormat) {
            isoFormat.format(Date(millis))
        }
    }

    fun formatRelativeTime(millis: Long, now: Long = System.currentTimeMillis()): String {
        if (millis <= 0) return "Never"
        val diffMs = now - millis
        if (diffMs < 0) return "Just now"
        val diffSeconds = diffMs / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return when {
            diffSeconds < 3 -> "Just now"
            diffSeconds < 60 -> "${diffSeconds}s ago"
            diffMinutes < 60 -> "${diffMinutes}m ${diffSeconds % 60}s ago"
            diffHours < 24 -> "${diffHours}h ${diffMinutes % 60}m ago"
            diffDays == 1L -> "Yesterday ${formatTime(millis)}"
            else -> "${diffDays}d ago (${formatDateTime(millis)})"
        }
    }

    fun formatRealtimeBadge(millis: Long, now: Long = System.currentTimeMillis()): String {
        if (millis <= 0) return "N/A"
        val timeStr = formatTime(millis)
        val relStr = formatRelativeTime(millis, now)
        return "$timeStr ($relStr)"
    }
}

/**
 * A Composable state that updates every 1 second, providing a live real-time ticking clock
 * for dynamic relative timestamp re-evaluations across all screens.
 */
@Composable
fun rememberLiveCurrentTime(updateIntervalMs: Long = 1000L): State<Long> {
    val currentTime = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(updateIntervalMs) {
        while (isActive) {
            currentTime.longValue = System.currentTimeMillis()
            delay(updateIntervalMs)
        }
    }
    return currentTime
}
