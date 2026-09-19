package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.service.NetworkMonitorService
import com.example.service.WiFiManager
import com.example.service.WifiRealtimeMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkMonitorServiceTest {

    private lateinit var application: Application

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSignalPercentageCalculations() {
        assertEquals(0, NetworkMonitorService.calculateSignalPercent(-105))
        assertEquals(0, NetworkMonitorService.calculateSignalPercent(-100))
        assertEquals(100, NetworkMonitorService.calculateSignalPercent(-50))
        assertEquals(100, NetworkMonitorService.calculateSignalPercent(-30))

        // -75 dBm -> 2 * (-75 + 100) = 50%
        assertEquals(50, NetworkMonitorService.calculateSignalPercent(-75))
        // -60 dBm -> 2 * (-60 + 100) = 80%
        assertEquals(80, NetworkMonitorService.calculateSignalPercent(-60))
    }

    @Test
    fun testSignalQualityEvaluation() {
        assertEquals("Excellent", NetworkMonitorService.evaluateSignalQuality(-50))
        assertEquals("Good", NetworkMonitorService.evaluateSignalQuality(-65))
        assertEquals("Fair", NetworkMonitorService.evaluateSignalQuality(-75))
        assertEquals("Poor", NetworkMonitorService.evaluateSignalQuality(-90))
    }

    @Test
    fun testWifiBandDetermination() {
        assertEquals("2.4 GHz", NetworkMonitorService.determineWifiBand(2412))
        assertEquals("2.4 GHz", NetworkMonitorService.determineWifiBand(2462))
        assertEquals("5 GHz", NetworkMonitorService.determineWifiBand(5180))
        assertEquals("5 GHz", NetworkMonitorService.determineWifiBand(5745))
        assertEquals("6 GHz", NetworkMonitorService.determineWifiBand(5955))
        assertEquals("6 GHz", NetworkMonitorService.determineWifiBand(6200))
    }

    @Test
    fun testChannelCalculation() {
        assertEquals(1, NetworkMonitorService.calculateChannel(2412))
        assertEquals(6, NetworkMonitorService.calculateChannel(2437))
        assertEquals(11, NetworkMonitorService.calculateChannel(2462))
        assertEquals(14, NetworkMonitorService.calculateChannel(2484))
        assertEquals(36, NetworkMonitorService.calculateChannel(5180))
        assertEquals(40, NetworkMonitorService.calculateChannel(5200))
    }

    @Test
    fun testQueryCurrentMetricsInRobolectric() {
        val metrics = NetworkMonitorService.queryCurrentMetrics(application)
        assertNotNull(metrics)
        assertNotNull(metrics.ssid)
        assertNotNull(metrics.bssid)
        assertTrue(metrics.rssi <= 0)
        assertTrue(metrics.signalPercent in 0..100)
        assertTrue(metrics.linkSpeedMbps >= 0)
    }

    @Test
    fun testParseMetricsDisconnectedFallback() {
        val metrics = NetworkMonitorService.parseMetrics(
            isWifiConnected = false,
            capabilities = null,
            wifiInfo = null,
            wm = null
        )

        assertFalse(metrics.isConnected)
        assertEquals("Disconnected", metrics.ssid)
        assertEquals(-100, metrics.rssi)
        assertEquals(0, metrics.linkSpeedMbps)
        assertEquals(0, metrics.signalPercent)
    }

    @Test
    fun testWiFiManagerRealtimeMetricsIntegration() {
        val wifiManager = WiFiManager(application)
        val realtimeMetrics = wifiManager.getRealtimeMetrics()
        assertNotNull(realtimeMetrics)
        assertNotNull(realtimeMetrics.signalQuality)
    }
}
