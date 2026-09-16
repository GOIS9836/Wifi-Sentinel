package com.example.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.NetworkDeviceEntity
import com.example.data.local.SecurityAlertEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsible for handling interactive alert actions triggered
 * directly from unauthorized device notifications (e.g. Block Device, Trust & Whitelist).
 */
class SecurityAlertActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_BLOCK_DEVICE = "com.example.action.BLOCK_UNAUTHORIZED_DEVICE"
        const val ACTION_WHITELIST_DEVICE = "com.example.action.WHITELIST_DEVICE"
        const val ACTION_DISMISS_ALERT = "com.example.action.DISMISS_DEVICE_ALERT"

        const val EXTRA_MAC = "extra_device_mac"
        const val EXTRA_IP = "extra_device_ip"
        const val EXTRA_VENDOR = "extra_device_vendor"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        private const val TAG = "SecurityAlertAction"

        fun createBlockPendingIntent(
            context: Context,
            mac: String,
            ip: String,
            notificationId: Int
        ): PendingIntent {
            val intent = Intent(context, SecurityAlertActionReceiver::class.java).apply {
                action = ACTION_BLOCK_DEVICE
                putExtra(EXTRA_MAC, mac)
                putExtra(EXTRA_IP, ip)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, (mac + "_block").hashCode(), intent, flags)
        }

        fun createWhitelistPendingIntent(
            context: Context,
            mac: String,
            ip: String,
            vendor: String,
            notificationId: Int
        ): PendingIntent {
            val intent = Intent(context, SecurityAlertActionReceiver::class.java).apply {
                action = ACTION_WHITELIST_DEVICE
                putExtra(EXTRA_MAC, mac)
                putExtra(EXTRA_IP, ip)
                putExtra(EXTRA_VENDOR, vendor)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, (mac + "_whitelist").hashCode(), intent, flags)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val mac = intent.getStringExtra(EXTRA_MAC) ?: return
        val ip = intent.getStringExtra(EXTRA_IP) ?: "Unknown IP"
        val vendor = intent.getStringExtra(EXTRA_VENDOR) ?: "Unidentified Host"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, mac.hashCode())

        Log.d(TAG, "Handling notification action $action for device $ip ($mac)")
        val pendingResult = try { goAsync() } catch (e: Exception) { null }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                executeAction(context, action, mac, ip, vendor, notificationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing notification action: $action", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    suspend fun executeAction(
        context: Context,
        action: String,
        mac: String,
        ip: String,
        vendor: String,
        notificationId: Int
    ) {
        val db = AppDatabase.getInstance(context)
        when (action) {
            ACTION_BLOCK_DEVICE -> {
                val existing = db.networkDeviceDao().getDeviceByMac(mac)
                if (existing != null) {
                    db.networkDeviceDao().setBlocked(mac, true)
                } else {
                    db.networkDeviceDao().insertOrUpdate(
                        NetworkDeviceEntity(
                            macAddress = mac,
                            ipAddress = ip,
                            vendor = vendor,
                            isAuthorized = false,
                            isBlocked = true
                        )
                    )
                }

                // Record security quarantine action
                db.securityAlertDao().insert(
                    SecurityAlertEntity(
                        title = "🛡️ Unauthorized Host Quarantined via Alert",
                        description = "Host at $ip ($mac) was immediately blocked and isolated from the local network via notification action.",
                        deviceIp = ip,
                        deviceMac = mac,
                        severity = "CRITICAL",
                        confidencePercent = 100
                    )
                )

                // Update notification to confirm action taken
                showActionFeedbackNotification(
                    context = context,
                    notificationId = notificationId,
                    title = "🛡️ Unauthorized Device Blocked & Isolated",
                    message = "Network access dropped for $ip ($mac). Zero-Tolerance ACL active."
                )
            }

            ACTION_WHITELIST_DEVICE -> {
                val existing = db.networkDeviceDao().getDeviceByMac(mac)
                val updated = existing?.copy(isAuthorized = true, isBlocked = false)
                    ?: NetworkDeviceEntity(
                        macAddress = mac,
                        ipAddress = ip,
                        vendor = vendor,
                        isAuthorized = true,
                        isBlocked = false
                    )
                db.networkDeviceDao().insertOrUpdate(updated)

                // Update notification to confirm action taken
                showActionFeedbackNotification(
                    context = context,
                    notificationId = notificationId,
                    title = "✓ Device Whitelisted as Trusted",
                    message = "Host at $ip ($mac) added to trusted device baseline."
                )
            }

            ACTION_DISMISS_ALERT -> {
                NotificationManagerCompat.from(context).cancel(notificationId)
            }
        }
    }

    private fun showActionFeedbackNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SecurityNotificationDispatcher.EXTRA_NAVIGATE_TO_SECURITY, true)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId + 99, tapIntent, flags)

        val notification = NotificationCompat.Builder(context, SecurityNotificationDispatcher.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF00F0FF.toInt())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setTimeoutAfter(8000)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while showing action feedback notification", e)
        }
    }
}
