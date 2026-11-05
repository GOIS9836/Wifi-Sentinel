package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DetectedThreatDao
import com.example.data.local.DetectedThreatEntity
import com.example.data.local.NetworkConnectionHistoryDao
import com.example.data.local.NetworkConnectionHistoryEntity
import com.example.security.MacSanitizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric JVM Test Suite for Room Database Schema & DAOs:
 * - NetworkConnectionHistoryEntity & NetworkConnectionHistoryDao
 * - DetectedThreatEntity & DetectedThreatDao
 * - POTRAZ Cyber and Data Protection Act [Chapter 12:07] data minimization compliance
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NetworkHistoryAndThreatDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var historyDao: NetworkConnectionHistoryDao
    private lateinit var threatDao: DetectedThreatDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        historyDao = database.networkConnectionHistoryDao()
        threatDao = database.detectedThreatDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // NetworkConnectionHistoryDao Tests
    // =========================================================================

    @Test
    fun testInsertAndRetrieveConnectionHistory() = runBlocking {
        val rawBssid = "00:14:22:01:23:45"
        val ssid = "Harare_Fiber_Secure"
        val pseudonym = MacSanitizer.hashIdentifier("$rawBssid:$ssid")

        val connection = NetworkConnectionHistoryEntity(
            ssid = ssid,
            bssid = rawBssid,
            gatewayIp = "192.168.100.1",
            gatewayMac = "00:14:22:01:23:40",
            localIp = "192.168.100.42",
            frequencyMhz = 5180,
            channel = 36,
            securityType = "WPA3-SAE",
            rssiDbm = -55,
            linkSpeedMbps = 866,
            connectedTimestamp = System.currentTimeMillis() - 600000,
            safetyScore = 98,
            pseudonymHash = pseudonym
        )

        val id = historyDao.insert(connection)
        assertTrue(id > 0)

        val retrieved = historyDao.getConnectionById(id)
        assertNotNull(retrieved)
        assertEquals(ssid, retrieved?.ssid)
        assertEquals(rawBssid, retrieved?.bssid)
        assertEquals("WPA3-SAE", retrieved?.securityType)
        assertEquals(98, retrieved?.safetyScore)
        assertEquals(pseudonym, retrieved?.pseudonymHash)
        assertNull(retrieved?.disconnectedTimestamp)

        // Verify active connection query
        val active = historyDao.getActiveConnection()
        assertNotNull(active)
        assertEquals(id, active?.id)

        // Close session
        val disconnectTime = System.currentTimeMillis()
        val duration = 600L
        historyDao.closeSession(id, disconnectTime, duration)

        val closed = historyDao.getConnectionById(id)
        assertNotNull(closed?.disconnectedTimestamp)
        assertEquals(duration, closed?.durationSeconds)

        // Active connection should now be null
        val activeAfterClose = historyDao.getActiveConnection()
        assertNull(activeAfterClose)
    }

    @Test
    fun testConnectionHistoryFiltersAndPruning() = runBlocking {
        val now = System.currentTimeMillis()

        for (i in 1..5) {
            val ssid = if (i % 2 == 0) "Corp_Mesh_East" else "Guest_WiFi"
            historyDao.insert(
                NetworkConnectionHistoryEntity(
                    ssid = ssid,
                    bssid = "02:00:00:00:00:0$i",
                    gatewayIp = "10.0.0.1",
                    gatewayMac = "00:11:22:33:44:55",
                    localIp = "10.0.0.$i",
                    frequencyMhz = 2412,
                    channel = 1,
                    securityType = "WPA2-PSK",
                    rssiDbm = -60 - i,
                    linkSpeedMbps = 150,
                    connectedTimestamp = now - (i * 100000L),
                    disconnectedTimestamp = now - ((i - 1) * 100000L),
                    durationSeconds = 100L,
                    safetyScore = 90 - i,
                    pseudonymHash = MacSanitizer.hashIdentifier("02:00:00:00:00:0$i")
                )
            )
        }

        val all = historyDao.getAllConnectionHistory().first()
        assertEquals(5, all.size)

        val corpList = historyDao.getHistoryForSsid("Corp_Mesh_East").first()
        assertEquals(2, corpList.size)

        val recentTwo = historyDao.getRecentConnections(limit = 2)
        assertEquals(2, recentTwo.size)

        // Prune older than 350 seconds ago
        val cutoff = now - 250000L
        val deletedCount = historyDao.deleteOlderThan(cutoff)
        assertTrue(deletedCount >= 3)

        val remaining = historyDao.getAllConnectionHistory().first()
        assertEquals(5 - deletedCount, remaining.size)
    }

    // =========================================================================
    // DetectedThreatDao Tests
    // =========================================================================

    @Test
    fun testInsertAndMitigateDetectedThreat() = runBlocking {
        val rogueMac = "AA:BB:CC:DD:EE:FF"
        val threat = DetectedThreatEntity(
            threatType = "EVIL_TWIN",
            severity = "CRITICAL",
            title = "Rogue Twin Access Point Detected",
            description = "BSSID clone spoofing legit Corp SSID on channel 6 with downgrade to OPEN",
            sourceIp = "192.168.1.250",
            sourceMac = rogueMac,
            pseudonymHash = MacSanitizer.hashIdentifier(rogueMac),
            targetSsid = "Corp_Primary_5G",
            targetBssid = "00:1A:2B:3C:4D:5E",
            channel = 6,
            rssiDbm = -35,
            confidencePercent = 99
        )

        val threatId = threatDao.insert(threat)
        assertTrue(threatId > 0)

        // Check active threats
        val activeThreats = threatDao.getActiveThreats().first()
        assertEquals(1, activeThreats.size)
        assertEquals("CRITICAL", activeThreats[0].severity)
        assertFalse(activeThreats[0].isMitigated)
        assertFalse(activeThreats[0].isFalsePositive)

        val activeCount = threatDao.getActiveThreatCount().first()
        assertEquals(1, activeCount)

        // Mitigate threat
        threatDao.updateMitigation(threatId, isMitigated = true, mitigationAction = "Isolated via 802.11w Protected Mgmt Frames & ACL Block")

        val mitigatedThreat = threatDao.getThreatById(threatId)
        assertNotNull(mitigatedThreat)
        assertTrue(mitigatedThreat?.isMitigated == true)
        assertEquals("Isolated via 802.11w Protected Mgmt Frames & ACL Block", mitigatedThreat?.mitigationAction)

        // Active threat count should drop to 0
        val activeAfterMitigation = threatDao.getActiveThreatCount().first()
        assertEquals(0, activeAfterMitigation)
    }

    @Test
    fun testFalsePositiveCorroborationByMac() = runBlocking {
        val testMac = "52:54:00:12:34:56"
        val t1 = DetectedThreatEntity(
            threatType = "ARP_SPOOFING",
            severity = "HIGH",
            title = "Gratuitous ARP storm",
            description = "High rate of ARP announcements",
            sourceIp = "192.168.1.15",
            sourceMac = testMac,
            pseudonymHash = MacSanitizer.hashIdentifier(testMac),
            targetSsid = "Lab_Mesh",
            targetBssid = "00:11:22:33:44:55"
        )
        val t2 = DetectedThreatEntity(
            threatType = "ROGUE_LISTENER",
            severity = "MEDIUM",
            title = "Promiscuous mode probe",
            description = "Probe requests in monitor mode",
            sourceIp = "192.168.1.15",
            sourceMac = testMac,
            pseudonymHash = MacSanitizer.hashIdentifier(testMac),
            targetSsid = "Lab_Mesh",
            targetBssid = "00:11:22:33:44:55"
        )

        threatDao.insert(t1)
        threatDao.insert(t2)

        val activeBefore = threatDao.getActiveThreats().first()
        assertEquals(2, activeBefore.size)

        // User or system corroborates benign virtual switch behavior -> mark false positive by MAC
        threatDao.markFalsePositiveByMac(testMac)

        val activeAfter = threatDao.getActiveThreats().first()
        assertEquals(0, activeAfter.size)

        val all = threatDao.getAllThreats().first()
        assertEquals(2, all.size)
        assertTrue(all.all { it.isFalsePositive && it.isMitigated })
    }

    @Test
    fun testPotrazPseudonymHashIntegrity() {
        // Under POTRAZ CDPA Chapter 12:07, persistent entities must maintain deterministic salted pseudonym hashes
        val mac1 = "AA:BB:CC:11:22:33"
        val hash1 = MacSanitizer.hashIdentifier(mac1)
        val hash2 = MacSanitizer.hashIdentifier(mac1)

        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // SHA-256 hex string length
        assertFalse(hash1.contains("AA:BB:CC")) // Zero cleartext MAC leakage
    }
}
