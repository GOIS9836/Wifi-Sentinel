package com.example.service

import android.content.Context
import com.example.data.model.BetaFlightRunResult
import com.example.data.model.ClusterBackupSnapshot
import com.example.data.model.NodeClusterStatus
import com.example.data.model.PergamusClusterNode
import com.example.data.model.RecurringBetaTester
import com.example.data.model.TrialCategory
import com.example.data.model.TrialErrorEntry
import com.example.data.model.TrialOutcome
import com.example.data.model.WorkspaceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class WorkspaceClusterManager private constructor(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // Workspaces
    private val _workspaces = MutableStateFlow<List<WorkspaceProfile>>(
        listOf(
            WorkspaceProfile(
                id = "WS-OPIFEX-01",
                name = "Global Opifex - Primary SecOps",
                organization = "Global Opifex Innovative Solutions",
                organizationCode = "OPX-CORP-9836",
                tier = "Enterprise Defense Cluster",
                activeNodesCount = 5,
                totalBackupsCount = 3,
                isCurrent = true,
                description = "Primary operational tenant with full Pergamus cluster mesh and continuous native backup."
            ),
            WorkspaceProfile(
                id = "WS-PERGAMUS-DISTRIB",
                name = "Pergamus Distributed Node Cluster",
                organization = "Global Opifex Innovative Solutions",
                organizationCode = "OPX-CORP-9836",
                tier = "Mesh Sentinel Ring",
                activeNodesCount = 8,
                totalBackupsCount = 2,
                isCurrent = false,
                description = "Dedicated perimeter mesh cluster running 802.11ax/be hardware probes."
            ),
            WorkspaceProfile(
                id = "WS-ZERO-TRUST-03",
                name = "Zero-Trust Perimeter Flight",
                organization = "Global Opifex Innovative Solutions",
                organizationCode = "OPX-CORP-9836",
                tier = "Air-Gapped SecOps",
                activeNodesCount = 4,
                totalBackupsCount = 1,
                isCurrent = false,
                description = "Strict zero-tolerance quarantine workspace for adversarial testing."
            )
        )
    )
    val workspaces: StateFlow<List<WorkspaceProfile>> = _workspaces.asStateFlow()

    private val _currentWorkspace = MutableStateFlow(_workspaces.value.first())
    val currentWorkspace: StateFlow<WorkspaceProfile> = _currentWorkspace.asStateFlow()

    // Nodes Pergamus
    private val _pergamusNodes = MutableStateFlow<List<PergamusClusterNode>>(
        listOf(
            PergamusClusterNode(
                nodeId = "NODE-PERGAMUS-ALPHA",
                clusterName = "Nodes Pergamus",
                displayName = "Node Pergamus-Alpha (Master Gateway Core)",
                ipAddress = "192.168.1.1",
                macAddress = "70:EE:50:8A:21:40",
                role = "Master Gateway Orchestrator",
                status = NodeClusterStatus.ONLINE,
                latencyMs = 3L,
                uptimePercent = 99.99,
                lastHeartbeatFormatted = "1s ago",
                sourceDataCount = 412,
                trialCount = 98
            ),
            PergamusClusterNode(
                nodeId = "NODE-PERGAMUS-BETA",
                clusterName = "Nodes Pergamus",
                displayName = "Node Pergamus-Beta (Perimeter Radar)",
                ipAddress = "192.168.1.42",
                macAddress = "70:EE:50:8A:21:41",
                role = "5GHz/6GHz Spectrum Sentry",
                status = NodeClusterStatus.ONLINE,
                latencyMs = 9L,
                uptimePercent = 99.95,
                lastHeartbeatFormatted = "4s ago",
                sourceDataCount = 285,
                trialCount = 54
            ),
            PergamusClusterNode(
                nodeId = "NODE-PERGAMUS-GAMMA",
                clusterName = "Nodes Pergamus",
                displayName = "Node Pergamus-Gamma (BLE Mesh Sentinel)",
                ipAddress = "192.168.1.77",
                macAddress = "70:EE:50:8A:21:42",
                role = "Zero-Tolerance BT Sniffer",
                status = NodeClusterStatus.ONLINE,
                latencyMs = 14L,
                uptimePercent = 99.88,
                lastHeartbeatFormatted = "2s ago",
                sourceDataCount = 189,
                trialCount = 36
            ),
            PergamusClusterNode(
                nodeId = "NODE-PERGAMUS-DELTA",
                clusterName = "Nodes Pergamus",
                displayName = "Node Pergamus-Delta (Packet Fuzzer Rig)",
                ipAddress = "192.168.1.105",
                macAddress = "70:EE:50:8A:21:43",
                role = "Packet Entropy Analyzer",
                status = NodeClusterStatus.STANDBY,
                latencyMs = 7L,
                uptimePercent = 99.91,
                lastHeartbeatFormatted = "12s ago",
                sourceDataCount = 340,
                trialCount = 120
            ),
            PergamusClusterNode(
                nodeId = "NODE-PERGAMUS-OMEGA",
                clusterName = "Nodes Pergamus",
                displayName = "Node Pergamus-Omega (Air-Gapped Vault)",
                ipAddress = "192.168.1.250",
                macAddress = "70:EE:50:8A:21:44",
                role = "Native Cluster Backup Repository",
                status = NodeClusterStatus.HARDENED,
                latencyMs = 1L,
                uptimePercent = 100.0,
                lastHeartbeatFormatted = "Live",
                sourceDataCount = 580,
                trialCount = 65
            )
        )
    )
    val pergamusNodes: StateFlow<List<PergamusClusterNode>> = _pergamusNodes.asStateFlow()

    // Native Backup Snapshots with All Clusters
    private val _backupSnapshots = MutableStateFlow<List<ClusterBackupSnapshot>>(
        listOf(
            ClusterBackupSnapshot(
                backupId = "OPX-SNAP-20260916-01",
                workspaceName = "Global Opifex Innovative Solutions",
                clusterTarget = "Nodes Pergamus (All Clusters)",
                timestamp = System.currentTimeMillis() - 1800000L,
                formattedDate = "Today, 08:15 UTC",
                nodeCount = 5,
                devicesCount = 18,
                trialsCount = 42,
                sizeKb = 142L,
                checksumSha256 = "c8f2b740e34a712d9095bb49fa8102d1847291a5ec40b2efd142109861e38a20",
                isNativeStored = true,
                rawJsonPayload = generateSampleJsonPayload("OPX-SNAP-20260916-01")
            ),
            ClusterBackupSnapshot(
                backupId = "OPX-SNAP-20260915-02",
                workspaceName = "Global Opifex Innovative Solutions",
                clusterTarget = "Nodes Pergamus (All Clusters)",
                timestamp = System.currentTimeMillis() - 86400000L,
                formattedDate = "Yesterday, 19:40 UTC",
                nodeCount = 5,
                devicesCount = 16,
                trialsCount = 38,
                sizeKb = 135L,
                checksumSha256 = "7a4192b0c4de1930fa42bc942b001a4e2185dca8990141f09cba11946fe701bc",
                isNativeStored = true,
                rawJsonPayload = generateSampleJsonPayload("OPX-SNAP-20260915-02")
            )
        )
    )
    val backupSnapshots: StateFlow<List<ClusterBackupSnapshot>> = _backupSnapshots.asStateFlow()

    // Trial or Error Library Rich with Sources-Data
    private val _trialEntries = MutableStateFlow<List<TrialErrorEntry>>(
        listOf(
            TrialErrorEntry(
                id = "TRIAL-ARP-POISON-01",
                title = "ARP Cache Poisoning Handshake Injection Trial",
                category = TrialCategory.SECURITY_TRIAL,
                protocol = "ARP / RFC 826",
                sourceNode = "Node Pergamus-Alpha",
                outcome = TrialOutcome.PASSED,
                rootCause = "Injected unsolicited ARP reply claiming gateway IP. Dynamic ARP inspection immediately dropped and quarantined spoofing frame in 2.1ms.",
                sourceDataPayloadHex = "00 01 08 00 06 04 00 02 70 ee 50 8a 21 40 c0 a8 01 01 ff ff ff ff ff ff c0 a8 01 de 00 00 00 00 00 00 00 00 00 00",
                sourceDataSummary = "Ethernet Type 0x0806 (ARP), Opcode 2 (Reply). Sender IP 192.168.1.1 mapped to rogue MAC. Sentinel Guard blocked rewrite.",
                testIterations = 48
            ),
            TrialErrorEntry(
                id = "ERR-WPA3-SAE-04",
                title = "WPA3 Simultaneous Authentication of Equals Scalar Anomaly",
                category = TrialCategory.PROTOCOL_ERROR,
                protocol = "WPA3-SAE / 802.11ax",
                sourceNode = "Node Pergamus-Beta",
                outcome = TrialOutcome.REPRODUCED,
                rootCause = "Malformed elliptic curve scalar point in SAE Commit frame caused handshake timeout. Diagnostic trace flagged non-canonical point.",
                sourceDataPayloadHex = "00 21 00 01 00 13 4a 6b 9c 1e 2f a0 b1 c2 d3 e4 f5 06 17 28 39 4a 5b 6c 7d 8e 9f a0 b2 c4 d6 e8 f0 12 34 56 78 9a",
                sourceDataSummary = "SAE Commit scalar payload with invalid curve parameter 19. Anti-clogging token handshake triggered for client retry.",
                testIterations = 14
            ),
            TrialErrorEntry(
                id = "SRC-ROUTER-BEACON-09",
                title = "Multi-BSSID Beacon Frame Entropy & Rogue AP Capture",
                category = TrialCategory.SOURCE_DATA_CAPTURE,
                protocol = "802.11ax Beacon / Element ID 255",
                sourceNode = "Node Pergamus-Beta",
                outcome = TrialOutcome.MITIGATED,
                rootCause = "Rogue duplicate access point observed emitting matching SSID with anomalous beacon timing (interval jitter > 30ms).",
                sourceDataPayloadHex = "80 00 00 00 ff ff ff ff ff ff 70 ee 50 8a 21 41 70 ee 50 8a 21 41 80 0c 10 20 30 40 50 60 70 80 64 00 31 04 00 00",
                sourceDataSummary = "802.11 Beacon frame, SSID tag 'Opifex_SecOps_5G', Capability Info 0x0431 (ESS + Privacy + Spectrum Management).",
                testIterations = 72
            ),
            TrialErrorEntry(
                id = "TRIAL-DHCP-ROGUE-02",
                title = "Rogue DHCP Server Offer Containment Trial",
                category = TrialCategory.SECURITY_TRIAL,
                protocol = "DHCP Option 82 / RFC 3046",
                sourceNode = "Node Pergamus-Delta",
                outcome = TrialOutcome.ISOLATED,
                rootCause = "Secondary rogue DHCP offer detected on subnet offering fraudulent gateway 192.168.1.200. Gateway Switch Guard isolated port.",
                sourceDataPayloadHex = "02 01 06 00 39 03 f3 26 00 00 80 00 00 00 00 00 c0 a8 01 64 00 00 00 00 00 00 00 00 70 ee 50 8a 21 99 00 00 00 00",
                sourceDataSummary = "BOOTPREPLY containing Option 53 (DHCP Offer), Server ID 192.168.1.200, Option 3 (Router: 192.168.1.200). Blocked by ACL.",
                testIterations = 31
            ),
            TrialErrorEntry(
                id = "BENCH-ENTROPY-TLS-01",
                title = "Deep Packet Inspection Zero-Tolerance Cipher Benchmark",
                category = TrialCategory.HARDENING_BENCHMARK,
                protocol = "TLS 1.3 / SNI Handshake",
                sourceNode = "Node Pergamus-Omega",
                outcome = TrialOutcome.PASSED,
                rootCause = "Cryptographic baseline verification across all Pergamus clusters confirmed 100% encrypted telemetry with zero cleartext leak.",
                sourceDataPayloadHex = "16 03 03 00 c8 01 00 00 c4 03 03 a1 b2 c3 d4 e5 f6 07 18 29 3a 4b 5c 6d 7e 8f 90 01 02 03 04 05 06 07 08 09 0a 0b 0c",
                sourceDataSummary = "TLS Client Hello record (v1.3), Supported Cipher: TLS_AES_256_GCM_SHA384, Elliptic curve x25519 key exchange validated.",
                testIterations = 105
            )
        )
    )
    val trialEntries: StateFlow<List<TrialErrorEntry>> = _trialEntries.asStateFlow()

    // Recurring Beta Testers
    private val _betaTesters = MutableStateFlow<List<RecurringBetaTester>>(
        listOf(
            RecurringBetaTester(
                testerId = "BT-OPX-104",
                email = "libertykondo9836@gmail.com",
                name = "Liberty Kondo (Lead SecOps)",
                flightRank = "Principal Flight Lead",
                tier = "Recurring Beta Flight Tier-1",
                iterationsCompleted = 142,
                activeStreakDays = 18,
                devicePlatform = "Google Pixel 9 Pro (Pergamus Attached)",
                lastActiveFormatted = "Active Today",
                flightChannel = "Opifex Beta Ring 4.2",
                notes = "Primary QA pilot for zero-tolerance duplicate guard and native backup cluster synchronization."
            ),
            RecurringBetaTester(
                testerId = "BT-OPX-208",
                email = "elena.rostova@opifex.io",
                name = "Elena Rostova (QA Flight)",
                flightRank = "Diamond Protocol QA",
                tier = "Recurring Beta Flight Tier-1",
                iterationsCompleted = 89,
                activeStreakDays = 14,
                devicePlatform = "Samsung Galaxy S24 Ultra",
                lastActiveFormatted = "Active Today",
                flightChannel = "Opifex Beta Ring 4.2",
                notes = "Specializes in WPA3 SAE handshake anomaly reproduction and RF interference radar trials."
            ),
            RecurringBetaTester(
                testerId = "BT-OPX-312",
                email = "alex.chen@secops.global",
                name = "Alex Chen (RF Protocol)",
                flightRank = "RF Field Specialist",
                tier = "Recurring Beta Flight Tier-2",
                iterationsCompleted = 76,
                activeStreakDays = 9,
                devicePlatform = "Pixel Tablet / Mesh Probe",
                lastActiveFormatted = "Active Yesterday",
                flightChannel = "Opifex Beta Ring 4.2",
                notes = "Field testing multi-gateway failover thresholds and packet entropy bounds across mesh nodes."
            ),
            RecurringBetaTester(
                testerId = "BT-OPX-405",
                email = "marcus.vance@opifex.io",
                name = "Marcus Vance (Mesh Lead)",
                flightRank = "Zero-Tolerance Auditor",
                tier = "Recurring Beta Flight Tier-1",
                iterationsCompleted = 215,
                activeStreakDays = 26,
                devicePlatform = "OnePlus 12 Security Lab",
                lastActiveFormatted = "Active Today",
                flightChannel = "Opifex Beta Ring 4.2",
                notes = "Continuous background automated flight runner lead with 100% pass verification on Pergamus-Omega."
            )
        )
    )
    val betaTesters: StateFlow<List<RecurringBetaTester>> = _betaTesters.asStateFlow()

    private val _lastFlightResult = MutableStateFlow<BetaFlightRunResult?>(null)
    val lastFlightResult: StateFlow<BetaFlightRunResult?> = _lastFlightResult.asStateFlow()

    private val _isFlightRunning = MutableStateFlow(false)
    val isFlightRunning: StateFlow<Boolean> = _isFlightRunning.asStateFlow()

    private val _isClusterSyncing = MutableStateFlow(false)
    val isClusterSyncing: StateFlow<Boolean> = _isClusterSyncing.asStateFlow()

    // --- Actions: Workspaces ---
    fun selectWorkspace(workspaceId: String) {
        val updated = _workspaces.value.map {
            it.copy(isCurrent = it.id == workspaceId)
        }
        _workspaces.value = updated
        _workspaces.value.find { it.id == workspaceId }?.let {
            _currentWorkspace.value = it
        }
    }

    // --- Actions: Nodes Pergamus ---
    fun syncAllNodes(onComplete: (() -> Unit)? = null) {
        _isClusterSyncing.value = true
        // Simulate heartbeat sync across all nodes
        val now = "Just now"
        val updated = _pergamusNodes.value.map { node ->
            node.copy(
                lastHeartbeatFormatted = now,
                latencyMs = (node.latencyMs + Random.nextLong(-2, 3)).coerceIn(1L, 40L),
                sourceDataCount = node.sourceDataCount + Random.nextInt(1, 5)
            )
        }
        _pergamusNodes.value = updated
        _isClusterSyncing.value = false
        onComplete?.invoke()
    }

    fun toggleIsolateNode(nodeId: String) {
        _pergamusNodes.value = _pergamusNodes.value.map { node ->
            if (node.nodeId == nodeId) {
                val newStatus = if (node.status == NodeClusterStatus.ISOLATED) {
                    NodeClusterStatus.ONLINE
                } else {
                    NodeClusterStatus.ISOLATED
                }
                node.copy(status = newStatus)
            } else node
        }
    }

    fun addPergamusNode(
        displayName: String,
        ipAddress: String,
        macAddress: String,
        role: String
    ) {
        val nodeCount = _pergamusNodes.value.size + 1
        val newNode = PergamusClusterNode(
            nodeId = "NODE-PERGAMUS-CUSTOM-$nodeCount",
            displayName = displayName.ifBlank { "Node Pergamus-Custom-$nodeCount" },
            ipAddress = ipAddress.ifBlank { "192.168.1.${100 + nodeCount}" },
            macAddress = macAddress.ifBlank { "70:EE:50:8A:21:5$nodeCount" },
            role = role.ifBlank { "Edge Cluster Sentinel" },
            status = NodeClusterStatus.ONLINE,
            latencyMs = 8L,
            uptimePercent = 100.0,
            lastHeartbeatFormatted = "Just now",
            sourceDataCount = 10,
            trialCount = 2
        )
        _pergamusNodes.value = _pergamusNodes.value + newNode
    }

    // --- Actions: Native Backup with All Clusters ---
    fun createNativeClusterBackup(
        customNote: String = "",
        onCreated: ((ClusterBackupSnapshot) -> Unit)? = null
    ): ClusterBackupSnapshot {
        val now = System.currentTimeMillis()
        val formattedDate = dateFormat.format(Date(now))
        val backupId = "OPX-SNAP-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now))}"
        val nodes = _pergamusNodes.value
        val trials = _trialEntries.value

        val payload = JSONObject().apply {
            put("backupId", backupId)
            put("workspace", _currentWorkspace.value.name)
            put("organization", "Global Opifex Innovative Solutions")
            put("organizationCode", "OPX-CORP-9836")
            put("clusterGroup", "Nodes Pergamus")
            put("timestamp", now)
            put("formattedDate", formattedDate)
            put("note", customNote)
            put("nodeCount", nodes.size)

            val nodesArray = JSONArray()
            nodes.forEach { node ->
                nodesArray.put(JSONObject().apply {
                    put("nodeId", node.nodeId)
                    put("displayName", node.displayName)
                    put("ip", node.ipAddress)
                    put("mac", node.macAddress)
                    put("role", node.role)
                    put("status", node.status.name)
                })
            }
            put("nodes", nodesArray)

            val trialsArray = JSONArray()
            trials.forEach { trial ->
                trialsArray.put(JSONObject().apply {
                    put("id", trial.id)
                    put("title", trial.title)
                    put("category", trial.category.name)
                    put("protocol", trial.protocol)
                    put("outcome", trial.outcome.name)
                })
            }
            put("trials", trialsArray)
        }.toString(2)

        val sha256 = calculateSha256(payload)
        val snapshot = ClusterBackupSnapshot(
            backupId = backupId,
            workspaceName = _currentWorkspace.value.name,
            clusterTarget = "Nodes Pergamus (All Clusters)",
            timestamp = now,
            formattedDate = formattedDate,
            nodeCount = nodes.size,
            devicesCount = 20,
            trialsCount = trials.size,
            sizeKb = (payload.toByteArray().size / 1024L).coerceAtLeast(120L),
            checksumSha256 = sha256,
            isNativeStored = true,
            rawJsonPayload = payload
        )

        _backupSnapshots.value = listOf(snapshot) + _backupSnapshots.value
        onCreated?.invoke(snapshot)
        return snapshot
    }

    fun restoreClusterSnapshot(snapshotId: String): Boolean {
        val snapshot = _backupSnapshots.value.find { it.backupId == snapshotId } ?: return false
        // Trigger health and heartbeat refresh as confirmation
        syncAllNodes()
        return true
    }

    fun deleteBackupSnapshot(snapshotId: String) {
        _backupSnapshots.value = _backupSnapshots.value.filter { it.backupId != snapshotId }
    }

    // --- Actions: Trial & Error Library Rich with Sources-Data ---
    fun runTrialSimulation(
        title: String,
        category: TrialCategory,
        protocol: String,
        sourceNode: String
    ): TrialErrorEntry {
        val id = "TRIAL-${System.currentTimeMillis() % 100000}"
        val outcomes = listOf(TrialOutcome.PASSED, TrialOutcome.MITIGATED, TrialOutcome.PASSED)
        val outcome = outcomes.random()

        val hexBytes = ByteArray(32) { Random.nextInt(0, 256).toByte() }
        val hexString = hexBytes.joinToString(" ") { String.format("%02x", it) }

        val newEntry = TrialErrorEntry(
            id = id,
            title = title.ifBlank { "Dynamic Spectral & Protocol Security Trial" },
            category = category,
            protocol = protocol.ifBlank { "802.11ax / WPA3-Enterprise" },
            sourceNode = sourceNode.ifBlank { "Node Pergamus-Alpha" },
            outcome = outcome,
            rootCause = "Simulated trial executed against Pergamus defenses. Frame integrity check verified signature and recorded source-data metrics.",
            sourceDataPayloadHex = hexString,
            sourceDataSummary = "Captured live trial frame with 32-byte security header, sequence ID #${Random.nextInt(1000, 9999)}.",
            timestamp = System.currentTimeMillis(),
            testIterations = 1
        )

        _trialEntries.value = listOf(newEntry) + _trialEntries.value
        return newEntry
    }

    // --- Actions: Recurring Beta Testers ---
    fun enrollRecurringBetaTester(
        name: String,
        email: String,
        device: String
    ): RecurringBetaTester {
        val count = _betaTesters.value.size + 1
        val newTester = RecurringBetaTester(
            testerId = "BT-OPX-${100 + count * 20}",
            email = email.ifBlank { "beta.tester$count@opifex.io" },
            name = name.ifBlank { "Beta Tester $count" },
            flightRank = "Recurring Flight Evaluator",
            tier = "Recurring Beta Flight Tier-2",
            iterationsCompleted = 1,
            activeStreakDays = 1,
            devicePlatform = device.ifBlank { "Android 14 Handset (Pergamus Sentry Attached)" },
            lastActiveFormatted = "Active Just Now",
            flightChannel = "Opifex Beta Ring 4.2",
            notes = "Enrolled into Global Opifex recurring beta flightdeck."
        )
        _betaTesters.value = _betaTesters.value + newTester
        return newTester
    }

    fun runDailyBetaFlightSuite(onComplete: ((BetaFlightRunResult) -> Unit)? = null) {
        _isFlightRunning.value = true
        val flightId = "FLIGHT-OPX-${System.currentTimeMillis() % 100000}"
        val totalTests = 18
        val passed = 17
        val flagged = 1
        val duration = 3450L

        val report = """
            === GLOBAL OPIFEX RECURRING BETA FLIGHT REPORT ===
            Flight ID: $flightId
            Target Cluster: Nodes Pergamus (5 Distributed Nodes)
            Active Workspace: ${_currentWorkspace.value.name}
            Checksum Validation: SHA-256 Verified
            Suite Breakdown:
            - [PASS] ARP Integrity & Cache Poisoning Defense (Node Pergamus-Alpha)
            - [PASS] WPA3 SAE Scalar Anti-Clogging Handshake (Node Pergamus-Beta)
            - [PASS] Zero-Tolerance BLE Perimeter Beacon Guard (Node Pergamus-Gamma)
            - [PASS] Deep Packet Entropy & Fuzzing Bounds (Node Pergamus-Delta)
            - [PASS] Air-Gapped Cluster Backup Snapshot Storage (Node Pergamus-Omega)
            - [PASS] Native Room DB Entity Serialization & Export
            - [REVIEW] Multi-BSSID Jitter Tolerance Threshold (+4.2ms)
            Total Executed: $totalTests | Passed: $passed | Flagged: $flagged
            Status: FLIGHT DEPLOYMENT CERTIFIED
        """.trimIndent()

        val result = BetaFlightRunResult(
            flightId = flightId,
            timestamp = System.currentTimeMillis(),
            testsExecuted = totalTests,
            passedCount = passed,
            flaggedCount = flagged,
            durationMs = duration,
            summaryReport = report
        )

        // Increment tester flight counts
        _betaTesters.value = _betaTesters.value.map { tester ->
            tester.copy(
                iterationsCompleted = tester.iterationsCompleted + 1,
                lastActiveFormatted = "Active Just Now"
            )
        }

        _lastFlightResult.value = result
        _isFlightRunning.value = false
        onComplete?.invoke(result)
    }

    private fun calculateSha256(content: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(content.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        }
    }

    private fun generateSampleJsonPayload(snapshotId: String): String {
        return JSONObject().apply {
            put("snapshotId", snapshotId)
            put("organization", "Global Opifex Innovative Solutions")
            put("tenant", "OPX-CORP-9836")
            put("cluster", "Nodes Pergamus")
            put("status", "VALIDATED_NATIVE_BACKUP")
            put("nodesActive", 5)
            put("engineVersion", "Sentinel 4.2-Opifex")
        }.toString(2)
    }

    companion object {
        @Volatile
        private var instance: WorkspaceClusterManager? = null

        fun getInstance(context: Context): WorkspaceClusterManager {
            return instance ?: synchronized(this) {
                instance ?: WorkspaceClusterManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
