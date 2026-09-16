package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.data.model.AppRiskLevel
import com.example.data.model.DailyScanScheduleSettings
import com.example.data.model.JunkType
import java.util.Calendar
import java.util.Locale

object DailyScanScheduler {

    private const val TAG = "DailyScanScheduler"
    private const val PREFS_NAME = "sentinel_scan_schedule_prefs"

    private const val KEY_ENABLED = "key_schedule_enabled"
    private const val KEY_HOUR = "key_schedule_hour"
    private const val KEY_MINUTE = "key_schedule_minute"
    private const val KEY_SCAN_AV = "key_scan_antivirus"
    private const val KEY_SCAN_JUNK = "key_scan_junk"
    private const val KEY_AUTO_CLEAN_SAFE = "key_auto_clean_safe"
    private const val KEY_LAST_RUN_TIMESTAMP = "key_last_run_timestamp"
    private const val KEY_LAST_SCANNED_APPS = "key_last_scanned_apps"
    private const val KEY_LAST_RUN_THREATS = "key_last_run_threats"
    private const val KEY_LAST_RUN_JUNK_BYTES = "key_last_run_junk_bytes"
    private const val KEY_LAST_RUN_SUMMARY = "key_last_run_summary"

    private const val ALARM_REQUEST_CODE = 9009

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Loads saved daily scan settings from persistent SharedPreferences.
     */
    fun loadSettings(context: Context): DailyScanScheduleSettings {
        val prefs = getPrefs(context)
        return DailyScanScheduleSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            hour = prefs.getInt(KEY_HOUR, 2), // Default 02:00 AM
            minute = prefs.getInt(KEY_MINUTE, 0),
            scanAntivirus = prefs.getBoolean(KEY_SCAN_AV, true),
            scanJunkCleaner = prefs.getBoolean(KEY_SCAN_JUNK, true),
            autoCleanSafeJunk = prefs.getBoolean(KEY_AUTO_CLEAN_SAFE, true),
            lastRunTimestamp = prefs.getLong(KEY_LAST_RUN_TIMESTAMP, 0L),
            lastScannedAppsCount = prefs.getInt(KEY_LAST_SCANNED_APPS, 0),
            lastRunThreatsCount = prefs.getInt(KEY_LAST_RUN_THREATS, 0),
            lastRunJunkBytes = prefs.getLong(KEY_LAST_RUN_JUNK_BYTES, 0L),
            lastRunSummary = prefs.getString(KEY_LAST_RUN_SUMMARY, "") ?: ""
        )
    }

    /**
     * Persists daily scan settings to SharedPreferences.
     */
    fun saveSettings(context: Context, settings: DailyScanScheduleSettings) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.isEnabled)
            .putInt(KEY_HOUR, settings.hour)
            .putInt(KEY_MINUTE, settings.minute)
            .putBoolean(KEY_SCAN_AV, settings.scanAntivirus)
            .putBoolean(KEY_SCAN_JUNK, settings.scanJunkCleaner)
            .putBoolean(KEY_AUTO_CLEAN_SAFE, settings.autoCleanSafeJunk)
            .putLong(KEY_LAST_RUN_TIMESTAMP, settings.lastRunTimestamp)
            .putInt(KEY_LAST_SCANNED_APPS, settings.lastScannedAppsCount)
            .putInt(KEY_LAST_RUN_THREATS, settings.lastRunThreatsCount)
            .putLong(KEY_LAST_RUN_JUNK_BYTES, settings.lastRunJunkBytes)
            .putString(KEY_LAST_RUN_SUMMARY, settings.lastRunSummary)
            .apply()

        if (settings.isEnabled) {
            scheduleAlarm(context, settings)
        } else {
            cancelAlarm(context)
        }
    }

    /**
     * Calculates the millisecond timestamp for the next occurrence of the scheduled hour and minute.
     * If the time for today has already passed, schedules for tomorrow.
     */
    fun calculateNextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }

    /**
     * Configures the system AlarmManager to wake up and trigger the DailyScanAlarmReceiver.
     */
    fun scheduleAlarm(context: Context, settings: DailyScanScheduleSettings) {
        if (!settings.isEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMillis = calculateNextTriggerMillis(settings.hour, settings.minute)

        val intent = Intent(context, DailyScanAlarmReceiver::class.java).apply {
            action = DailyScanAlarmReceiver.ACTION_TRIGGER_DAILY_SCAN
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled daily scan alarm for $triggerMillis (${formatTime12Hour(settings.hour, settings.minute)})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback alarm schedule also failed", fallbackEx)
            }
        }
    }

    /**
     * Cancels any previously registered daily scan alarm.
     */
    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyScanAlarmReceiver::class.java).apply {
            action = DailyScanAlarmReceiver.ACTION_TRIGGER_DAILY_SCAN
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            flags
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled daily scan alarm")
        }
    }

    /**
     * Executes the automated scan routine (antivirus and/or cleaner), dispatches notification,
     * logs results, and re-schedules the next day's alarm.
     */
    suspend fun executeScheduledScan(context: Context): DailyScanScheduleSettings {
        val settings = loadSettings(context)
        var threatsFound = 0
        var totalScannedApps = 0
        var junkBytes = 0L
        var autoCleaned = false

        // 1. Antivirus Scan
        if (settings.scanAntivirus) {
            try {
                val scanned = AntivirusScannerEngine.scanAllAppsSync(context)
                totalScannedApps = scanned.size
                threatsFound = scanned.count {
                    it.riskLevel == AppRiskLevel.CRITICAL ||
                            it.riskLevel == AppRiskLevel.HIGH_RISK ||
                            it.riskLevel == AppRiskLevel.SUSPICIOUS
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error running scheduled antivirus scan", e)
            }
        }

        // 2. Junk Cleaner Scan & Optional Auto-Clean
        if (settings.scanJunkCleaner) {
            try {
                val categories = JunkCleanerEngine.calculateJunkCategories(context)
                junkBytes = categories.sumOf { it.sizeBytes }

                if (settings.autoCleanSafeJunk && junkBytes > 0) {
                    val safeCategories = setOf(JunkType.APP_CACHE, JunkType.TEMP_FILES, JunkType.LOG_BUFFERS)
                    val cleanResult = JunkCleanerEngine.executeClean(context, safeCategories)
                    autoCleaned = true
                    junkBytes = cleanResult.totalBytesCleaned
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error running scheduled junk clean", e)
            }
        }

        // 3. Post Notification
        SecurityNotificationDispatcher.postDailyScanCompletedNotification(
            context = context,
            scannedAppsCount = totalScannedApps,
            threatsFound = threatsFound,
            junkBytes = junkBytes,
            autoCleaned = autoCleaned
        )

        // 4. Update and persist settings with last run metadata
        val summaryText = buildString {
            if (settings.scanAntivirus) {
                append("$totalScannedApps apps audited ($threatsFound threats)")
            }
            if (settings.scanJunkCleaner) {
                if (isNotEmpty()) append(" • ")
                val formatted = JunkCleanerEngine.formatBytes(junkBytes)
                append(if (autoCleaned) "$formatted auto-purged" else "$formatted junk found")
            }
        }

        val updatedSettings = settings.copy(
            lastRunTimestamp = System.currentTimeMillis(),
            lastScannedAppsCount = totalScannedApps,
            lastRunThreatsCount = threatsFound,
            lastRunJunkBytes = junkBytes,
            lastRunSummary = summaryText
        )
        saveSettings(context, updatedSettings)

        // 5. Re-schedule alarm for the next day
        if (updatedSettings.isEnabled) {
            scheduleAlarm(context, updatedSettings)
        }

        return updatedSettings
    }

    /**
     * Formats 24-hour hour & minute into standard 12-hour format: "02:00 AM".
     */
    fun formatTime12Hour(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, amPm)
    }

    /**
     * Human-readable time remaining string until next execution.
     */
    fun formatTimeRemaining(nextTriggerMillis: Long): String {
        val diff = nextTriggerMillis - System.currentTimeMillis()
        if (diff <= 0) return "imminent"
        val totalMinutes = diff / (1000 * 60)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 -> "in ${hours}h ${mins}m"
            else -> "in ${mins}m"
        }
    }

    /**
     * Formats timestamp in milliseconds to human-readable date & time string.
     */
    fun formatTimestamp(millis: Long): String {
        if (millis <= 0L) return "Never"
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }
}
