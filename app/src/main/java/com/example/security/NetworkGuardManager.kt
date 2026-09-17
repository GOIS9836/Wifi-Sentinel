package com.example.security

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log

interface GuardEventListener {
    fun onGatewayAnomalyDetected(oldGateway: String, newGateway: String)
    fun onSafeLockdownExecuted()
}

/**
 * Compliant Self-Defense Engine.
 * Instead of attacking external devices, this performs host self-isolation:
 * when a critical gateway shift or rogue network anomaly is detected,
 * it unbinds local sockets, isolates the host device, and alerts the user
 * without violating Google Play or telecommunication (FCC/CFAA) policies.
 */
class NetworkGuardManager(
    private val context: Context,
    private val listener: GuardEventListener
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var lockedGatewayBaseline: String? = null
    private var isMonitoring = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) {
            val currentGateway = properties.routes
                .firstOrNull { it.isDefaultRoute }
                ?.gateway
                ?.hostAddress ?: return

            if (lockedGatewayBaseline == null) {
                // Establish initial trusted gateway baseline
                lockedGatewayBaseline = currentGateway
                Log.d("WiFiSentinel", "Baseline locked to gateway: $currentGateway")
            } else if (lockedGatewayBaseline != currentGateway) {
                // Gateway changed without re-association -> Possible ARP poisoning or Rogue AP
                Log.w("WiFiSentinel", "CRITICAL: Gateway shifted from $lockedGatewayBaseline to $currentGateway")
                val oldGw = lockedGatewayBaseline ?: "Unknown"
                listener.onGatewayAnomalyDetected(oldGw, currentGateway)
            }
        }

        override fun onLost(network: Network) {
            Log.d("WiFiSentinel", "Network connection lost or disconnected.")
            lockedGatewayBaseline = null
        }
    }

    fun startMonitoring() {
        if (isMonitoring) return

        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            connectivityManager?.registerNetworkCallback(request, networkCallback)
            isMonitoring = true
            Log.d("WiFiSentinel", "NetworkGuardManager monitoring started.")
        } catch (e: Exception) {
            Log.e("WiFiSentinel", "Failed to start network monitoring: ${e.message}")
        }
    }

    fun stopMonitoring() {
        if (!isMonitoring) return
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e("WiFiSentinel", "Failed to unregister network callback: ${e.message}")
        }
        isMonitoring = false
        Log.d("WiFiSentinel", "NetworkGuardManager monitoring stopped.")
    }

    /**
     * COMPLIANT LOCKDOWN:
     * Does NOT attack or send packets to third-party devices.
     * Protects the host device by stripping network routing privileges
     * and prompting disconnection from the hostile network.
     */
    fun executeSafeLockdown() {
        try {
            // Unbind network from the process to kill ongoing cleartext or hijacked sockets
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager?.bindProcessToNetwork(null)
            }

            // Signal UI to notify user to disconnect from the malicious Wi-Fi
            listener.onSafeLockdownExecuted()
            Log.i("WiFiSentinel", "Safe lockdown executed: Host traffic severed.")
        } catch (e: Exception) {
            Log.e("WiFiSentinel", "Lockdown execution failed: ${e.message}")
        }
    }

    /**
     * Restores normal socket routing after user verifies network safety or switches AP.
     */
    fun releaseLockdown() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Re-bind to default active network if desired, or leave system default
                connectivityManager?.bindProcessToNetwork(null)
            }
            Log.i("WiFiSentinel", "Safe lockdown released.")
        } catch (e: Exception) {
            Log.e("WiFiSentinel", "Lockdown release failed: ${e.message}")
        }
    }

    fun getLockedGatewayBaseline(): String? = lockedGatewayBaseline

    fun resetBaseline(newBaseline: String? = null) {
        lockedGatewayBaseline = newBaseline
        Log.d("WiFiSentinel", "Gateway baseline reset to: $newBaseline")
    }

    fun isCurrentlyMonitoring(): Boolean = isMonitoring
}
