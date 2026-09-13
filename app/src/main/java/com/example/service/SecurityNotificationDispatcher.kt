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

object SecurityNotificationDispatcher {

    const val CHANNEL_ID = "sentinel_unknown_device_channel"
    private const val CHANNEL_NAME = "Unknown Device Intrusion Alerts"
    private const val CHANNEL_DESC = "Real-time alerts triggered when an unknown or unauthorized host is detected on the local Wi-Fi subnet."

    const val EXTRA_NAVIGATE_TO_SECURITY = "extra_navigate_security"
    const val EXTRA_INSPECT_MAC = "extra_inspect_mac"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = 0xFFFF0055.toInt() // CyberRed
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250, 150, 400)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
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
     * Dispatches an Android system notification for an unknown device detected on the local network.
     */
    fun postUnknownDeviceNotification(context: Context, device: DiscoveredDevice): Boolean {
        initNotificationChannel(context)

        if (!areNotificationsAllowed(context)) {
            return false
        }

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
            device.macAddress.hashCode(),
            tapIntent,
            pendingFlags
        )

        val vendorLabel = if (device.vendor.isNotBlank() && device.vendor != "Unknown Vendor") device.vendor else "Unidentified Host"
        val title = "🚨 Unknown Device Detected: ${device.ip}"
        val shortContent = "$vendorLabel (${device.macAddress}) is not on your known-device whitelist."
        val bigText = StringBuilder().apply {
            append("An unrecognized device joined the local network:\n")
            append("• IP Address: ${device.ip}\n")
            append("• MAC Address: ${device.macAddress}\n")
            append("• Vendor: $vendorLabel\n")
            if (device.openPorts.isNotEmpty()) {
                append("• Open Ports: ${device.openPorts.joinToString(", ")}\n")
            }
            append("• Whitelist Status: NOT RECOGNIZED (Zero-Tolerance Alert)\n")
            append("Tap to inspect device details, block traffic, or add to whitelist.")
        }.toString()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFFF0055.toInt())
            .build()

        try {
            val notificationId = device.macAddress.hashCode()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    /**
     * Dispatches an aggregate notification when multiple unknown devices are detected during a single audit.
     */
    fun postMultipleUnknownDevicesNotification(context: Context, unknownDevices: List<DiscoveredDevice>): Boolean {
        if (unknownDevices.isEmpty()) return false
        if (unknownDevices.size == 1) {
            return postUnknownDeviceNotification(context, unknownDevices.first())
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
            .setBigContentTitle("🚨 ${unknownDevices.size} Unknown Devices Detected")
            .setSummaryText("Known-Device Whitelist Breach")

        unknownDevices.take(5).forEach { dev ->
            val v = if (dev.vendor.isNotBlank() && dev.vendor != "Unknown Vendor") dev.vendor else "Unknown"
            inboxStyle.addLine("${dev.ip} - $v (${dev.macAddress})")
        }
        if (unknownDevices.size > 5) {
            inboxStyle.addLine("+ ${unknownDevices.size - 5} more unauthorized hosts")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle("🚨 ${unknownDevices.size} Unknown Devices on Wi-Fi")
            .setContentText("Multiple unauthorized hosts detected not matching your known-device whitelist.")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFFF0055.toInt())
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
            return true
        } catch (e: SecurityException) {
            return false
        }
    }
}
