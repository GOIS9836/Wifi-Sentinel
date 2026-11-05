package com.example

import com.example.data.model.NodeClusterStatus
import com.example.service.LinuxSentryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LinuxSentryManagerTest {

    @Test
    fun testInitialLinuxNodesPopulated() {
        val nodes = LinuxSentryManager.linuxNodes.value
        assertTrue("Initial Linux machines must be registered", nodes.isNotEmpty())
        val pergamus = nodes.firstOrNull { it.hostname == "Nodes-Pergamus" }
        assertNotNull("Nodes-Pergamus should be in cluster", pergamus)
        assertEquals("Ubuntu 24.04 LTS (Noble Numbat)", pergamus?.distro)
    }

    @Test
    fun testIngestTelemetryJsonUpdatesNodeAndGeneratesAlert() {
        val sampleJson = """
            {
                "node_id": "Ubuntu-Lab-Box",
                "hostname": "Ubuntu-Lab-Box",
                "payload": {
                    "ip": "192.168.1.175",
                    "mac": "00:1A:2B:33:44:55",
                    "iface": "wlan0",
                    "os": {
                        "distro": "Ubuntu 22.04 LTS",
                        "kernel": "5.15.0-generic"
                    },
                    "wifi": {
                        "is_wifi": true,
                        "ssid": "Office_Ultra_5G",
                        "rssi_dbm": -55
                    },
                    "active_threats_count": 1,
                    "subnet_device_count": 5,
                    "listeners": [
                        {
                            "port": 5555,
                            "protocol": "TCP",
                            "is_rogue": true,
                            "desc": "Wireless ADB"
                        }
                    ],
                    "firewall_blocked_macs": ["DA:A1:19:00:11:22"]
                }
            }
        """.trimIndent()

        val initialPayloadCount = LinuxSentryManager.hubState.value.ingestedPayloadCount
        LinuxSentryManager.ingestTelemetryJson(sampleJson)

        val updatedNodes = LinuxSentryManager.linuxNodes.value
        val ingestedNode = updatedNodes.firstOrNull { it.hostname == "Ubuntu-Lab-Box" }

        assertNotNull("Ingested Linux node must be found", ingestedNode)
        assertEquals("192.168.1.175", ingestedNode?.ipAddress)
        assertEquals("Ubuntu 22.04 LTS", ingestedNode?.distro)
        assertEquals(1, ingestedNode?.activeThreatsCount)
        assertEquals(1, ingestedNode?.rogueListeners?.size)

        assertTrue("Hub server payload count must increment",
            LinuxSentryManager.hubState.value.ingestedPayloadCount > initialPayloadCount)

        val alerts = LinuxSentryManager.recentAlerts.value
        assertTrue("Alert must be triggered for rogue port 5555",
            alerts.any { it.hostname == "Ubuntu-Lab-Box" && it.title.contains("Rogue Sockets") })
    }

    @Test
    fun testToggleIsolateNode() {
        val targetId = "NODE-PERGAMUS-DESKTOP"
        val initial = LinuxSentryManager.linuxNodes.value.first { it.nodeId == targetId }
        val initialIsolated = initial.isIsolated

        LinuxSentryManager.toggleIsolateNode(targetId)
        val toggled = LinuxSentryManager.linuxNodes.value.first { it.nodeId == targetId }
        assertEquals(!initialIsolated, toggled.isIsolated)
        assertEquals(if (!initialIsolated) NodeClusterStatus.ISOLATED else NodeClusterStatus.ONLINE, toggled.status)

        // Toggle back for clean test state
        LinuxSentryManager.toggleIsolateNode(targetId)
    }

    @Test
    fun testRegisterManualNodeAndRemove() {
        val testHost = "Raspberry-Pi-Sentry"
        LinuxSentryManager.registerManualNode(testHost, "192.168.1.210", "Raspberry Pi OS")

        val nodes = LinuxSentryManager.linuxNodes.value
        val found = nodes.firstOrNull { it.hostname == testHost }
        assertNotNull("Manually registered node should exist", found)
        assertEquals("Raspberry Pi OS", found?.distro)

        // Remove
        if (found != null) {
            LinuxSentryManager.removeNode(found.nodeId)
            val afterRemove = LinuxSentryManager.linuxNodes.value
            assertFalse("Node should be removed", afterRemove.any { it.nodeId == found.nodeId })
        }
    }
}
