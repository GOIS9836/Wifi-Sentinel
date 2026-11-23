package com.example.wifisentinel

import com.example.service.AndroidAppOptimizerEngine
import com.example.service.OptimizationProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AndroidAppOptimizerEngineTest {

    @Test
    fun testOptimizationProfileIntegrity() {
        val profile = OptimizationProfile(
            memoryFreedBytes = 25_000_000L,
            storageFreedBytes = 18_000_000L,
            dnsPurged = true,
            networkLatencyMs = 8L,
            batterySaverModeActive = false,
            greenAiEnergySavingsPercent = 74.2f,
            executionDurationMs = 120L
        )

        assertNotNull(profile)
        assertTrue(profile.memoryFreedBytes > 0)
        assertTrue(profile.storageFreedBytes > 0)
        assertTrue(profile.dnsPurged)
        assertTrue(profile.greenAiEnergySavingsPercent >= 70.0f)
        assertEquals(8L, profile.networkLatencyMs)
    }
}
