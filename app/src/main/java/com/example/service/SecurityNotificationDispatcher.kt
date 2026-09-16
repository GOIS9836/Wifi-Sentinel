package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.DiscoveredDevice

/**
 * Local notification dispatcher for the Android security and sentinel suite.
 * Triggers high-priority local device alerts whenever a new or unauthorized
 * host is identified by the background scanner.
 */
object SecurityNotificationDispatcher {

    const val CHANNEL_ID = "sentinel_unknown_device_channel"
    const val CHANNEL_UNAUTHORIZED_ID = CHANNEL_ID
    private const val CHANNEL_NAME = "Unauthorized Device Intrusion Alerts"
    private const val CHANNEL_DESC = "Real-time alerts triggered when an unknown or unauthorized host is detected on the local Wi-Fi subnet."

    const val CHANNEL_DAILY_SCAN_ID = "sentinel_daily_scan_channel"
    private const val CHANNEL_DAILY_SCAN_NAME = "Daily Sentinel Scans"
    private const val CHANNEL_DAILY_SCAN_DESC = "Notifications and security reports for scheduled daily antivirus and storage cleaner scans."

    const val CHANNEL_FOREGROUND_SCANNER_ID = "sentinel_foreground_scanner_channel"
    private const val CHANNEL_FOREGROUND_SCANNER_NAME = "Network Scanner Shield Service"
    private const val CHANNEL_FOREGROUND_SCANNER_DESC = "Persistent foreground monitoring service for real-time unauthorized device detection."

    const val EXTRA_NAVIGATE_TO_SECURITY = "extra_navigate_security"
    const val EXTRA_NAVIGATE_TO_CLEANER = "extra_navigate_cleaner"
    const val EXTRA_INSPECT_MAC = "extra_inspect_mac"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val intrusionChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = 0xFFFF0055.toInt() // CyberRed
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250, 150, 400)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(intrusionChannel)

            val dailyScanChannel = NotificationChannel(CHANNEL_DAILY_SCAN_ID, CHANNEL_DAILY_SCAN_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DAILY_SCAN_DESC
                enableLights(true)
                lightColor = 0xFF00F0FF.toInt() // CyberCyan
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(dailyScanChannel)

            val foregroundChannel = NotificationChannel(CHANNEL_FOREGROUND_SCANNER_ID, CHANNEL_FOREGROUND_SCANNER_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                description = CHANNEL_FOREGROUND_SCANNER_DESC
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(foregroundChannel)
        }
    }

    fun areNotificationsAllowed(context: Context): Boolean {
        val managerCompat = NotificationManagerCompat.from(context)
        if (!managerCompat.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    /**
     * Local notification system that triggers an alert on the Android device whenever a new,
     * unauthorized device is identified on the network by the background scanner.
     */
    fun postUnauthorizedDeviceAlert(
        context: Context,
        device: DiscoveredDevice,
        threatReason: String = "Host is not recognized on known-device whitelist"
    ): Boolean {
        initNotificationChannel(context)

        if (!areNotificationsAllowed(context)) {
            return false
        }

        val notificationId = device.macAddress.uppercase().hashCode()

        // 1. Content Intent: opens app and directs user to the Security Intrusion inspector
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO_SECURITY, true)
            putExtra(EXTRA_INSPECT_MAC, device.macAddress)
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            pendingFlags
        )

        // 2. Action: Block Device immediately from the notification
        val blockPendingIntent = SecurityAlertActionReceiver.createBlockPendingIntent(
            context = context,
            mac = device.macAddress,
            ip = device.ip,
            notificationId = notificationId
        )

        // 3. Action: Whitelist Device immediately from the notification
        val whitelistPendingIntent = SecurityAlertActionReceiver.createWhitelistPendingIntent(
            context = context,
            mac = device.macAddress,
            ip = device.ip,
            vendor = device.vendor,
            notificationId = notificationId
        )

        val vendorLabel = if (device.vendor.isNotBlank() && device.vendor != "Unknown Vendor") {
            device.vendor
        } else {
            "Unidentified Host"
        }

        val title = "🚨 Unauthorized Device Alert: ${device.ip}"
        val shortContent = "$vendorLabel (${device.macAddress}) detected on Wi-Fi by background scanner."

        val bigText = StringBuilder().apply {
            append("⚠️ INTRUDER / UNAUTHORIZED HOST DETECTED\n")
            append("• IP Address: ${device.ip}\n")
            append("• MAC Address: ${device.macAddress}\n")
            append("• Vendor / Hardware: $vendorLabel\n")
            append("• Threat Vector: $threatReason\n")
            if (device.openPorts.isNotEmpty()) {
                append("• Exposed Ports: ${device.openPorts.joinToString(", ")}\n")
            }
            append("• Detection Source: Autonomous Background Scanner\n\n")
            append("Choose an immediate action below or tap to view packet telemetry.")
        }.toString()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFFF0055.toInt())
            .addAction(
                R.drawable.ic_notification_shield,
                "🚫 BLOCK DEVICE",
                blockPendingIntent
            )
            .addAction(
                R.drawable.ic_notification_shield,
                "✓ TRUST & WHITELIST",
                whitelistPendingIntent
            )
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Backward-compatible helper for posting unknown device notifications.
     */
    fun postUnknownDeviceNotification(context: Context, device: DiscoveredDevice): Boolean {
        return postUnauthorizedDeviceAlert(context, device)
    }

    /**
     * Dispatches an aggregate notification when multiple unauthorized devices are detected during background scanning.
     */
    fun postMultipleUnauthorizedDevicesAlert(
        context: Context,
        unauthorizedDevices: List<DiscoveredDevice>
    ): Boolean {
        if (unauthorizedDevices.isEmpty()) return false
        if (unauthorizedDevices.size == 1) {
            return postUnauthorizedDeviceAlert(context, unauthorizedDevices.first())
        }

        initNotificationChannel(context)

        if (!areNotificationsAllowed(context)) {
            return false
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO_SECURITY, true)
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            tapIntent,
            pendingFlags
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("🚨 ${unauthorizedDevices.size} Unauthorized Devices Detected")
            .setSummaryText("Background Subnet Scanner Alert")

        unauthorizedDevices.take(5).forEach { dev ->
            val v = if (dev.vendor.isNotBlank() && dev.vendor != "Unknown Vendor") dev.vendor else "Unknown"
            inboxStyle.addLine("${dev.ip} - $v (${dev.macAddress})")
        }
        if (unauthorizedDevices.size > 5) {
            inboxStyle.addLine("+ ${unauthorizedDevices.size - 5} more unauthorized hosts")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle("🚨 ${unauthorizedDevices.size} Unauthorized Devices on Subnet")
            .setContentText("Multiple unauthorized hosts detected on your local network by the background scanner.")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFFF0055.toInt())
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(1001, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Backward-compatible helper for multiple unknown devices.
     */
    fun postMultipleUnknownDevicesNotification(context: Context, unknownDevices: List<DiscoveredDevice>): Boolean {
        return postMultipleUnauthorizedDevicesAlert(context, unknownDevices)
    }

    /**
     * Dispatches a notification summarizing the automatic daily scheduled antivirus and cleaner scan results.
     */
    fun postDailyScanCompletedNotification(
        context: Context,
        scannedAppsCount: Int,
        threatsFound: Int,
        junkBytes: Long,
        autoCleaned: Boolean
    ): Boolean {
        initNotificationChannel(context)

        if (!areNotificationsAllowed(context)) {
            return false
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO_CLEANER, true)
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            2002,
            tapIntent,
            pendingFlags
        )

        val junkFormatted = com.example.service.JunkCleanerEngine.formatBytes(junkBytes)
        val hasThreats = threatsFound > 0

        val title = if (hasThreats) {
            "⚠️ Daily Scan: $threatsFound Potential Threats Identified"
        } else {
            "🛡️ Daily Sentinel Scan Complete: All Clean"
        }

        val shortContent = if (hasThreats) {
            "$scannedAppsCount packages audited. $threatsFound risk(s) flagged • $junkFormatted junk identified."
        } else {
            "$scannedAppsCount packages audited safely • $junkFormatted ${if (autoCleaned) "junk auto-cleaned" else "junk found"}."
        }

        val bigText = StringBuilder().apply {
            append("Automated Daily Device Scan Summary:\n")
            append("• Applications Audited: $scannedAppsCount installed packages\n")
            if (hasThreats) {
                append("• Threat Status: $threatsFound potential risk items detected!\n")
            } else {
                append("• Threat Status: All applications verified clean & safe\n")
            }
            append("• Storage & Cache: $junkFormatted ${if (autoCleaned) "automatically freed & optimized" else "available to clean"}\n")
            append("Tap to open Sentinel Cleaner & inspect device health.")
        }.toString()

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_SCAN_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(if (hasThreats) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(if (hasThreats) 0xFFFF0055.toInt() else 0xFF00F0FF.toInt())
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(2002, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
