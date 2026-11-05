package com.example.service

import com.example.data.model.LinuxEndpointAlert
import com.example.data.model.LinuxEndpointNode
import com.example.data.model.LinuxHubServerState
import com.example.data.model.NodeClusterStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object LinuxSentryManager {

    private val _linuxNodes = MutableStateFlow<List<LinuxEndpointNode>>(
        listOf(
            LinuxEndpointNode(
                nodeId = "NODE-PERGAMUS-DESKTOP",
                hostname = "Nodes-Pergamus",
                ipAddress = "192.168.1.55",
                macAddress = "D8:3A:DD:99:A1:01",
                distro = "Ubuntu 24.04 LTS (Noble Numbat)",
                kernelVersion = "6.8.0-45-generic",
                activeIface = "wlp3s0",
                isWifi = true,
                wifiSsid = "Office_Ultra_5G",
                wifiRssi = -48,
                activeThreatsCount = 0,
                isIsolated = false,
                activeFirewallRulesCount = 2,
                rogueListeners = emptyList(),
                subnetPeersCount = 6,
                lastSeenEpochMs = System.currentTimeMillis() - 2000L,
                status = NodeClusterStatus.ONLINE
            ),
            LinuxEndpointNode(
                nodeId = "LINUX-KALI-AUDIT",
                hostname = "Kali-Perimeter-Rig",
                ipAddress = "192.168.1.189",
                macAddress = "E4:5F:01:22:33:44",
                distro = "Kali GNU/Linux Rolling 2024.3",
                kernelVersion = "6.6.15-kali1-amd64",
                activeIface = "wlan0",
                isWifi = true,
                wifiSsid = "Office_Ultra_5G",
                wifiRssi = -58,
                activeThreatsCount = 1,
                isIsolated = false,
                activeFirewallRulesCount = 4,
                rogueListeners = listOf("Wireless ADB / TCP 5555"),
                subnetPeersCount = 8,
                lastSeenEpochMs = System.currentTimeMillis() - 12000L,
                status = NodeClusterStatus.ONLINE
            ),
            LinuxEndpointNode(
                nodeId = "LINUX-SERVER-PRIMARY",
                hostname = "GOIS-DataHub-Core",
                ipAddress = "192.168.1.10",
                macAddress = "00:1A:2B:EE:FF:11",
                distro = "Debian 12 Bookworm",
                kernelVersion = "6.1.0-21-amd64",
                activeIface = "eth0",
                isWifi = false,
                wifiSsid = "Wired Gigabit Core",
                wifiRssi = -30,
                activeThreatsCount = 0,
                isIsolated = false,
                activeFirewallRulesCount = 12,
                rogueListeners = emptyList(),
                subnetPeersCount = 14,
                lastSeenEpochMs = System.currentTimeMillis() - 5000L,
                status = NodeClusterStatus.HARDENED
            )
        )
    )
    val linuxNodes: StateFlow<List<LinuxEndpointNode>> = _linuxNodes.asStateFlow()

    private val _recentAlerts = MutableStateFlow<List<LinuxEndpointAlert>>(
        listOf(
            LinuxEndpointAlert(
                id = "ALERT-LX-1",
                nodeId = "LINUX-KALI-AUDIT",
                hostname = "Kali-Perimeter-Rig",
                title = "Rogue TCP Listener Detected",
                severity = "WARNING",
                detail = "Port 5555 listening on 0.0.0.0. Potential unauthorized wireless ADB or C2 socket.",
                timestamp = System.currentTimeMillis() - 360000L
            )
        )
    )
    val recentAlerts: StateFlow<List<LinuxEndpointAlert>> = _recentAlerts.asStateFlow()

    private val _hubState = MutableStateFlow(LinuxHubServerState())
    val hubState: StateFlow<LinuxHubServerState> = _hubState.asStateFlow()

    fun updateServerIp(ip: String) {
        _hubState.value = _hubState.value.copy(serverIp = ip)
    }

    fun setServerRunning(running: Boolean, port: Int = 8721, error: String? = null) {
        _hubState.value = _hubState.value.copy(
            isRunning = running,
            port = port,
            lastError = error
        )
    }

    fun ingestTelemetryJson(jsonString: String) {
        try {
            val root = JSONObject(jsonString)
            val hostname = root.optString("node_id", root.optString("hostname", "Linux-Endpoint"))
            val payload = root.optJSONObject("payload") ?: root

            val ip = payload.optString("ip", "127.0.0.1")
            val mac = payload.optString("mac", "00:00:00:00:00:00")
            val iface = payload.optString("iface", "wlan0")
            val osObj = payload.optJSONObject("os")
            val distro = osObj?.optString("distro", "Linux Workstation") ?: "Linux Workstation"
            val kernel = osObj?.optString("kernel", "6.x") ?: "6.x"

            val wifiObj = payload.optJSONObject("wifi")
            val isWifi = wifiObj?.optBoolean("is_wifi", true) ?: true
            val ssid = wifiObj?.optString("ssid", "Local Wi-Fi") ?: "Local Wi-Fi"
            val rssi = wifiObj?.optInt("rssi_dbm", -55) ?: -55

            val threatsCount = payload.optInt("active_threats_count", 0)
            val subnetCount = payload.optInt("subnet_device_count", 0)

            val listenersArray = payload.optJSONArray("listeners")
            val rogueList = mutableListOf<String>()
            if (listenersArray != null) {
                for (i in 0 until listenersArray.length()) {
                    val lObj = listenersArray.optJSONObject(i)
                    if (lObj?.optBoolean("is_rogue", false) == true) {
                        val port = lObj.optInt("port", 0)
                        val desc = lObj.optString("desc", "Rogue Listener")
                        rogueList.add("Port $port: $desc")
                    }
                }
            }

            val nodeId = "NODE-" + hostname.uppercase().replace(Regex("[^A-Z0-9-]"), "-")

            val updatedNode = LinuxEndpointNode(
                nodeId = nodeId,
                hostname = hostname,
                ipAddress = ip,
                macAddress = mac,
                distro = distro,
                kernelVersion = kernel,
                activeIface = iface,
                isWifi = isWifi,
                wifiSsid = ssid,
                wifiRssi = rssi,
                activeThreatsCount = threatsCount,
                isIsolated = false,
                activeFirewallRulesCount = payload.optJSONArray("firewall_blocked_macs")?.length() ?: 0,
                rogueListeners = rogueList,
                subnetPeersCount = subnetCount,
                lastSeenEpochMs = System.currentTimeMillis(),
                status = if (threatsCount > 0) NodeClusterStatus.ONLINE else NodeClusterStatus.HARDENED
            )

            // Update or insert
            val currentList = _linuxNodes.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.hostname.equals(hostname, ignoreCase = true) || it.nodeId == nodeId }
            if (existingIndex != -1) {
                currentList[existingIndex] = updatedNode
            } else {
                currentList.add(0, updatedNode)
            }
            _linuxNodes.value = currentList

            // If rogue listeners, add alert
            if (rogueList.isNotEmpty()) {
                val newAlert = LinuxEndpointAlert(
                    id = "ALERT-${System.currentTimeMillis()}",
                    nodeId = nodeId,
                    hostname = hostname,
                    title = "Rogue Sockets on $hostname",
                    severity = "CRITICAL",
                    detail = rogueList.joinToString(" • "),
                    timestamp = System.currentTimeMillis()
                )
                _recentAlerts.value = listOf(newAlert) + _recentAlerts.value.take(19)
            }

            _hubState.value = _hubState.value.copy(
                ingestedPayloadCount = _hubState.value.ingestedPayloadCount + 1,
                connectedNodesCount = _linuxNodes.value.size,
                lastIngestTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleIsolateNode(nodeId: String) {
        _linuxNodes.value = _linuxNodes.value.map { node ->
            if (node.nodeId == nodeId) {
                val newIsolated = !node.isIsolated
                node.copy(
                    isIsolated = newIsolated,
                    status = if (newIsolated) NodeClusterStatus.ISOLATED else NodeClusterStatus.ONLINE
                )
            } else node
        }
    }

    fun removeNode(nodeId: String) {
        _linuxNodes.value = _linuxNodes.value.filterNot { it.nodeId == nodeId }
        _hubState.value = _hubState.value.copy(connectedNodesCount = _linuxNodes.value.size)
    }

    fun registerManualNode(hostname: String, ip: String, distro: String) {
        val nodeId = "NODE-" + hostname.uppercase().replace(Regex("[^A-Z0-9-]"), "-")
        val newNode = LinuxEndpointNode(
            nodeId = nodeId,
            hostname = hostname,
            ipAddress = ip,
            macAddress = "D8:3A:DD:" + (10..99).random() + ":" + (10..99).random() + ":" + (10..99).random(),
            distro = distro,
            kernelVersion = "Linux 6.8.x",
            activeIface = "wlan0",
            status = NodeClusterStatus.ONLINE
        )
        _linuxNodes.value = listOf(newNode) + _linuxNodes.value.filterNot { it.nodeId == nodeId }
        _hubState.value = _hubState.value.copy(connectedNodesCount = _linuxNodes.value.size)
    }
}
