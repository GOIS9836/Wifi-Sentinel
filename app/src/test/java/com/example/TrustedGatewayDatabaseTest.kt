package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.TrustedGateway
import com.example.data.local.TrustedGatewayDao
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrustedGatewayDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: TrustedGatewayDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.trustedGatewayDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveTrustedGateway() = runBlocking {
        val gateway = TrustedGateway(
            gatewayIp = "192.168.1.1",
            bssid = "00:11:22:33:44:55",
            ssid = "Office_Mesh_Node1",
            subnetMask = "255.255.255.0",
            label = "Main Router (Lounge)",
            isPrimary = true
        )
        dao.insertOrUpdate(gateway)

        val retrieved = dao.getGatewayByIp("192.168.1.1")
        assertNotNull(retrieved)
        assertEquals("192.168.1.1", retrieved?.gatewayIp)
        assertEquals("Office_Mesh_Node1", retrieved?.ssid)
        assertEquals("Main Router (Lounge)", retrieved?.label)
        assertTrue(retrieved?.isPrimary == true)

        val all = dao.getAllTrustedGateways().first()
        assertEquals(1, all.size)
    }

    @Test
    fun testSetAsPrimaryGateway() = runBlocking {
        val gw1 = TrustedGateway(
            gatewayIp = "192.168.1.1",
            bssid = "00:11:22:33:44:55",
            ssid = "Mesh_AP1",
            subnetMask = "255.255.255.0",
            label = "Main Router (Lounge)",
            isPrimary = true
        )
        val gw2 = TrustedGateway(
            gatewayIp = "192.168.1.2",
            bssid = "00:11:22:33:44:56",
            ssid = "Mesh_AP2",
            subnetMask = "255.255.255.0",
            label = "AP Upstairs",
            isPrimary = false
        )
        dao.insertOrUpdate(gw1)
        dao.insertOrUpdate(gw2)

        // Make gw2 primary
        dao.setAsPrimary("192.168.1.2")

        val updatedGw1 = dao.getGatewayByIp("192.168.1.1")
        val updatedGw2 = dao.getGatewayByIp("192.168.1.2")

        assertFalse(updatedGw1?.isPrimary ?: true)
        assertTrue(updatedGw2?.isPrimary ?: false)

        val primary = dao.getPrimaryGateway().first()
        assertEquals("192.168.1.2", primary?.gatewayIp)
    }

    @Test
    fun testDeleteTrustedGateway() = runBlocking {
        val gateway = TrustedGateway(
            gatewayIp = "10.0.0.1",
            bssid = null,
            ssid = "Subnet_Gateway",
            subnetMask = "255.0.0.0",
            label = "Datacenter Uplink",
            isPrimary = false
        )
        dao.insertOrUpdate(gateway)
        assertNotNull(dao.getGatewayByIp("10.0.0.1"))

        dao.deleteGateway("10.0.0.1")
        assertNull(dao.getGatewayByIp("10.0.0.1"))
    }
}
