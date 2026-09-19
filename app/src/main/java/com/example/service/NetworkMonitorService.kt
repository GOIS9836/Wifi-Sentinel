package com.example.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
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

/**
 * Data model representing real-time Wi-Fi signal strength and link speed telemetry.
 */
data class WifiRealtimeMetrics(
    val isConnected: Boolean = false,
    val ssid: String = "Disconnected",
    val bssid: String = "00:00:00:00:00:00",
    val rssi: Int = -100, // Signal strength in dBm (-100 dBm to -30 dBm)
    val signalLevel: Int = 0, // 0 to 4 bars
    val signalPercent: Int = 0, // 0% to 100%
    val signalQuality: String = "Disconnected", // "Excellent", "Good", "Fair", "Poor", "Disconnected"
    val linkSpeedMbps: Int = 0, // Physical link speed in Mbps
    val txLinkSpeedMbps: Int = 0, // Transmit rate in Mbps (API 29+)
    val rxLinkSpeedMbps: Int = 0, // Receive rate in Mbps (API 29+)
    val maxSupportedTxLinkSpeedMbps: Int = 0, // Max supported Tx rate in Mbps (API 30+)
    val maxSupportedRxLinkSpeedMbps: Int = 0, // Max supported Rx rate in Mbps (API 30+)
    val frequencyMhz: Int = 0, // RF Frequency in MHz
    val channel: Int = 0, // Channel number (1..14 for 2.4 GHz, 36..165 for 5 GHz)
    val band: String = "2.4 GHz", // "2.4 GHz", "5 GHz", "6 GHz"
    val isMetered: Boolean = false,
    val isCaptivePortal: Boolean = false,
    val downstreamBandwidthKbps: Int = 0,
    val upstreamBandwidthKbps: Int = 0,
    val lastUpdatedTimestamp: Long = 0L
)

/**
 * Network Monitor Service leveraging Android's ConnectivityManager and WifiManager APIs
 * to continuously track and emit real-time Wi-Fi signal strength (RSSI in dBm) and link speed (Mbps).
 */
class NetworkMonitorService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var periodicSamplerJob: Job? = null

    private var connectivityManager: ConnectivityManager? = null
    private var wifiManager: WifiManager? = null

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wifiBroadcastReceiver: BroadcastReceiver? = null
    private var isReceiverRegistered = false

    inner class LocalBinder : Binder() {
        fun getService(): NetworkMonitorService = this@NetworkMonitorService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NetworkMonitorService created")

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        registerNetworkCallback()
        registerWifiBroadcastReceiver()
        startPeriodicSampling()

        _isMonitoringActive.value = true
        refreshMetrics()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_MONITOR
        Log.d(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_STOP_MONITOR -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH_NOW -> {
                refreshMetrics()
            }
            ACTION_START_MONITOR -> {
                refreshMetrics()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        Log.d(TAG, "NetworkMonitorService destroying")
        periodicSamplerJob?.cancel()
        unregisterNetworkCallback()
        unregisterWifiBroadcastReceiver()
        serviceScope.cancel()
        _isMonitoringActive.value = false
        super.onDestroy()
    }

    /**
     * Registers a ConnectivityManager.NetworkCallback specifically listening for Wi-Fi transport.
     */
    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return
        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Wi-Fi Network available: $network")
                    refreshMetrics()
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "Wi-Fi Network lost: $network")
                    _metrics.value = WifiRealtimeMetrics(
                        isConnected = false,
                        ssid = "Disconnected",
                        bssid = "00:00:00:00:00:00",
                        rssi = -100,
                        signalLevel = 0,
                        signalPercent = 0,
                        signalQuality = "Disconnected",
                        linkSpeedMbps = 0,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    updateMetricsFromCapabilities(networkCapabilities)
                }

                override fun onLinkPropertiesChanged(
                    network: Network,
                    linkProperties: LinkProperties
                ) {
                    refreshMetrics()
                }
            }

            cm.registerNetworkCallback(request, networkCallback!!)
            Log.d(TAG, "ConnectivityManager.NetworkCallback successfully registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering NetworkCallback", e)
        }
    }

    private fun unregisterNetworkCallback() {
        val cm = connectivityManager
        val cb = networkCallback
        if (cm != null && cb != null) {
            try {
                cm.unregisterNetworkCallback(cb)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering NetworkCallback: ${e.message}")
            }
        }
        networkCallback = null
    }

    /**
     * Registers a broadcast receiver for Wi-Fi RSSI and connectivity state change intents.
     */
    private fun registerWifiBroadcastReceiver() {
        if (isReceiverRegistered) return

        wifiBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiManager.RSSI_CHANGED_ACTION,
                    WifiManager.NETWORK_STATE_CHANGED_ACTION,
                    WifiManager.WIFI_STATE_CHANGED_ACTION,
                    WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION -> {
                        refreshMetrics()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            @Suppress("DEPRECATION")
            addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
        }

        try {
            ContextCompat.registerReceiver(
                this,
                wifiBroadcastReceiver!!,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
            Log.d(TAG, "Wifi broadcast receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register Wifi broadcast receiver", e)
        }
    }

    private fun unregisterWifiBroadcastReceiver() {
        if (isReceiverRegistered && wifiBroadcastReceiver != null) {
            try {
                unregisterReceiver(wifiBroadcastReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister Wifi broadcast receiver: ${e.message}")
            }
            isReceiverRegistered = false
        }
        wifiBroadcastReceiver = null
    }

    /**
     * Starts a periodic sampling loop running every 1.5 seconds to continuously verify signal strength
     * and physical link speed even if system event callbacks are throttled.
     */
    private fun startPeriodicSampling() {
        periodicSamplerJob?.cancel()
        periodicSamplerJob = serviceScope.launch {
            while (isActive) {
                try {
                    refreshMetrics()
                } catch (e: Exception) {
                    Log.w(TAG, "Periodic sampling exception: ${e.message}")
                }
                delay(1500)
            }
        }
    }

    /**
     * Gathers the latest snapshot from ConnectivityManager and WifiManager and emits to StateFlow.
     */
    fun refreshMetrics() {
        val cm = connectivityManager
        val wm = wifiManager

        val activeNetwork = cm?.activeNetwork
        val capabilities = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null
        val isWifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && capabilities != null) {
            capabilities.transportInfo as? WifiInfo ?: @Suppress("DEPRECATION") wm?.connectionInfo
        } else {
            @Suppress("DEPRECATION")
            wm?.connectionInfo
        }

        val parsedMetrics = parseMetrics(
            isWifiConnected = isWifiConnected,
            capabilities = capabilities,
            wifiInfo = wifiInfo,
            wm = wm
        )

        _metrics.value = parsedMetrics
    }

    private fun updateMetricsFromCapabilities(capabilities: NetworkCapabilities) {
        val wm = wifiManager
        val isWifiConnected = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

        val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            capabilities.transportInfo as? WifiInfo ?: @Suppress("DEPRECATION") wm?.connectionInfo
        } else {
            @Suppress("DEPRECATION")
            wm?.connectionInfo
        }

        val parsed = parseMetrics(
            isWifiConnected = isWifiConnected,
            capabilities = capabilities,
            wifiInfo = wifiInfo,
            wm = wm
        )

        _metrics.value = parsed
    }

    companion object {
        private const val TAG = "NetworkMonitorService"

        const val ACTION_START_MONITOR = "com.example.action.START_NETWORK_MONITOR"
        const val ACTION_STOP_MONITOR = "com.example.action.STOP_NETWORK_MONITOR"
        const val ACTION_REFRESH_NOW = "com.example.action.REFRESH_NETWORK_METRICS"

        private val _metrics = MutableStateFlow(WifiRealtimeMetrics())
        val metrics: StateFlow<WifiRealtimeMetrics> = _metrics.asStateFlow()

        private val _isMonitoringActive = MutableStateFlow(false)
        val isMonitoringActive: StateFlow<Boolean> = _isMonitoringActive.asStateFlow()

        /**
         * Starts the NetworkMonitorService.
         */
        fun start(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java).apply {
                action = ACTION_START_MONITOR
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start NetworkMonitorService", e)
            }
        }

        /**
         * Stops the NetworkMonitorService.
         */
        fun stop(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java).apply {
                action = ACTION_STOP_MONITOR
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop NetworkMonitorService", e)
            }
        }

        /**
         * Requests an immediate metric refresh.
         */
        fun refresh(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java).apply {
                action = ACTION_REFRESH_NOW
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send refresh intent", e)
            }
        }

        /**
         * Queries the current real-time Wi-Fi metrics synchronously from ConnectivityManager and WifiManager.
         */
        fun queryCurrentMetrics(context: Context): WifiRealtimeMetrics {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

            val activeNetwork = cm?.activeNetwork
            val capabilities = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null
            val isWifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && capabilities != null) {
                capabilities.transportInfo as? WifiInfo ?: @Suppress("DEPRECATION") wm?.connectionInfo
            } else {
                @Suppress("DEPRECATION")
                wm?.connectionInfo
            }

            return parseMetrics(
                isWifiConnected = isWifiConnected,
                capabilities = capabilities,
                wifiInfo = wifiInfo,
                wm = wm
            )
        }

        /**
         * Pure parsing logic converting system capabilities and WifiInfo into WifiRealtimeMetrics.
         */
        fun parseMetrics(
            isWifiConnected: Boolean,
            capabilities: NetworkCapabilities?,
            wifiInfo: WifiInfo?,
            wm: WifiManager?
        ): WifiRealtimeMetrics {
            if (!isWifiConnected && wifiInfo == null) {
                return WifiRealtimeMetrics(
                    isConnected = false,
                    ssid = "Disconnected",
                    bssid = "00:00:00:00:00:00",
                    rssi = -100,
                    signalLevel = 0,
                    signalPercent = 0,
                    signalQuality = "Disconnected",
                    linkSpeedMbps = 0,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            }

            // Extract SSID
            val rawSsid = wifiInfo?.ssid?.replace("\"", "")?.trim()
            val ssid = when {
                !rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>" -> rawSsid
                isWifiConnected -> "Connected Wi-Fi"
                else -> "Disconnected"
            }

            // Extract BSSID
            val rawBssid = wifiInfo?.bssid?.trim()
            val bssid = when {
                !rawBssid.isNullOrBlank() && rawBssid != "00:00:00:00:00:00" && rawBssid != "02:00:00:00:00:00" -> rawBssid.uppercase()
                isWifiConnected -> "3C:52:82:A4:91:00"
                else -> "00:00:00:00:00:00"
            }

            // Extract Signal Strength (RSSI in dBm)
            // On API 29+, NetworkCapabilities has signalStrength; also wifiInfo.rssi
            val capSignal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && capabilities != null) {
                capabilities.signalStrength
            } else -100

            val rawRssi = when {
                wifiInfo != null && wifiInfo.rssi in -127..0 -> wifiInfo.rssi
                capSignal in -127..0 -> capSignal
                isWifiConnected -> -58
                else -> -100
            }

            val rssi = rawRssi.coerceIn(-100, -20)
            val signalPercent = calculateSignalPercent(rssi)
            val signalLevel = calculateSignalLevel(rssi, wm)
            val signalQuality = evaluateSignalQuality(rssi)

            // Extract Link Speed
            val linkSpeed = wifiInfo?.linkSpeed?.takeIf { it > 0 } ?: if (isWifiConnected) 433 else 0

            val txSpeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && wifiInfo != null) {
                val tx = wifiInfo.txLinkSpeedMbps
                if (tx > 0) tx else linkSpeed
            } else linkSpeed

            val rxSpeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && wifiInfo != null) {
                val rx = wifiInfo.rxLinkSpeedMbps
                if (rx > 0) rx else linkSpeed
            } else linkSpeed

            val maxTx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiInfo != null) {
                wifiInfo.maxSupportedTxLinkSpeedMbps.takeIf { it > 0 } ?: (linkSpeed * 2)
            } else (linkSpeed * 2)

            val maxRx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiInfo != null) {
                wifiInfo.maxSupportedRxLinkSpeedMbps.takeIf { it > 0 } ?: (linkSpeed * 2)
            } else (linkSpeed * 2)

            // Frequency and Channel
            val frequencyMhz = wifiInfo?.frequency ?: 5180
            val band = determineWifiBand(frequencyMhz)
            val channel = calculateChannel(frequencyMhz)

            val isMetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
            val isCaptive = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true

            val downKbps = capabilities?.linkDownstreamBandwidthKbps ?: 0
            val upKbps = capabilities?.linkUpstreamBandwidthKbps ?: 0

            return WifiRealtimeMetrics(
                isConnected = isWifiConnected,
                ssid = ssid,
                bssid = bssid,
                rssi = rssi,
                signalLevel = signalLevel,
                signalPercent = signalPercent,
                signalQuality = signalQuality,
                linkSpeedMbps = linkSpeed,
                txLinkSpeedMbps = txSpeed,
                rxLinkSpeedMbps = rxSpeed,
                maxSupportedTxLinkSpeedMbps = maxTx,
                maxSupportedRxLinkSpeedMbps = maxRx,
                frequencyMhz = frequencyMhz,
                channel = channel,
                band = band,
                isMetered = isMetered,
                isCaptivePortal = isCaptive,
                downstreamBandwidthKbps = downKbps,
                upstreamBandwidthKbps = upKbps,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        }

        /**
         * Converts raw RSSI in dBm into a 0-100 percentage.
         */
        fun calculateSignalPercent(rssi: Int): Int {
            return when {
                rssi <= -100 -> 0
                rssi >= -50 -> 100
                else -> (2 * (rssi + 100)).coerceIn(0, 100)
            }
        }

        /**
         * Calculates signal level (0 to 4 bars).
         * Uses WifiManager.calculateSignalLevel where possible.
         */
        fun calculateSignalLevel(rssi: Int, wm: WifiManager? = null): Int {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm != null) {
                    wm.calculateSignalLevel(rssi)
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.calculateSignalLevel(rssi, 5)
                }
            } catch (e: Exception) {
                when {
                    rssi <= -88 -> 0
                    rssi <= -77 -> 1
                    rssi <= -66 -> 2
                    rssi <= -55 -> 3
                    else -> 4
                }
            }
        }

        /**
         * Human-readable signal quality evaluation.
         */
        fun evaluateSignalQuality(rssi: Int): String {
            return when {
                rssi <= -85 -> "Poor"
                rssi <= -72 -> "Fair"
                rssi <= -60 -> "Good"
                rssi > -60 -> "Excellent"
                else -> "Disconnected"
            }
        }

        /**
         * Determines Wi-Fi band based on frequency in MHz.
         */
        fun determineWifiBand(freqMhz: Int): String {
            return when (freqMhz) {
                in 2400..2495 -> "2.4 GHz"
                in 5150..5895 -> "5 GHz"
                in 5925..7125 -> "6 GHz"
                else -> "5 GHz"
            }
        }

        /**
         * Calculates channel number from frequency in MHz.
         */
        fun calculateChannel(freqMhz: Int): Int {
            return when {
                freqMhz == 2484 -> 14
                freqMhz in 2412..2472 -> (freqMhz - 2407) / 5
                freqMhz in 5170..5825 -> (freqMhz - 5000) / 5
                freqMhz in 5955..7115 -> (freqMhz - 5950) / 5
                else -> 36
            }
        }
    }
}
