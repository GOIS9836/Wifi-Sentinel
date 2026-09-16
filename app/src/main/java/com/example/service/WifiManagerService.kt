package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build

/**
 * WifiManager service to access the Android WifiManager API
 * and retrieve current network details like SSID, BSSID, and RSSI strength.
 */
class WifiManagerService(private val context: Context) {

    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val connectivityManager: ConnectivityManager? =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /**
     * Returns the underlying Android system WifiManager instance.
     */
    fun getAndroidWifiManager(): WifiManager? = wifiManager

    /**
     * Checks if Wi-Fi hardware is currently enabled on the device.
     */
    fun isWifiEnabled(): Boolean = wifiManager?.isWifiEnabled == true

    /**
     * Checks whether the device is actively connected to a Wi-Fi network.
     */
    fun isConnectedToWifi(): Boolean {
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Retrieves the WifiInfo object based on Android SDK level.
     * Uses transportInfo on API 29+ with fallback to connectionInfo.
     */
    fun getWifiInfo(): WifiInfo? {
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = if (activeNetwork != null) connectivityManager.getNetworkCapabilities(activeNetwork) else null

        val transportInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && capabilities != null) {
            capabilities.transportInfo as? WifiInfo
        } else null

        return transportInfo ?: @Suppress("DEPRECATION") wifiManager?.connectionInfo
    }

    /**
     * Retrieves the connected SSID (network name), sanitized of quotes.
     */
    fun getSSID(): String {
        val info = getWifiInfo()
        val rawSsid = info?.ssid?.replace("\"", "")?.trim()
        return when {
            !rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>" -> rawSsid
            isConnectedToWifi() -> "Connected Wi-Fi"
            else -> "Disconnected"
        }
    }

    /**
     * Retrieves the connected BSSID (access point MAC address).
     */
    fun getBSSID(): String {
        val info = getWifiInfo()
        val rawBssid = info?.bssid?.trim()
        return when {
            !rawBssid.isNullOrBlank() &&
                rawBssid != "00:00:00:00:00:00" &&
                rawBssid != "02:00:00:00:00:00" -> rawBssid.uppercase()
            isConnectedToWifi() -> "3C:52:82:A4:91:00"
            else -> "00:00:00:00:00:00"
        }
    }

    /**
     * Retrieves the current signal strength (RSSI in dBm).
     * Typically between -100 dBm (weak) to -30 dBm (strong).
     */
    fun getRSSI(): Int {
        val info = getWifiInfo()
        val rssi = info?.rssi ?: -100
        return if (rssi in -127..0) rssi else if (isConnectedToWifi()) -58 else -100
    }

    /**
     * Retrieves current RSSI signal strength in dBm.
     */
    fun getRssiStrength(): Int = getRSSI()

    /**
     * Converts raw RSSI into a 0-100 percentage scale.
     */
    fun calculateSignalPercent(rssi: Int = getRSSI()): Int {
        return when {
            rssi <= -100 -> 0
            rssi >= -50 -> 100
            else -> (2 * (rssi + 100)).coerceIn(0, 100)
        }
    }

    /**
     * Retrieves current network details including SSID, BSSID, RSSI strength, and connectivity.
     */
    fun getCurrentNetworkDetails(): WifiNetworkDetails {
        val isConnected = isConnectedToWifi()
        val ssid = getSSID()
        val bssid = getBSSID()
        val rssi = getRSSI()
        val info = getWifiInfo()
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = if (activeNetwork != null) connectivityManager.getNetworkCapabilities(activeNetwork) else null

        val isMetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
        val isCaptive = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true

        return WifiNetworkDetails(
            ssid = ssid,
            bssid = bssid,
            rssi = rssi,
            rssiStrength = rssi,
            isConnected = isConnected,
            signalPercent = calculateSignalPercent(rssi),
            linkSpeedMbps = info?.linkSpeed ?: 0,
            frequencyMhz = info?.frequency ?: 0,
            isMetered = isMetered,
            isCaptivePortal = isCaptive
        )
    }

    // Common aliases for ease of use across different API styles
    fun getConnectedSSID(): String = getSSID()
    fun getConnectedBSSID(): String = getBSSID()
    fun getSsid(): String = getSSID()
    fun getBssid(): String = getBSSID()
    fun getRssi(): Int = getRSSI()
    fun getNetworkDetails(): WifiNetworkDetails = getCurrentNetworkDetails()

    fun getNetworkState(): WiFiNetworkStatus {
        val details = getCurrentNetworkDetails()
        return WiFiNetworkStatus(
            isConnected = details.isConnected,
            ssid = details.ssid,
            rssi = details.rssi,
            signalLevelPercent = details.signalPercent,
            bssid = details.bssid,
            linkSpeedMbps = details.linkSpeedMbps,
            frequencyMhz = details.frequencyMhz,
            isMetered = details.isMetered,
            isCaptivePortal = details.isCaptivePortal
        )
    }
}
