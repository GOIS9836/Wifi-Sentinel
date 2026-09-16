package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityAlertEntity
import com.example.data.model.DiscoveredDevice
import com.example.data.model.ThreatLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Persistent foreground service for the network scanner.
 * Continuously monitors the local Wi-Fi subnet and uses Android's NotificationManager
 * to dispatch instantaneous push alerts whenever an unauthorized device is detected.
 *
 * Implements strict idempotency:
 * - Repeated service start invocations are idempotent and maintain a single scanning loop.
 * - Repeated detections of the same unauthorized host do NOT produce redundant notification spam.
 * - State and alert tracking remain consistent across repeated scan intervals.
 */
class NetworkScannerForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scannerJob: Job? = null
    private var wifiManager: WiFiManager? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NetworkScannerForegroundService created")
        SecurityNotificationDispatcher.initNotificationChannel(this)
        wifiManager = WiFiManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SCANNER

        when (action) {
            ACTION_STOP_SCANNER -> {
                stopScanner()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RESET_IDEMPOTENCY -> {
                resetIdempotencyCache()
            }
            ACTION_TRIGGER_IMMEDIATE_SCAN -> {
                serviceScope.launch {
                    performSubnetAudit(isManualTrigger = true)
                }
            }
            ACTION_START_SCANNER -> {
                startForegroundWithNotification()
                startPeriodicScanning()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "NetworkScannerForegroundService onDestroy")
        stopScanner()
        serviceScope.cancel()
        _isServiceRunning.value = false
        super.onDestroy()
    }

    private fun startForegroundWithNotification() {
        val notification = buildPersistentForegroundNotification(
            statusText = "Autonomous Subnet Monitor & Unauthorized Intrusion Shield Active",
            devicesScanned = _lastScannedDeviceCount.value
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } catch (e: Exception) {
                Log.w(TAG, "Falling back to standard startForeground: ${e.message}")
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isServiceRunning.value = true
    }

    /**
     * Idempotent periodic scanning loop.
     * If already running, subsequent calls do not launch duplicate coroutines.
     */
    @Synchronized
    private fun startPeriodicScanning() {
        if (scannerJob?.isActive == true) {
            Log.d(TAG, "Scanning loop already active; startPeriodicScanning is idempotent")
            return
        }

        scannerJob = serviceScope.launch {
            Log.i(TAG, "Starting autonomous subnet monitoring loop")
            while (isActive) {
                try {
                    performSubnetAudit(isManualTrigger = false)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in subnet scan loop: ${e.message}", e)
                }
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    private fun stopScanner() {
        scannerJob?.cancel()
        scannerJob = null
        _isServiceRunning.value = false
        Log.i(TAG, "Subnet scanning loop stopped")
    }

    private fun getSafeContext(): Context {
        return try {
            applicationContext ?: this
        } catch (e: Exception) {
            this
        }
    }

    /**
     * Executes an audit pass across the subnet.
     * Identifies unauthorized hosts not present on the whitelist.
     * Strictly verifies idempotency before dispatching push notifications.
     */
    suspend fun performSubnetAudit(
        isManualTrigger: Boolean = false,
        injectedDevices: List<DiscoveredDevice>? = null,
        contextOverride: Context? = null
    ): List<DiscoveredDevice> {
        val effectiveContext = contextOverride ?: getSafeContext()
        val devices = injectedDevices ?: scanLocalSubnet(effectiveContext)
        _lastScannedDeviceCount.value = devices.size

        val db = AppDatabase.getInstance(effectiveContext)
        val whitelistedMacs = try {
            db.networkDeviceDao().getWhitelistedDevicesList()
                .filter { it.isAuthorized }
                .map { it.macAddress.uppercase() }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }

        val newlyDiscoveredUnauthorized = mutableListOf<DiscoveredDevice>()

        for (device in devices) {
            val upperMac = device.macAddress.uppercase()
            val isWhitelisted = whitelistedMacs.contains(upperMac)
            val isSelfOrGateway = device.isSelf || device.isGateway
            val isAuthorized = device.isAuthorized || isWhitelisted || isSelfOrGateway

            if (!isAuthorized) {
                // IDEMPOTENCY CHECK:
                // If this unauthorized device has already been alerted and not dismissed,
                // do NOT dispatch a duplicate push alert.
                val alreadyAlerted = alertedUnauthorizedMacs.containsKey(upperMac)
                if (!alreadyAlerted) {
                    alertedUnauthorizedMacs[upperMac] = System.currentTimeMillis()
                    newlyDiscoveredUnauthorized.add(device)

                    Log.w(TAG, "🚨 NEW Unauthorized Device: ${device.ip} ($upperMac) - Dispatching push alert")
                    
                    // Dispatch instant push alert via Android NotificationManager
                    SecurityNotificationDispatcher.postUnauthorizedDeviceAlert(
                        context = effectiveContext,
                        device = device,
                        threatReason = "Unauthorized host detected on subnet by persistent foreground scanner"
                    )

                    // Persist security alert entry
                    try {
                        db.securityAlertDao().insert(
                            SecurityAlertEntity(
                                title = "🚨 Unauthorized Device Discovered: ${device.ip}",
                                description = "${device.vendor.ifBlank { "Unverified Host" }} ($upperMac) joined local subnet without whitelist authorization.",
                                deviceIp = device.ip,
                                deviceMac = upperMac,
                                severity = "CRITICAL",
                                confidencePercent = 95
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to persist alert in database", e)
                    }
                } else {
                    Log.d(TAG, "Device $upperMac is unauthorized but alert was already dispatched (Idempotent: skipping duplicate alert)")
                }
            }
        }

        _unauthorizedHostsCount.value = alertedUnauthorizedMacs.size

        // Update foreground persistent notification summary
        try {
            val updatedNotification = buildPersistentForegroundNotification(
                statusText = if (alertedUnauthorizedMacs.isNotEmpty())
                    "Shield Active • ${alertedUnauthorizedMacs.size} unauthorized hosts quarantined"
                else
                    "Shield Active • All ${devices.size} subnet hosts authorized",
                devicesScanned = devices.size
            )
            val notificationManager = effectiveContext.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.notify(NOTIFICATION_ID, updatedNotification)
        } catch (e: Exception) {
            Log.w(TAG, "Notification update skipped: ${e.message}")
        }

        return newlyDiscoveredUnauthorized
    }

    private suspend fun scanLocalSubnet(context: Context = getSafeContext()): List<DiscoveredDevice> {
        val wm = wifiManager ?: WiFiManager(context)
        val arpResult = wm.scanForConnectedDevicesUsingArp(
            authorizedMacs = emptySet(),
            subnetRange = 1..254,
            probeTimeoutMs = 100
        )
        return arpResult.devices.map { arp ->
            DiscoveredDevice(
                ip = arp.ip,
                macAddress = arp.macAddress,
                vendor = arp.vendor,
                isAuthorized = arp.isAuthorized,
                isBlocked = arp.isBlocked,
                responseTimeMs = arp.responseTimeMs,
                threatLevel = if (arp.isPotentialUnauthorizedUser) ThreatLevel.UNAUTHORIZED_INTRUDER else ThreatLevel.SAFE,
                isSelf = arp.isSelf,
                isGateway = arp.isGateway
            )
        }
    }

    private fun buildPersistentForegroundNotification(
        statusText: String,
        devicesScanned: Int
    ): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SecurityNotificationDispatcher.EXTRA_NAVIGATE_TO_SECURITY, true)
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            tapIntent,
            pendingFlags
        )

        return NotificationCompat.Builder(this, SecurityNotificationDispatcher.CHANNEL_FOREGROUND_SCANNER_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle("🛡️ Sentinel Network Shield: Active")
            .setContentText(statusText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$statusText\nMonitoring local subnet in real-time. Unauthorized devices are automatically detected and push-alerted.")
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .setColor(0xFF00F0FF.toInt()) // CyberCyan
            .build()
    }

    companion object {
        const val TAG = "NetScanFgService"
        const val NOTIFICATION_ID = 9001
        private const val SCAN_INTERVAL_MS = 20_000L

        const val ACTION_START_SCANNER = "com.example.action.START_NETWORK_SCANNER"
        const val ACTION_STOP_SCANNER = "com.example.action.STOP_NETWORK_SCANNER"
        const val ACTION_TRIGGER_IMMEDIATE_SCAN = "com.example.action.TRIGGER_IMMEDIATE_SCAN"
        const val ACTION_RESET_IDEMPOTENCY = "com.example.action.RESET_IDEMPOTENCY"

        // State tracking
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _lastScannedDeviceCount = MutableStateFlow(0)
        val lastScannedDeviceCount: StateFlow<Int> = _lastScannedDeviceCount.asStateFlow()

        private val _unauthorizedHostsCount = MutableStateFlow(0)
        val unauthorizedHostsCount: StateFlow<Int> = _unauthorizedHostsCount.asStateFlow()

        // Concurrent cache for tracking alerted unauthorized MACs to enforce strict alerting idempotency
        val alertedUnauthorizedMacs = ConcurrentHashMap<String, Long>()

        fun start(context: Context) {
            val intent = Intent(context, NetworkScannerForegroundService::class.java).apply {
                action = ACTION_START_SCANNER
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting foreground scanner service: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, NetworkScannerForegroundService::class.java).apply {
                action = ACTION_STOP_SCANNER
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping foreground scanner service: ${e.message}", e)
            }
        }

        fun triggerScan(context: Context) {
            val intent = Intent(context, NetworkScannerForegroundService::class.java).apply {
                action = ACTION_TRIGGER_IMMEDIATE_SCAN
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering immediate scan: ${e.message}", e)
            }
        }

        fun resetIdempotencyCache() {
            alertedUnauthorizedMacs.clear()
            _unauthorizedHostsCount.value = 0
            Log.d(TAG, "Idempotency cache cleared")
        }

        fun isDeviceAlerted(mac: String): Boolean {
            return alertedUnauthorizedMacs.containsKey(mac.uppercase())
        }

        fun getAlertedDevicesCount(): Int = alertedUnauthorizedMacs.size
    }
}
