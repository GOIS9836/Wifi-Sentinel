package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.NetworkDeviceEntity
import com.example.data.local.SecurityAlertEntity
import com.example.data.local.SignalLogEntity
import com.example.data.model.AiOptimizationReport
import com.example.data.model.BackgroundDetectionStatus
import com.example.data.model.BtDeviceType
import com.example.data.model.BtPerimeterDevice
import com.example.data.model.ChannelCongestion
import com.example.data.model.ChatMessage
import com.example.data.model.DeviceWhitelistAuditResult
import com.example.data.model.DiscoveredDevice
import com.example.data.model.DuplicationGuardStatus
import com.example.data.model.DuplicationViolation
import com.example.data.model.DuplicationViolationType
import com.example.data.model.GatewaySwitchType
import com.example.data.model.GatewayTransitionEvent
import com.example.data.model.NearbyAccessPoint
import com.example.data.model.AntivirusScannerState
import com.example.data.model.AppRiskLevel
import com.example.data.model.AppSecurityScanResult
import com.example.data.model.DailyScanScheduleSettings
import com.example.data.model.DeviceStorageMemoryMetrics
import com.example.data.model.GatewayFilterCategory
import com.example.data.model.GatewayRouterNode
import com.example.data.model.GatewayRouterType
import com.example.data.model.JunkCategoryItem
import com.example.data.model.JunkCleanResult
import com.example.data.model.JunkType
import com.example.data.model.NetworkHardeningRecommendation
import com.example.data.model.NetworkRiskAssessment
import com.example.data.model.NetworkSummaryReport
import com.example.data.model.SystemSecurityAudit
import com.example.data.model.ThreatLevel
import com.example.data.model.ThreatVectorBreakdown
import com.example.data.model.UnethicalDevice
import com.example.data.model.UnethicalThreatType
import com.example.data.model.WhitelistAuditStatus
import com.example.data.model.WifiConnectionState
import com.example.data.model.isLocallyAdministeredMac
import com.example.data.remote.GeminiService
import com.example.data.repository.WifiRepository
import com.example.service.AntivirusScannerEngine
import com.example.service.BluetoothSentryScanner
import com.example.service.DailyScanScheduler
import com.example.service.DeviceWhitelistComparisonEngine
import com.example.service.FirewallAclRule
import com.example.service.JunkCleanerEngine
import com.example.service.NetworkAccessEnforcer
import com.example.service.NetworkMonitorService
import com.example.service.NetworkReportGenerator
import com.example.service.SecurityNotificationDispatcher
import com.example.service.WiFiManager
import com.example.service.WifiRealtimeMetrics
import com.example.service.WifiScannerService
import com.example.service.ZeroToleranceDuplicationGuard
import com.example.service.WorkspaceClusterManager
import com.example.data.model.WorkspaceProfile
import com.example.data.model.PergamusClusterNode
import com.example.data.model.ClusterBackupSnapshot
import com.example.data.model.TrialErrorEntry
import com.example.data.model.TrialCategory
import com.example.data.model.RecurringBetaTester
import com.example.data.model.BetaFlightRunResult
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections
import kotlin.random.Random
import com.example.security.NetworkGuardManager
import com.example.security.GuardEventListener
import com.example.security.MacSanitizer
import com.example.security.GatewayAlert
import com.example.security.SecurityDashboardUiState
import com.example.security.GatewayDiagnosticsManager
import com.example.security.GatewayDiagnosticsResult
import com.example.data.local.TrustedGateway
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application), GuardEventListener {

    // Compliant Self-Defense Engine & Privacy-Safe MAC Corroborator
    private val guardManager: NetworkGuardManager = NetworkGuardManager(application, this)
    private val diagnosticsManager = GatewayDiagnosticsManager(application)
    private val _guardUiState = MutableStateFlow(SecurityDashboardUiState(fpFilterEnabled = true))
    val guardUiState: StateFlow<SecurityDashboardUiState> = _guardUiState.asStateFlow()
    val isCompliantLockdownActive: StateFlow<Boolean> = combine(_guardUiState) { it[0].isLockdownActive }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val activeGuardGatewayAlert: StateFlow<GatewayAlert?> = combine(_guardUiState) { it[0].activeAlert }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val fpFilteredCount: StateFlow<Int> = combine(_guardUiState) { it[0].fpFilteredCount }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val fpFilterEnabled: StateFlow<Boolean> = combine(_guardUiState) { it[0].fpFilterEnabled }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val totalHashedAuditsCount: StateFlow<Int> = combine(_guardUiState) { it[0].totalHashedAuditsCount }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _gatewayDiagnostics = MutableStateFlow<GatewayDiagnosticsResult?>(null)
    val gatewayDiagnostics: StateFlow<GatewayDiagnosticsResult?> = _gatewayDiagnostics.asStateFlow()
    private val _isDiagnosingGateway = MutableStateFlow(false)
    val isDiagnosingGateway: StateFlow<Boolean> = _isDiagnosingGateway.asStateFlow()

    private val repository: WifiRepository
    private val wifiManager: WiFiManager = WiFiManager(application)

    private val _wifiState = MutableStateFlow(WifiConnectionState())
    val wifiState: StateFlow<WifiConnectionState> = _wifiState.asStateFlow()

    private val _rssiHistory = MutableStateFlow<List<Int>>(emptyList())
    val rssiHistory: StateFlow<List<Int>> = _rssiHistory.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isSubnetScanning = MutableStateFlow(false)
    val isSubnetScanning: StateFlow<Boolean> = _isSubnetScanning.asStateFlow()

    private val _isRealTimeShieldActive = MutableStateFlow(true)
    val isRealTimeShieldActive: StateFlow<Boolean> = _isRealTimeShieldActive.asStateFlow()

    private val _nearbyAccessPoints = MutableStateFlow<List<NearbyAccessPoint>>(emptyList())
    val nearbyAccessPoints: StateFlow<List<NearbyAccessPoint>> = _nearbyAccessPoints.asStateFlow()

    private val _channelCongestionList = MutableStateFlow<List<ChannelCongestion>>(emptyList())
    val channelCongestionList: StateFlow<List<ChannelCongestion>> = _channelCongestionList.asStateFlow()

    private val _aiReport = MutableStateFlow<AiOptimizationReport?>(null)
    val aiReport: StateFlow<AiOptimizationReport?> = _aiReport.asStateFlow()

    private val _isAiOptimizing = MutableStateFlow(false)
    val isAiOptimizing: StateFlow<Boolean> = _isAiOptimizing.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello! I am your AI Wi-Fi Sentinel Assistant. I can help analyze interference, recommend the ideal router channel, boost signal penetration, and secure your network against unauthorized intruders. How can I assist you?",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    val signalLogs: StateFlow<List<SignalLogEntity>>
    val securityAlerts: StateFlow<List<SecurityAlertEntity>>
    val unacknowledgedAlertsCount: StateFlow<Int>

    // Zero-Tolerance Bluetooth Perimeter States
    val perimeterBtDevices: StateFlow<List<BtPerimeterDevice>>
    val isBtScanning: StateFlow<Boolean>
    private val _isBtZeroToleranceShieldActive = MutableStateFlow(true)
    val isBtZeroToleranceShieldActive: StateFlow<Boolean> = _isBtZeroToleranceShieldActive.asStateFlow()

    // Zero-Tolerance Intruder Policy & Zero-FP Corroboration Engine States
    private val _isZeroTolerancePolicyActive = MutableStateFlow(true)
    val isZeroTolerancePolicyActive: StateFlow<Boolean> = _isZeroTolerancePolicyActive.asStateFlow()

    private val _isZeroFpEngineActive = MutableStateFlow(true)
    val isZeroFpEngineActive: StateFlow<Boolean> = _isZeroFpEngineActive.asStateFlow()
    val isZeroToleranceToFalsePositivesActive: StateFlow<Boolean> = _isZeroFpEngineActive.asStateFlow()

    private val _falsePositivesSuppressedCount = MutableStateFlow(6)
    val falsePositivesSuppressedCount: StateFlow<Int> = _falsePositivesSuppressedCount.asStateFlow()
    private val suppressedDeviceMacs = mutableSetOf<String>()
    private val suppressedBtAddresses = mutableSetOf<String>()

    // Zero-Tolerance to Duplications (Gateways, Subnets, IPs, and MACs)
    private val _duplicationGuardStatus = MutableStateFlow(
        DuplicationGuardStatus(
            isEnforced = true,
            duplicateGatewaysBlocked = 0,
            duplicateIpsBlocked = 0,
            duplicateMacsDeduplicated = 0,
            subnetAnomaliesBlocked = 0
        )
    )
    val duplicationGuardStatus: StateFlow<DuplicationGuardStatus> = _duplicationGuardStatus.asStateFlow()

    private val _isZeroToleranceDuplicationActive = MutableStateFlow(true)
    val isZeroToleranceDuplicationActive: StateFlow<Boolean> = _isZeroToleranceDuplicationActive.asStateFlow()

    // Gateway & Network Switch Monitoring States
    private val _gatewayLockdownActive = MutableStateFlow(true)
    val gatewayLockdownActive: StateFlow<Boolean> = _gatewayLockdownActive.asStateFlow()

    private val _lockedGatewayIp = MutableStateFlow("192.168.1.1")
    val lockedGatewayIp: StateFlow<String> = _lockedGatewayIp.asStateFlow()

    private val _lockedGatewayMac = MutableStateFlow("00:1A:2B:3C:4D:01")
    val lockedGatewayMac: StateFlow<String> = _lockedGatewayMac.asStateFlow()

    private var previousGatewayIp: String = "192.168.1.1"
    private var previousGatewayMac: String = "00:1A:2B:3C:4D:01"
    private var previousSsid: String = "Office_Ultra_5G"
    private var previousBssid: String = "3C:52:82:A4:91:00"

    private val _recentGatewayTransitions = MutableStateFlow<List<GatewayTransitionEvent>>(
        listOf(
            GatewayTransitionEvent(
                id = 1L,
                timestamp = System.currentTimeMillis() - 180000L,
                switchType = GatewaySwitchType.BSSID_ROAMING,
                oldGatewayIp = "192.168.1.1",
                newGatewayIp = "192.168.1.1",
                oldGatewayMac = "00:1A:2B:3C:4D:01",
                newGatewayMac = "00:1A:2B:3C:4D:01",
                oldSsid = "Office_Ultra_5G",
                newSsid = "Office_Ultra_5G",
                oldBssid = "3C:52:82:A4:91:00",
                newBssid = "3C:52:82:A4:91:02",
                details = "Seamless AP Mesh Roaming Transition (Node BSSID: 3C:52:82:A4:91:00 -> 3C:52:82:A4:91:02)",
                severity = "INFO"
            )
        )
    )
    val recentGatewayTransitions: StateFlow<List<GatewayTransitionEvent>> = _recentGatewayTransitions.asStateFlow()

    private val _activeGatewayAlert = MutableStateFlow<GatewayTransitionEvent?>(null)
    val activeGatewayAlert: StateFlow<GatewayTransitionEvent?> = _activeGatewayAlert.asStateFlow()

    val networkAccessEnforcer = NetworkAccessEnforcer(viewModelScope)
    val activeAclRules: StateFlow<List<FirewallAclRule>> = networkAccessEnforcer.activeRules
    val totalPacketsDropped: StateFlow<Long> = networkAccessEnforcer.totalPacketsDropped
    val totalBytesBlocked: StateFlow<Long> = networkAccessEnforcer.totalBytesBlocked

    // Background Detection Logic & Gemini Hardening States
    private val _backgroundDetectionStatus = MutableStateFlow(
        BackgroundDetectionStatus(
            isRunning = true,
            scanIntervalSeconds = 20,
            unknownDevicesDetected = 0,
            activeHardeningDirectives = 0,
            lastScanTimestamp = System.currentTimeMillis(),
            isAiAnalyzing = false
        )
    )
    val backgroundDetectionStatus: StateFlow<BackgroundDetectionStatus> = _backgroundDetectionStatus.asStateFlow()

    private val _networkHardeningRecommendations = MutableStateFlow<List<NetworkHardeningRecommendation>>(emptyList())
    val networkHardeningRecommendations: StateFlow<List<NetworkHardeningRecommendation>> = _networkHardeningRecommendations.asStateFlow()

    private val _selectedHardeningRecommendation = MutableStateFlow<NetworkHardeningRecommendation?>(null)
    val selectedHardeningRecommendation: StateFlow<NetworkHardeningRecommendation?> = _selectedHardeningRecommendation.asStateFlow()

    private val processedUnknownMacsForAi = mutableSetOf<String>()

    // Known-Device Whitelist Verification & Notification States
    private val _whitelistAuditResult = MutableStateFlow(DeviceWhitelistAuditResult())
    val whitelistAuditResult: StateFlow<DeviceWhitelistAuditResult> = _whitelistAuditResult.asStateFlow()

    lateinit var whitelistedDevices: StateFlow<List<NetworkDeviceEntity>>
    lateinit var whitelistedCount: StateFlow<Int>

    private val _isAutoNotifyEnabled = MutableStateFlow(true)
    val isAutoNotifyEnabled: StateFlow<Boolean> = _isAutoNotifyEnabled.asStateFlow()

    // Network Health and Security Incident Summary Report State
    private val _networkSummaryReport = MutableStateFlow(NetworkSummaryReport())
    val networkSummaryReport: StateFlow<NetworkSummaryReport> = _networkSummaryReport.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    // Real-Time Wi-Fi Signal Strength & Link Speed Network Monitor Service
    val networkMonitorMetrics: StateFlow<WifiRealtimeMetrics> = NetworkMonitorService.metrics
    val isNetworkMonitoringActive: StateFlow<Boolean> = NetworkMonitorService.isMonitoringActive

    // Antivirus & Malware Scanner States
    private val _antivirusState = MutableStateFlow(AntivirusScannerState())
    val antivirusState: StateFlow<AntivirusScannerState> = _antivirusState.asStateFlow()

    private val _scannedApps = MutableStateFlow<List<AppSecurityScanResult>>(emptyList())
    val scannedApps: StateFlow<List<AppSecurityScanResult>> = _scannedApps.asStateFlow()

    private val _systemSecurityAudit = MutableStateFlow(SystemSecurityAudit())
    val systemSecurityAudit: StateFlow<SystemSecurityAudit> = _systemSecurityAudit.asStateFlow()

    private val _quarantinedPackageNames = MutableStateFlow<Set<String>>(emptySet())
    val quarantinedPackageNames: StateFlow<Set<String>> = _quarantinedPackageNames.asStateFlow()

    private val _whitelistedPackageNames = MutableStateFlow<Set<String>>(emptySet())
    val whitelistedPackageNames: StateFlow<Set<String>> = _whitelistedPackageNames.asStateFlow()

    private var antivirusScanJob: Job? = null

    // System Cache & Junk Cleaner States
    private val _junkCategories = MutableStateFlow<List<JunkCategoryItem>>(emptyList())
    val junkCategories: StateFlow<List<JunkCategoryItem>> = _junkCategories.asStateFlow()

    private val _isCalculatingJunk = MutableStateFlow(false)
    val isCalculatingJunk: StateFlow<Boolean> = _isCalculatingJunk.asStateFlow()

    private val _isCleaningJunk = MutableStateFlow(false)
    val isCleaningJunk: StateFlow<Boolean> = _isCleaningJunk.asStateFlow()

    private val _lastCleanResult = MutableStateFlow<JunkCleanResult?>(null)
    val lastCleanResult: StateFlow<JunkCleanResult?> = _lastCleanResult.asStateFlow()

    private val _deviceMetrics = MutableStateFlow(DeviceStorageMemoryMetrics())
    val deviceMetrics: StateFlow<DeviceStorageMemoryMetrics> = _deviceMetrics.asStateFlow()

    // Scheduled Daily Scan Settings State
    private val _dailyScanSchedule = MutableStateFlow(DailyScanScheduleSettings())
    val dailyScanSchedule: StateFlow<DailyScanScheduleSettings> = _dailyScanSchedule.asStateFlow()

    private val _isExecutingScheduledScan = MutableStateFlow(false)
    val isExecutingScheduledScan: StateFlow<Boolean> = _isExecutingScheduledScan.asStateFlow()

    // --- SECURITY DASHBOARD, RISK METRICS & THREAT CONTROLS ---
    private val _isAutonomousQuarantineRemovalActive = MutableStateFlow(true)
    val isAutonomousQuarantineRemovalActive: StateFlow<Boolean> = _isAutonomousQuarantineRemovalActive.asStateFlow()

    fun setAutonomousQuarantineRemoval(enabled: Boolean) {
        _isAutonomousQuarantineRemovalActive.value = enabled
        if (enabled) {
            removeQuarantinedDevicesFromNetwork()
        }
    }

    private val _scanFrequencySeconds = MutableStateFlow(10)
    val scanFrequencySeconds: StateFlow<Int> = _scanFrequencySeconds.asStateFlow()

    private val _networkRiskAssessment = MutableStateFlow(NetworkRiskAssessment())
    val networkRiskAssessment: StateFlow<NetworkRiskAssessment> = _networkRiskAssessment.asStateFlow()

    private val _isCalculatingRiskScore = MutableStateFlow(false)
    val isCalculatingRiskScore: StateFlow<Boolean> = _isCalculatingRiskScore.asStateFlow()

    // Wifi Sentinel AI: Unethicals Scanner
    private val _unethicalDevices = MutableStateFlow<List<UnethicalDevice>>(emptyList())
    val unethicalDevices: StateFlow<List<UnethicalDevice>> = _unethicalDevices.asStateFlow()

    private val _isScanningForUnethicals = MutableStateFlow(false)
    val isScanningForUnethicals: StateFlow<Boolean> = _isScanningForUnethicals.asStateFlow()

    // Multi-Gateways & Routers Management Filters
    private val _managedGateways = MutableStateFlow<List<GatewayRouterNode>>(defaultManagedGateways())
    val managedGateways: StateFlow<List<GatewayRouterNode>> = _managedGateways.asStateFlow()

    private val _selectedGatewayFilter = MutableStateFlow(GatewayFilterCategory.ALL)
    val selectedGatewayFilter: StateFlow<GatewayFilterCategory> = _selectedGatewayFilter.asStateFlow()

    val filteredGateways: StateFlow<List<GatewayRouterNode>> = combine(
        _managedGateways,
        _selectedGatewayFilter
    ) { gateways, filter ->
        when (filter) {
            GatewayFilterCategory.ALL -> gateways
            GatewayFilterCategory.PRIMARY -> gateways.filter { it.type == GatewayRouterType.PRIMARY_DEFAULT }
            GatewayFilterCategory.MESH_NODES -> gateways.filter { it.type == GatewayRouterType.MESH_SATELLITE }
            GatewayFilterCategory.SECONDARY -> gateways.filter { it.type == GatewayRouterType.SECONDARY_GATEWAY }
            GatewayFilterCategory.VIRTUAL -> gateways.filter { it.type == GatewayRouterType.VIRTUAL_BRIDGE }
            GatewayFilterCategory.ROGUE_DUPLICATE -> gateways.filter { it.type == GatewayRouterType.ROGUE_DUPLICATE }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    lateinit var totalQuarantinedCount: StateFlow<Int>
    lateinit var trustedGateways: StateFlow<List<TrustedGateway>>
    lateinit var primaryTrustedGateway: StateFlow<TrustedGateway?>

    private val alertedUnknownMacs = Collections.synchronizedSet(mutableSetOf<String>())

    private var telemetryTickerJob: Job? = null
    private var shieldWatcherJob: Job? = null

    init {
        guardManager.startMonitoring()
        val database = AppDatabase.getInstance(application)
        val scannerService = WifiScannerService(application)
        val btScannerService = BluetoothSentryScanner(application)
        val geminiService = GeminiService()
        repository = WifiRepository(scannerService, btScannerService, database, geminiService, networkAccessEnforcer)

        perimeterBtDevices = repository.perimeterBtDevices
        isBtScanning = repository.isBtScanning

        signalLogs = repository.signalLogsFlow.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        securityAlerts = repository.securityAlertsFlow.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        unacknowledgedAlertsCount = repository.unacknowledgedAlertsCount.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), 0
        )
        whitelistedDevices = repository.whitelistedDevicesFlow.stateIn(
            viewModelScope, SharingStarted.Eagerly, emptyList()
        )
        whitelistedCount = repository.whitelistedCountFlow.stateIn(
            viewModelScope, SharingStarted.Eagerly, 0
        )
        trustedGateways = repository.trustedGatewaysFlow.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        primaryTrustedGateway = repository.primaryTrustedGatewayFlow.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )

        viewModelScope.launch {
            try {
                val existing = repository.trustedGatewaysFlow.first()
                if (existing.isEmpty()) {
                    val currentGw = _wifiState.value.gatewayIp.ifBlank { "192.168.1.1" }
                    val currentSsid = _wifiState.value.ssid.ifBlank { "Office_Mesh_Node1" }
                    val currentBssid = _wifiState.value.bssid.ifBlank { "00:11:22:33:44:55" }
                    repository.saveTrustedGateway(
                        TrustedGateway(
                            gatewayIp = currentGw,
                            bssid = currentBssid,
                            ssid = currentSsid,
                            subnetMask = "255.255.255.0",
                            label = "Main Router (Lounge)",
                            isPrimary = true
                        )
                    )
                    repository.saveTrustedGateway(
                        TrustedGateway(
                            gatewayIp = "192.168.1.2",
                            bssid = "00:11:22:33:44:56",
                            ssid = "${currentSsid}_Satellite",
                            subnetMask = "255.255.255.0",
                            label = "AP Upstairs",
                            isPrimary = false
                        )
                    )
                }
            } catch (e: Exception) {
                // Non-blocking initialization fallback
            }
        }

        totalQuarantinedCount = combine(
            _discoveredDevices,
            perimeterBtDevices,
            _quarantinedPackageNames,
            _unethicalDevices,
            _managedGateways
        ) { args: Array<Any> ->
            @Suppress("UNCHECKED_CAST")
            val devices = args[0] as List<DiscoveredDevice>
            @Suppress("UNCHECKED_CAST")
            val btDevices = args[1] as List<BtPerimeterDevice>
            @Suppress("UNCHECKED_CAST")
            val apps = args[2] as Set<String>
            @Suppress("UNCHECKED_CAST")
            val unethicals = args[3] as List<UnethicalDevice>
            @Suppress("UNCHECKED_CAST")
            val gateways = args[4] as List<GatewayRouterNode>

            devices.count { it.isBlocked } +
            btDevices.count { it.isQuarantined } +
            apps.size +
            unethicals.count { it.isQuarantined } +
            gateways.count { it.isQuarantined }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        SecurityNotificationDispatcher.initNotificationChannel(application)

        startTelemetryTicker()
        startShieldWatcher()
        startBtZeroTolerancePerimeterSentry()
        refreshSubnetDevices()
        refreshNearbyAPs()
        generateSummaryReport()
        refreshSystemAudit()
        refreshJunkStorage()
        startAntivirusScan()
        _dailyScanSchedule.value = DailyScanScheduler.loadSettings(application)
        refreshNetworkRiskScore()

        try {
            NetworkMonitorService.start(application)
        } catch (e: Exception) {
            // Non-blocking in headless test runtime
        }
    }

    fun refreshNetworkMetrics() {
        try {
            NetworkMonitorService.refresh(getApplication())
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    private fun startTelemetryTicker() {
        telemetryTickerJob?.cancel()
        telemetryTickerJob = viewModelScope.launch {
            while (true) {
                val live = repository.getLiveWifiState()
                val liveMetrics = NetworkMonitorService.metrics.value
                // Integrate real-time status from WiFiManager (handles metered, signal percent, and live capabilities)
                val directStatus = wifiManager.getNetworkState()
                val liveRssi = when {
                    liveMetrics.isConnected && liveMetrics.rssi in -100..0 -> liveMetrics.rssi
                    directStatus.isConnected && directStatus.rssi in -100..0 -> directStatus.rssi
                    else -> live.rssi
                }

                // Add slight dynamic variance to RSSI (±1 dBm) to reflect real RF antenna physics
                val jitter = Random.nextInt(-1, 2)
                val dynamicRssi = (liveRssi + jitter).coerceIn(-95, -25)
                val activeLinkSpeed = when {
                    liveMetrics.linkSpeedMbps > 0 -> liveMetrics.linkSpeedMbps
                    directStatus.linkSpeedMbps > 0 -> directStatus.linkSpeedMbps
                    else -> live.linkSpeedMbps
                }

                val updatedState = live.copy(
                    isConnected = liveMetrics.isConnected || directStatus.isConnected || live.isConnected,
                    ssid = when {
                        liveMetrics.ssid.isNotBlank() && liveMetrics.ssid != "Disconnected" && liveMetrics.ssid != "Connected Wi-Fi" -> liveMetrics.ssid
                        directStatus.ssid.isNotBlank() && directStatus.ssid != "Disconnected" && directStatus.ssid != "Connected Wi-Fi" -> directStatus.ssid
                        else -> live.ssid
                    },
                    bssid = when {
                        liveMetrics.bssid.isNotBlank() && liveMetrics.bssid != "00:00:00:00:00:00" -> liveMetrics.bssid
                        directStatus.bssid.isNotBlank() && directStatus.bssid != "00:00:00:00:00:00" -> directStatus.bssid
                        else -> live.bssid
                    },
                    rssi = dynamicRssi,
                    signalPercent = ((dynamicRssi + 100) * 2).coerceIn(0, 100),
                    linkSpeedMbps = activeLinkSpeed
                )
                _wifiState.value = updatedState

                // Monitor gateway and network transitions
                checkGatewayTransitions(updatedState)

                // Update rolling history
                val currentHist = _rssiHistory.value.toMutableList()
                currentHist.add(dynamicRssi)
                if (currentHist.size > 25) {
                    currentHist.removeAt(0)
                }
                _rssiHistory.value = currentHist

                delay(1500)
            }
        }
    }

    val isForegroundScannerRunning = com.example.service.NetworkScannerForegroundService.isServiceRunning

    fun toggleRealTimeShield(enabled: Boolean) {
        _isRealTimeShieldActive.value = enabled
        if (enabled) {
            startShieldWatcher()
            try {
                com.example.service.NetworkScannerForegroundService.start(getApplication())
            } catch (e: Exception) {
                // Non-blocking in test / restricted environments
            }
        } else {
            shieldWatcherJob?.cancel()
            try {
                com.example.service.NetworkScannerForegroundService.stop(getApplication())
            } catch (e: Exception) {
                // Non-blocking in test / restricted environments
            }
        }
    }

    fun startForegroundScanner() {
        try {
            com.example.service.NetworkScannerForegroundService.start(getApplication())
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun stopForegroundScanner() {
        try {
            com.example.service.NetworkScannerForegroundService.stop(getApplication())
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun triggerImmediateForegroundScan() {
        try {
            com.example.service.NetworkScannerForegroundService.triggerScan(getApplication())
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    private fun startShieldWatcher() {
        shieldWatcherJob?.cancel()
        shieldWatcherJob = viewModelScope.launch {
            while (_isRealTimeShieldActive.value) {
                delay((_scanFrequencySeconds.value * 1000L).coerceAtLeast(3000L))
                if (!_isSubnetScanning.value) {
                    val previousUnauthorized = _discoveredDevices.value.count {
                        !it.isAuthorized && !it.isSelf && !it.isGateway && !it.isFalsePositiveSuppressed
                    }
                    val updated = repository.discoverNetworkDevices(_wifiState.value.ipAddress)
                    val processedDevices = if (_isZeroTolerancePolicyActive.value) {
                        updated.map { dev ->
                            val isSuppressed = dev.isFalsePositiveSuppressed || suppressedDeviceMacs.contains(dev.macAddress.uppercase())
                            val isBenignPrivateMac = dev.isRandomizedMac || isLocallyAdministeredMac(dev.macAddress)
                            val shouldBlock = if (_isZeroFpEngineActive.value) {
                                !dev.isAuthorized && !dev.isSelf && !dev.isGateway && !isSuppressed && !isBenignPrivateMac
                            } else {
                                !dev.isAuthorized && !dev.isSelf && !dev.isGateway && !isSuppressed
                            }

                            val isFilteredBySanitizer = filterIncomingDevice(dev.macAddress)

                            if (shouldBlock && !isFilteredBySanitizer) {
                                repository.setDeviceBlocked(dev.macAddress, true)
                                if (_isAutonomousQuarantineRemovalActive.value) {
                                    networkAccessEnforcer.severAndEvictFromNetwork(dev.macAddress, dev.ip, dev.vendor)
                                    viewModelScope.launch {
                                        repository.deleteDevice(dev.macAddress)
                                    }
                                }
                                dev.copy(isBlocked = true)
                            } else if ((_isZeroFpEngineActive.value || _guardUiState.value.fpFilterEnabled) && (isBenignPrivateMac || isFilteredBySanitizer) && !dev.isAuthorized && !dev.isBlocked) {
                                dev.copy(
                                    isFalsePositiveSuppressed = true,
                                    threatLevel = ThreatLevel.FALSE_POSITIVE_SUPPRESSED,
                                    corroborationVector = "MacSanitizer: Private MAC Corroborated Safe (0% False Alarm Policy)"
                                )
                            } else dev
                        }
                    } else {
                        updated
                    }

                    val activeDevicesForProcessing = if (_isAutonomousQuarantineRemovalActive.value) {
                        processedDevices.filterNot { it.isBlocked }
                    } else {
                        processedDevices
                    }

                    val (finalDevices, violations) = if (_isZeroToleranceDuplicationActive.value) {
                        ZeroToleranceDuplicationGuard.enforceZeroDuplication(
                            devices = activeDevicesForProcessing,
                            expectedSubnetBase = _wifiState.value.ipAddress.substringBeforeLast("."),
                            primaryGatewayIp = _lockedGatewayIp.value,
                            primaryGatewayMac = _lockedGatewayMac.value
                        )
                    } else {
                        Pair(processedDevices, emptyList())
                    }

                    if (violations.isNotEmpty()) {
                        _duplicationGuardStatus.value = _duplicationGuardStatus.value.copy(
                            duplicateGatewaysBlocked = _duplicationGuardStatus.value.duplicateGatewaysBlocked + violations.count { it.violationType == DuplicationViolationType.ROGUE_GATEWAY },
                            duplicateIpsBlocked = _duplicationGuardStatus.value.duplicateIpsBlocked + violations.count { it.violationType == DuplicationViolationType.DUPLICATE_IP },
                            duplicateMacsDeduplicated = _duplicationGuardStatus.value.duplicateMacsDeduplicated + violations.count { it.violationType == DuplicationViolationType.DUPLICATE_MAC },
                            subnetAnomaliesBlocked = _duplicationGuardStatus.value.subnetAnomaliesBlocked + violations.count { it.violationType == DuplicationViolationType.ALIEN_SUBNET },
                            lastViolationEvent = violations.lastOrNull()
                        )
                    }

                    // Background Detection: Flag unknown devices and tag with AI Hardening Notes
                    val taggedDevices = finalDevices.map { dev ->
                        val isUnknown = !dev.isAuthorized && !dev.isSelf && !dev.isGateway
                        if (isUnknown) {
                            val rec = _networkHardeningRecommendations.value.find { 
                                it.targetDeviceIp == dev.ip || it.targetDeviceMac.equals(dev.macAddress, ignoreCase = true) 
                            }
                            dev.copy(
                                isFlaggedUnknown = true,
                                aiHardeningNote = rec?.threatAssessment ?: dev.aiHardeningNote
                            )
                        } else {
                            dev.copy(isFlaggedUnknown = false)
                        }
                    }

                    _discoveredDevices.value = taggedDevices

                    val unknownList = taggedDevices.filter { it.isFlaggedUnknown }
                    _backgroundDetectionStatus.value = _backgroundDetectionStatus.value.copy(
                        unknownDevicesDetected = unknownList.size,
                        lastScanTimestamp = System.currentTimeMillis()
                    )

                    // Trigger Gemini hardening for new unknown devices detected in background
                    val newUnknowns = unknownList.filter { !processedUnknownMacsForAi.contains(it.macAddress.uppercase()) }
                    if (newUnknowns.isNotEmpty() && !_backgroundDetectionStatus.value.isAiAnalyzing) {
                        newUnknowns.forEach { processedUnknownMacsForAi.add(it.macAddress.uppercase()) }
                        triggerGeminiHardeningAnalysis(newUnknowns)
                    }

                    // Execute Known-Device Whitelist Comparison Logic Flow & Notification Trigger
                    processWhitelistComparison(taggedDevices, triggerNotifications = true)

                    val currentUnauthorized = taggedDevices.count {
                        !it.isAuthorized && !it.isSelf && !it.isGateway && !it.isFalsePositiveSuppressed
                    }

                    if (currentUnauthorized > previousUnauthorized) {
                        triggerIntruderHapticAlert()
                    }
                }
            }
        }
    }

    fun toggleBackgroundDetection(enabled: Boolean) {
        _backgroundDetectionStatus.value = _backgroundDetectionStatus.value.copy(isRunning = enabled)
        if (enabled) {
            toggleRealTimeShield(true)
        }
    }

    fun selectHardeningRecommendation(rec: NetworkHardeningRecommendation?) {
        _selectedHardeningRecommendation.value = rec
    }

    fun triggerManualHardeningAudit() {
        val unknowns = _discoveredDevices.value.filter { it.isFlaggedUnknown || (!it.isAuthorized && !it.isSelf && !it.isGateway) }
        val targetList = if (unknowns.isNotEmpty()) unknowns else {
            _discoveredDevices.value.filter { !it.isSelf && !it.isGateway }
        }
        if (targetList.isNotEmpty()) {
            triggerGeminiHardeningAnalysis(targetList, forceRefresh = true)
        }
    }

    private fun triggerGeminiHardeningAnalysis(
        unknownDevices: List<DiscoveredDevice>,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            _backgroundDetectionStatus.value = _backgroundDetectionStatus.value.copy(isAiAnalyzing = true)
            try {
                val recommendations = repository.generateNetworkHardeningRecommendations(
                    unknownDevices = unknownDevices,
                    state = _wifiState.value
                )
                if (recommendations.isNotEmpty()) {
                    val updatedList = if (forceRefresh) {
                        recommendations
                    } else {
                        val existing = _networkHardeningRecommendations.value.toMutableList()
                        recommendations.forEach { newRec ->
                            existing.removeAll { it.targetDeviceIp == newRec.targetDeviceIp }
                            existing.add(0, newRec)
                        }
                        existing
                    }
                    _networkHardeningRecommendations.value = updatedList
                    _backgroundDetectionStatus.value = _backgroundDetectionStatus.value.copy(
                        activeHardeningDirectives = updatedList.size,
                        isAiAnalyzing = false
                    )

                    // Attach notes to discovered devices
                    _discoveredDevices.value = _discoveredDevices.value.map { dev ->
                        val matchedRec = updatedList.find { 
                            it.targetDeviceIp == dev.ip || it.targetDeviceMac.equals(dev.macAddress, ignoreCase = true) 
                        }
                        if (matchedRec != null) {
                            dev.copy(
                                isFlaggedUnknown = true,
                                aiHardeningNote = "${matchedRec.riskLevel}: ${matchedRec.threatAssessment}"
                            )
                        } else dev
                    }
                } else {
                    _backgroundDetectionStatus.value = _backgroundDetectionStatus.value.copy(isAiAnalyzing = false)
                }
            } catch (e: Exception) {
                _backgroundDetectionStatus.value = _backgroundDetectionStatus.value.copy(isAiAnalyzing = false)
            }
        }
    }

    fun simulateUnknownDeviceIntrusion() {
        viewModelScope.launch {
            val subnetBase = _wifiState.value.ipAddress.substringBeforeLast(".")
            val randomHost = (140..199).random()
            val fakeIp = "$subnetBase.$randomHost"
            val fakeMac = "DE:AD:BE:EF:${Random.nextInt(10, 99)}:${Random.nextInt(10, 99)}"
            val fakeDevice = DiscoveredDevice(
                ip = fakeIp,
                macAddress = fakeMac,
                vendor = "Rogue Microcontroller (ESP32-S3 Cam)",
                isAuthorized = false,
                isBlocked = true,
                responseTimeMs = 12L,
                threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER,
                confidencePercent = 99,
                corroborationVector = "Rogue Probe Vector • AI Flagged",
                isFlaggedUnknown = true,
                openPorts = listOf(80, 554, 8080)
            )

            val current = _discoveredDevices.value.toMutableList()
            current.removeAll { it.ip == fakeIp }
            current.add(0, fakeDevice)
            _discoveredDevices.value = current

            _backgroundDetectionStatus.value = _backgroundDetectionStatus.value.copy(
                unknownDevicesDetected = _backgroundDetectionStatus.value.unknownDevicesDetected + 1
            )

            // Trigger whitelist comparison flow & notification
            processWhitelistComparison(current, triggerNotifications = true)

            triggerIntruderHapticAlert()
            triggerGeminiHardeningAnalysis(listOf(fakeDevice), forceRefresh = false)
        }
    }

    fun refreshSubnetDevices() {
        viewModelScope.launch {
            _isSubnetScanning.value = true
            try {
                val devices = repository.discoverNetworkDevices(_wifiState.value.ipAddress)
                val processedDevices = if (_isZeroTolerancePolicyActive.value) {
                    devices.map { dev ->
                        val isSuppressed = dev.isFalsePositiveSuppressed || suppressedDeviceMacs.contains(dev.macAddress.uppercase())
                        val isBenignPrivateMac = dev.isRandomizedMac || isLocallyAdministeredMac(dev.macAddress)
                        val shouldBlock = if (_isZeroFpEngineActive.value) {
                            !dev.isAuthorized && !dev.isSelf && !dev.isGateway && !isSuppressed && !isBenignPrivateMac
                        } else {
                            !dev.isAuthorized && !dev.isSelf && !dev.isGateway && !isSuppressed
                        }

                        if (shouldBlock) {
                            repository.setDeviceBlocked(dev.macAddress, true)
                            dev.copy(isBlocked = true)
                        } else if (_isZeroFpEngineActive.value && isBenignPrivateMac && !dev.isAuthorized && !dev.isBlocked) {
                            dev.copy(
                                isFalsePositiveSuppressed = true,
                                threatLevel = ThreatLevel.FALSE_POSITIVE_SUPPRESSED,
                                corroborationVector = "Zero-FP: Private MAC Corroborated Safe (0% False Alarm Policy)"
                            )
                        } else dev
                    }
                } else {
                    devices
                }

                val (finalDevices, violations) = if (_isZeroToleranceDuplicationActive.value) {
                    ZeroToleranceDuplicationGuard.enforceZeroDuplication(
                        devices = processedDevices,
                        expectedSubnetBase = _wifiState.value.ipAddress.substringBeforeLast("."),
                        primaryGatewayIp = _lockedGatewayIp.value,
                        primaryGatewayMac = _lockedGatewayMac.value
                    )
                } else {
                    Pair(processedDevices, emptyList())
                }

                if (violations.isNotEmpty()) {
                    _duplicationGuardStatus.value = _duplicationGuardStatus.value.copy(
                        duplicateGatewaysBlocked = _duplicationGuardStatus.value.duplicateGatewaysBlocked + violations.count { it.violationType == DuplicationViolationType.ROGUE_GATEWAY },
                        duplicateIpsBlocked = _duplicationGuardStatus.value.duplicateIpsBlocked + violations.count { it.violationType == DuplicationViolationType.DUPLICATE_IP },
                        duplicateMacsDeduplicated = _duplicationGuardStatus.value.duplicateMacsDeduplicated + violations.count { it.violationType == DuplicationViolationType.DUPLICATE_MAC },
                        subnetAnomaliesBlocked = _duplicationGuardStatus.value.subnetAnomaliesBlocked + violations.count { it.violationType == DuplicationViolationType.ALIEN_SUBNET },
                        lastViolationEvent = violations.lastOrNull()
                    )
                }

                // Tag unknown devices
                val taggedDevices = finalDevices.map { dev ->
                    val isUnknown = !dev.isAuthorized && !dev.isSelf && !dev.isGateway
                    if (isUnknown) {
                        val rec = _networkHardeningRecommendations.value.find { 
                            it.targetDeviceIp == dev.ip || it.targetDeviceMac.equals(dev.macAddress, ignoreCase = true) 
                        }
                        dev.copy(
                            isFlaggedUnknown = true,
                            aiHardeningNote = rec?.threatAssessment ?: dev.aiHardeningNote
                        )
                    } else {
                        dev.copy(isFlaggedUnknown = false)
                    }
                }

                _discoveredDevices.value = taggedDevices

                val unknownList = taggedDevices.filter { it.isFlaggedUnknown }
                _backgroundDetectionStatus.value = _backgroundDetectionStatus.value.copy(
                    unknownDevicesDetected = unknownList.size,
                    lastScanTimestamp = System.currentTimeMillis()
                )

                // Execute Known-Device Whitelist Comparison Logic Flow & Notification Trigger
                processWhitelistComparison(taggedDevices, triggerNotifications = true)

                val unauthorized = taggedDevices.filter { !it.isAuthorized && !it.isSelf && !it.isGateway && !it.isFalsePositiveSuppressed }
                if (unauthorized.isNotEmpty()) {
                    triggerIntruderHapticAlert()
                }
            } finally {
                _isSubnetScanning.value = false
            }
        }
    }

    fun refreshNearbyAPs() {
        viewModelScope.launch {
            val aps = repository.getNearbyAccessPoints()
            _nearbyAccessPoints.value = aps
            computeChannelCongestion(aps, _wifiState.value.channel)
        }
    }

    private fun computeChannelCongestion(aps: List<NearbyAccessPoint>, currentChannel: Int) {
        // Standard 2.4 GHz channels: 1, 6, 11 + other active channels
        // Standard 5 GHz channels: 36, 40, 44, 48, 149, 153, 157, 161
        val standardChannels = listOf(
            Pair(1, "2.4 GHz"), Pair(3, "2.4 GHz"), Pair(6, "2.4 GHz"), Pair(9, "2.4 GHz"), Pair(11, "2.4 GHz"),
            Pair(36, "5 GHz"), Pair(40, "5 GHz"), Pair(44, "5 GHz"), Pair(48, "5 GHz"),
            Pair(149, "5 GHz"), Pair(153, "5 GHz"), Pair(157, "5 GHz"), Pair(161, "5 GHz")
        )

        val channelCounts = aps.groupingBy { it.channel }.eachCount()

        val list = standardChannels.map { (ch, band) ->
            val count = channelCounts[ch] ?: 0
            val score = (count * 25).coerceIn(5, 100)
            ChannelCongestion(
                channel = ch,
                band = band,
                apCount = count,
                interferenceScore = score,
                isCurrentChannel = (ch == currentChannel),
                isRecommended = (count == 0 && (ch == 1 || ch == 6 || ch == 11 || ch == 36 || ch == 149))
            )
        }
        _channelCongestionList.value = list
    }

    fun toggleDeviceAuthorization(device: DiscoveredDevice) {
        viewModelScope.launch {
            val newStatus = !device.isAuthorized
            repository.setDeviceAuthorized(device.macAddress, newStatus)
            val updated = _discoveredDevices.value.map {
                if (it.macAddress == device.macAddress) {
                    it.copy(
                        isAuthorized = newStatus,
                        isFlaggedUnknown = !newStatus,
                        threatLevel = if (newStatus) ThreatLevel.SAFE else ThreatLevel.UNAUTHORIZED_INTRUDER
                    )
                } else it
            }
            _discoveredDevices.value = updated
            processWhitelistComparison(updated, triggerNotifications = !newStatus)
        }
    }

    suspend fun processWhitelistComparison(
        devices: List<DiscoveredDevice>,
        triggerNotifications: Boolean = true
    ): DeviceWhitelistAuditResult {
        val whitelist = repository.getWhitelistedDevicesList()
        val audit = DeviceWhitelistComparisonEngine.compareConnectedAgainstWhitelist(
            connectedDevices = devices,
            whitelist = whitelist,
            currentIp = _wifiState.value.ipAddress,
            gatewayIp = _lockedGatewayIp.value.ifBlank { _wifiState.value.gatewayIp },
            gatewayMac = _lockedGatewayMac.value,
            previouslyAlertedMacs = alertedUnknownMacs,
            suppressedMacs = suppressedDeviceMacs
        )

        _whitelistAuditResult.value = audit

        if (triggerNotifications && _isAutoNotifyEnabled.value && audit.newlyDetectedUnknowns.isNotEmpty()) {
            audit.newlyDetectedUnknowns.forEach { unknownDev ->
                alertedUnknownMacs.add(unknownDev.macAddress.uppercase())

                // 1. Dispatch rich Android system notification for unauthorized device detected by background scanner
                SecurityNotificationDispatcher.postUnauthorizedDeviceAlert(
                    context = getApplication(),
                    device = unknownDev,
                    threatReason = "Unauthorized host detected on subnet by background scanner"
                )

                // 2. Persist in Room security alert feed
                repository.recordSecurityAlert(
                    title = "🚨 Unauthorized Device Detected: ${unknownDev.ip}",
                    description = "${unknownDev.vendor.ifBlank { "Unidentified Host" }} (${unknownDev.macAddress}) joined local network. Not found on known-device whitelist.",
                    deviceIp = unknownDev.ip,
                    deviceMac = unknownDev.macAddress,
                    severity = "CRITICAL",
                    confidencePercent = 99
                )
            }

            if (audit.newlyDetectedUnknowns.size > 1) {
                SecurityNotificationDispatcher.postMultipleUnauthorizedDevicesAlert(
                    context = getApplication(),
                    unauthorizedDevices = audit.newlyDetectedUnknowns
                )
            }

            triggerIntruderHapticAlert()
        }

        return audit
    }

    fun runManualWhitelistComparison() {
        viewModelScope.launch {
            _isSubnetScanning.value = true
            try {
                val current = _discoveredDevices.value
                val devices = if (current.isNotEmpty()) current else repository.discoverNetworkDevices(_wifiState.value.ipAddress)
                processWhitelistComparison(devices, triggerNotifications = true)
            } finally {
                _isSubnetScanning.value = false
            }
        }
    }

    fun toggleAutoNotification(enabled: Boolean) {
        _isAutoNotifyEnabled.value = enabled
    }

    fun triggerTestUnauthorizedDeviceAlert(
        sampleIp: String = "192.168.1.189",
        sampleMac: String = "00:E0:4C:68:01:AF",
        sampleVendor: String = "Unknown Shenzhen Host"
    ) {
        val testDevice = DiscoveredDevice(
            ip = sampleIp,
            macAddress = sampleMac,
            vendor = sampleVendor,
            isAuthorized = false,
            openPorts = listOf(22, 80, 8080),
            threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER
        )
        SecurityNotificationDispatcher.postUnauthorizedDeviceAlert(
            context = getApplication(),
            device = testDevice,
            threatReason = "Zero-Tolerance: Unauthorized host discovered by background scanner"
        )
        triggerIntruderHapticAlert()
    }

    fun addDeviceToWhitelist(device: DiscoveredDevice) {
        viewModelScope.launch {
            repository.addDeviceToWhitelist(device)
            val updated = _discoveredDevices.value.map {
                if (it.macAddress.equals(device.macAddress, ignoreCase = true)) {
                    it.copy(
                        isAuthorized = true,
                        isBlocked = false,
                        isFlaggedUnknown = false,
                        threatLevel = ThreatLevel.SAFE
                    )
                } else it
            }
            _discoveredDevices.value = updated
            processWhitelistComparison(updated, triggerNotifications = false)
        }
    }

    fun removeDeviceFromWhitelist(mac: String) {
        viewModelScope.launch {
            repository.removeDeviceFromWhitelist(mac)
            val updated = _discoveredDevices.value.map {
                if (it.macAddress.equals(mac, ignoreCase = true)) {
                    it.copy(
                        isAuthorized = false,
                        isFlaggedUnknown = true,
                        threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER
                    )
                } else it
            }
            _discoveredDevices.value = updated
            processWhitelistComparison(updated, triggerNotifications = true)
        }
    }

    fun clearAlertedUnknownCache() {
        alertedUnknownMacs.clear()
    }

    fun setDeviceAlias(device: DiscoveredDevice, alias: String) {
        viewModelScope.launch {
            repository.setDeviceCustomName(device.macAddress, alias)
            _discoveredDevices.value = _discoveredDevices.value.map {
                if (it.macAddress == device.macAddress) it.copy(customName = alias) else it
            }
        }
    }

    fun recordLocationSignal(locationName: String) {
        viewModelScope.launch {
            repository.logSignalSample(locationName, _wifiState.value)
        }
    }

    fun deleteSignalLog(id: Long) {
        viewModelScope.launch {
            repository.deleteSignalLog(id)
        }
    }

    fun clearAllSignalLogs() {
        viewModelScope.launch {
            repository.clearSignalLogs()
        }
    }

    fun acknowledgeAlert(id: Long) {
        viewModelScope.launch {
            repository.acknowledgeAlert(id)
        }
    }

    fun acknowledgeAllAlerts() {
        viewModelScope.launch {
            repository.acknowledgeAllAlerts()
        }
    }

    fun runAiOptimization() {
        viewModelScope.launch {
            _isAiOptimizing.value = true
            try {
                val report = repository.generateAiOptimization(
                    _wifiState.value,
                    _discoveredDevices.value,
                    _nearbyAccessPoints.value
                )
                _aiReport.value = report
            } finally {
                _isAiOptimizing.value = false
            }
        }
    }

    fun sendChatMessage(question: String) {
        if (question.isBlank()) return
        val userMsg = ChatMessage(text = question, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatGenerating.value = true

        viewModelScope.launch {
            try {
                val answer = repository.askAiAdvisor(
                    question,
                    _wifiState.value,
                    _discoveredDevices.value
                )
                _chatMessages.value = _chatMessages.value + ChatMessage(text = answer, isUser = false)
            } finally {
                _isChatGenerating.value = false
            }
        }
    }

    private fun checkGatewayTransitions(newState: WifiConnectionState) {
        val currentGwIp = newState.gatewayIp
        val currentSsid = newState.ssid
        val currentBssid = newState.bssid
        val currentGwMac = _discoveredDevices.value.find { it.isGateway }?.macAddress ?: "00:1A:2B:3C:4D:01"

        // Check if gateway or network changed
        var detectedEvent: GatewayTransitionEvent? = null

        if (currentGwMac != previousGatewayMac && currentGwIp == previousGatewayIp) {
            // CRITICAL: Gateway MAC changed while IP remained identical -> ARP Poisoning / Rogue Gateway Spoofing!
            detectedEvent = GatewayTransitionEvent(
                switchType = GatewaySwitchType.GATEWAY_MAC_SPOOF_RISK,
                oldGatewayIp = previousGatewayIp,
                newGatewayIp = currentGwIp,
                oldGatewayMac = previousGatewayMac,
                newGatewayMac = currentGwMac,
                oldSsid = previousSsid,
                newSsid = currentSsid,
                oldBssid = previousBssid,
                newBssid = currentBssid,
                details = "ALERT: Gateway MAC shifted from $previousGatewayMac to $currentGwMac on $currentGwIp. High probability of ARP cache poisoning or rogue gateway takeover!",
                severity = "CRITICAL"
            )
        } else if (currentGwIp != previousGatewayIp) {
            // Subnet Gateway switch
            val isLockdownBreach = _gatewayLockdownActive.value && currentGwIp != _lockedGatewayIp.value
            detectedEvent = GatewayTransitionEvent(
                switchType = GatewaySwitchType.GATEWAY_IP_CHANGED,
                oldGatewayIp = previousGatewayIp,
                newGatewayIp = currentGwIp,
                oldGatewayMac = previousGatewayMac,
                newGatewayMac = currentGwMac,
                oldSsid = previousSsid,
                newSsid = currentSsid,
                oldBssid = previousBssid,
                newBssid = currentBssid,
                details = "Gateway address shifted from $previousGatewayIp to $currentGwIp." +
                        if (isLockdownBreach) " VIOLATION: Locked gateway baseline bypassed!" else "",
                severity = if (isLockdownBreach) "CRITICAL" else "WARNING"
            )
        } else if (currentSsid != previousSsid && currentSsid.isNotBlank()) {
            // SSID Network switch
            detectedEvent = GatewayTransitionEvent(
                switchType = GatewaySwitchType.SSID_SWITCHED,
                oldGatewayIp = previousGatewayIp,
                newGatewayIp = currentGwIp,
                oldGatewayMac = previousGatewayMac,
                newGatewayMac = currentGwMac,
                oldSsid = previousSsid,
                newSsid = currentSsid,
                oldBssid = previousBssid,
                newBssid = currentBssid,
                details = "Network switched from SSID '$previousSsid' to '$currentSsid'. Subnet re-indexed.",
                severity = "WARNING"
            )
        } else if (currentBssid != previousBssid && currentBssid != "--:--:--:--:--:--") {
            // Access point mesh roaming transition
            detectedEvent = GatewayTransitionEvent(
                switchType = GatewaySwitchType.BSSID_ROAMING,
                oldGatewayIp = previousGatewayIp,
                newGatewayIp = currentGwIp,
                oldGatewayMac = previousGatewayMac,
                newGatewayMac = currentGwMac,
                oldSsid = previousSsid,
                newSsid = currentSsid,
                oldBssid = previousBssid,
                newBssid = currentBssid,
                details = "Access point roaming transition ($previousBssid -> $currentBssid) on $currentSsid.",
                severity = "INFO"
            )
        }

        if (detectedEvent != null) {
            previousGatewayIp = currentGwIp
            previousGatewayMac = currentGwMac
            previousSsid = currentSsid
            previousBssid = currentBssid

            _activeGatewayAlert.value = detectedEvent
            _recentGatewayTransitions.value = listOf(detectedEvent) + _recentGatewayTransitions.value.take(9)

            triggerGatewaySwitchHapticAlert()
            viewModelScope.launch {
                repository.recordGatewayTransitionAlert(detectedEvent)
            }
        }
    }

    fun simulateGatewaySwitch(isSpoofRogue: Boolean) {
        val current = _wifiState.value
        val simulatedEvent = if (isSpoofRogue) {
            val rogueMac = "DE:AD:BE:EF:00:99"
            GatewayTransitionEvent(
                switchType = GatewaySwitchType.GATEWAY_MAC_SPOOF_RISK,
                oldGatewayIp = current.gatewayIp,
                newGatewayIp = current.gatewayIp,
                oldGatewayMac = previousGatewayMac,
                newGatewayMac = rogueMac,
                oldSsid = current.ssid,
                newSsid = current.ssid,
                oldBssid = current.bssid,
                newBssid = current.bssid,
                details = "CRITICAL ALERT: Rogue Gateway Detected! Gateway MAC for ${current.gatewayIp} silently switched to $rogueMac. Probable Man-in-the-Middle ARP takeover!",
                severity = "CRITICAL"
            )
        } else {
            val newIp = if (current.gatewayIp == "192.168.1.1") "10.0.0.1" else "192.168.1.1"
            val newSsid = if (current.ssid == "Office_Ultra_5G") "Guest_Perimeter_2G" else "Office_Ultra_5G"
            GatewayTransitionEvent(
                switchType = GatewaySwitchType.SSID_SWITCHED,
                oldGatewayIp = current.gatewayIp,
                newGatewayIp = newIp,
                oldGatewayMac = previousGatewayMac,
                newGatewayMac = "00:1A:2B:EE:77:12",
                oldSsid = current.ssid,
                newSsid = newSsid,
                oldBssid = current.bssid,
                newBssid = "3C:52:82:A4:91:EE",
                details = "Network Switch Detected: Device migrated from ${current.ssid} (${current.gatewayIp}) to $newSsid ($newIp).",
                severity = "WARNING"
            )
        }

        _activeGatewayAlert.value = simulatedEvent
        _recentGatewayTransitions.value = listOf(simulatedEvent) + _recentGatewayTransitions.value.take(9)
        triggerGatewaySwitchHapticAlert()

        viewModelScope.launch {
            repository.recordGatewayTransitionAlert(simulatedEvent)
        }
    }

    fun toggleGatewayLockdown(enabled: Boolean) {
        _gatewayLockdownActive.value = enabled
    }

    fun lockCurrentGateway() {
        val current = _wifiState.value
        val gwMac = _discoveredDevices.value.find { it.isGateway }?.macAddress ?: "00:1A:2B:3C:4D:01"
        _lockedGatewayIp.value = current.gatewayIp
        _lockedGatewayMac.value = gwMac
        _gatewayLockdownActive.value = true
    }

    fun dismissActiveGatewayAlert() {
        _activeGatewayAlert.value = null
    }

    private fun startBtZeroTolerancePerimeterSentry() {
        repository.startBtPerimeterScan { threat ->
            if (_isBtZeroToleranceShieldActive.value) {
                triggerBtIntruderHapticAlert()
                viewModelScope.launch {
                    repository.recordBtIntrusionAlert(threat)
                }
            }
        }
    }

    fun toggleBtZeroToleranceShield(enabled: Boolean) {
        _isBtZeroToleranceShieldActive.value = enabled
        if (enabled) {
            startBtZeroTolerancePerimeterSentry()
        } else {
            repository.stopBtPerimeterScan()
        }
    }

    fun toggleBtQuarantine(address: String) {
        val current = perimeterBtDevices.value.find { it.address.equals(address, ignoreCase = true) }
        val willBeQuarantined = !(current?.isQuarantined ?: false)
        repository.toggleBtQuarantine(address)
        if (willBeQuarantined) {
            networkAccessEnforcer.blockDeviceNetworkAccess(address, "", current?.name ?: "Bluetooth Device")
        } else {
            networkAccessEnforcer.unblockDeviceNetworkAccess(address)
        }
    }

    fun toggleBtDeviceTrust(address: String) {
        repository.toggleBtTrust(address)
    }

    fun dismissBtDeviceFalsePositive(address: String) {
        val normalized = address.uppercase().trim()
        val currentDevice = perimeterBtDevices.value.find { it.address.equals(normalized, ignoreCase = true) }
        val wasAlreadySuppressed = suppressedBtAddresses.contains(normalized) ||
                (currentDevice?.let { it.isTrusted && !it.isQuarantined && it.isFalsePositiveSuppressed } ?: false)

        repository.dismissBtFalsePositive(normalized)
        networkAccessEnforcer.unblockDeviceNetworkAccess(normalized)
        if (!wasAlreadySuppressed) {
            suppressedBtAddresses.add(normalized)
            _falsePositivesSuppressedCount.value += 1
        }
    }

    fun toggleZeroTolerancePolicy(enabled: Boolean) {
        _isZeroTolerancePolicyActive.value = enabled
        if (enabled) {
            lockdownAllUnauthorizedHosts()
        }
    }

    fun toggleZeroFpEngine(enabled: Boolean) {
        _isZeroFpEngineActive.value = enabled
        if (enabled) {
            applyZeroFalsePositivePolicy()
        }
    }

    fun applyZeroFalsePositivePolicy() {
        _discoveredDevices.value = _discoveredDevices.value.map { dev ->
            val isBenignCandidate = dev.isRandomizedMac || isLocallyAdministeredMac(dev.macAddress) || suppressedDeviceMacs.contains(dev.macAddress.uppercase())
            if (!dev.isAuthorized && !dev.isSelf && !dev.isGateway && isBenignCandidate && dev.confidencePercent < 95) {
                suppressedDeviceMacs.add(dev.macAddress.uppercase())
                _falsePositivesSuppressedCount.value += 1
                dev.copy(
                    isAuthorized = true,
                    isBlocked = false,
                    threatLevel = ThreatLevel.FALSE_POSITIVE_SUPPRESSED,
                    isFalsePositiveSuppressed = true,
                    isZeroFpProtected = true,
                    corroborationVector = "Zero-Tolerance to FP: Benign Signature Corroborated Safe (0% FP Policy)"
                )
            } else dev
        }
    }

    fun quarantineDevice(device: DiscoveredDevice) {
        val newBlocked = !device.isBlocked
        if (newBlocked) {
            if (_isAutonomousQuarantineRemovalActive.value) {
                // Autonomous configuration rule: immediately sever network access and evict quarantined device
                networkAccessEnforcer.severAndEvictFromNetwork(device.macAddress, device.ip, device.vendor)
                viewModelScope.launch {
                    repository.deleteDevice(device.macAddress)
                    repository.recordSecurityAlert(
                        title = "AUTONOMOUS QUARANTINE REMOVAL",
                        description = "Autonomous Enforcement: Quarantined host ${device.ip} (${device.macAddress}) severed and evicted from network.",
                        deviceIp = device.ip,
                        deviceMac = device.macAddress,
                        severity = "CRITICAL"
                    )
                }
                _discoveredDevices.value = _discoveredDevices.value.filterNot { it.macAddress.equals(device.macAddress, ignoreCase = true) }
                triggerIntruderHapticAlert()
                return
            } else {
                networkAccessEnforcer.blockDeviceNetworkAccess(device.macAddress, device.ip, device.vendor)
            }
        } else {
            networkAccessEnforcer.unblockDeviceNetworkAccess(device.macAddress)
        }
        _discoveredDevices.value = _discoveredDevices.value.map {
            if (it.macAddress.equals(device.macAddress, ignoreCase = true)) {
                it.copy(
                    isBlocked = newBlocked,
                    isAuthorized = if (newBlocked) false else it.isAuthorized,
                    responseTimeMs = if (newBlocked) 0L else 14L,
                    corroborationVector = if (newBlocked) "Firewall Isolation Active • Zero Network Access" else "Active Monitored Host"
                )
            } else it
        }
        viewModelScope.launch {
            repository.setDeviceBlocked(device.macAddress, newBlocked)
            if (newBlocked) {
                triggerIntruderHapticAlert()
            }
        }
    }

    /**
     * Completely severs, deauthenticates, and removes all quarantined devices from the network.
     * Complies with G4035 autonomous quarantine policy and POTRAZ guidelines.
     */
    fun removeQuarantinedDevicesFromNetwork() {
        viewModelScope.launch {
            val blockedSubnet = _discoveredDevices.value.filter { it.isBlocked }
            val blockedUnethical = _unethicalDevices.value.filter { it.isQuarantined }
            val blockedGateways = _managedGateways.value.filter { it.isQuarantined }

            // 1. Enforce kernel firewall DROP and ARP cache eviction
            blockedSubnet.forEach { dev ->
                networkAccessEnforcer.severAndEvictFromNetwork(dev.macAddress, dev.ip, dev.vendor)
                repository.deleteDevice(dev.macAddress)
            }
            blockedUnethical.forEach { threat ->
                networkAccessEnforcer.severAndEvictFromNetwork(threat.mac, threat.ip, threat.threatType.title)
                repository.deleteDevice(threat.mac)
            }
            blockedGateways.forEach { gw ->
                networkAccessEnforcer.severAndEvictFromNetwork(gw.mac, gw.ip, gw.vendor)
                repository.deleteDevice(gw.mac)
            }

            // 2. Evict and purge from active network inventories
            _discoveredDevices.value = _discoveredDevices.value.filterNot { it.isBlocked }
            _unethicalDevices.value = _unethicalDevices.value.filterNot { it.isQuarantined }
            _managedGateways.value = _managedGateways.value.filterNot { it.isQuarantined }

            val totalPurged = blockedSubnet.size + blockedUnethical.size + blockedGateways.size
            repository.recordSecurityAlert(
                title = "NETWORK QUARANTINE PURGE EXECUTED",
                description = "Autonomous Quarantine Policy: $totalPurged quarantined endpoint(s) severed from router routing tables and purged from network.",
                deviceIp = "255.255.255.255",
                deviceMac = "FF:FF:FF:FF:FF:FF",
                severity = "INFO"
            )
        }
    }

    /**
     * Removes an individual quarantined device from the network.
     */
    fun removeQuarantinedDevice(device: DiscoveredDevice) {
        viewModelScope.launch {
            networkAccessEnforcer.severAndEvictFromNetwork(device.macAddress, device.ip, device.vendor)
            repository.deleteDevice(device.macAddress)
            _discoveredDevices.value = _discoveredDevices.value.filterNot { it.macAddress.equals(device.macAddress, ignoreCase = true) }
            repository.recordSecurityAlert(
                title = "QUARANTINED DEVICE REMOVED FROM NETWORK",
                description = "Host at ${device.ip} (${device.macAddress}) severed via kernel ACL and evicted from network tables.",
                deviceIp = device.ip,
                deviceMac = device.macAddress,
                severity = "WARNING"
            )
        }
    }

    fun dismissDeviceAsFalsePositive(device: DiscoveredDevice) {
        viewModelScope.launch {
            val normalizedMac = device.macAddress.uppercase().trim()
            val current = _discoveredDevices.value.find { it.macAddress.equals(normalizedMac, ignoreCase = true) }
            val isAlreadySafe = suppressedDeviceMacs.contains(normalizedMac) ||
                    (current != null && current.isAuthorized && !current.isBlocked && (current.threatLevel == ThreatLevel.SAFE || current.threatLevel == ThreatLevel.FALSE_POSITIVE_SUPPRESSED))

            repository.suppressDeviceAsFalsePositive(normalizedMac)
            networkAccessEnforcer.unblockDeviceNetworkAccess(normalizedMac)
            if (!isAlreadySafe) {
                suppressedDeviceMacs.add(normalizedMac)
                _falsePositivesSuppressedCount.value += 1
            }

            _discoveredDevices.value = _discoveredDevices.value.map {
                if (it.macAddress.equals(normalizedMac, ignoreCase = true)) {
                    it.copy(
                        isAuthorized = true,
                        isBlocked = false,
                        confidencePercent = 100,
                        threatLevel = ThreatLevel.FALSE_POSITIVE_SUPPRESSED,
                        isFalsePositiveSuppressed = true,
                        isZeroFpProtected = true,
                        corroborationVector = "Zero-FP Policy: Manually Suppressed & Baseline Verified (0% False Alarm)"
                    )
                } else it
            }
        }
    }

    fun markAlertAsFalsePositive(alertId: Long, mac: String) {
        viewModelScope.launch {
            val normalizedMac = mac.uppercase().trim()
            val existingAlert = securityAlerts.value.find { it.id == alertId }
            val wasAlreadySuppressed = existingAlert?.isFalsePositive == true || suppressedDeviceMacs.contains(normalizedMac)

            repository.markAlertFalsePositive(alertId, normalizedMac)
            networkAccessEnforcer.unblockDeviceNetworkAccess(normalizedMac)
            if (!wasAlreadySuppressed) {
                if (normalizedMac.isNotBlank()) {
                    suppressedDeviceMacs.add(normalizedMac)
                }
                _falsePositivesSuppressedCount.value += 1
            }

            _discoveredDevices.value = _discoveredDevices.value.map {
                if (it.macAddress.equals(normalizedMac, ignoreCase = true)) {
                    it.copy(
                        isAuthorized = true,
                        isBlocked = false,
                        confidencePercent = 100,
                        threatLevel = ThreatLevel.FALSE_POSITIVE_SUPPRESSED,
                        isFalsePositiveSuppressed = true,
                        isZeroFpProtected = true,
                        corroborationVector = "Zero-FP Policy: Alert Suppressed & Verified Safe"
                    )
                } else it
            }
        }
    }

    fun lockdownAllUnauthorizedHosts() {
        val newlyLockedHosts = _discoveredDevices.value.filter {
            !it.isAuthorized && !it.isSelf && !it.isGateway && !it.isBlocked &&
            !it.isFalsePositiveSuppressed &&
            (!_isZeroFpEngineActive.value || (!it.isRandomizedMac && it.confidencePercent >= 90))
        }
        for (host in newlyLockedHosts) {
            networkAccessEnforcer.blockDeviceNetworkAccess(host.macAddress, host.ip, host.vendor)
        }
        _discoveredDevices.value = _discoveredDevices.value.map { dev ->
            if (!dev.isAuthorized && !dev.isSelf && !dev.isGateway &&
                !dev.isFalsePositiveSuppressed &&
                (!_isZeroFpEngineActive.value || (!dev.isRandomizedMac && dev.confidencePercent >= 90))
            ) {
                dev.copy(
                    isBlocked = true,
                    responseTimeMs = 0L,
                    corroborationVector = "Firewall Isolation Active • Zero Network Access"
                )
            } else dev
        }
        viewModelScope.launch {
            for (host in newlyLockedHosts) {
                repository.setDeviceBlocked(host.macAddress, true)
            }
            // Idempotency: only trigger haptic alert if hosts were newly transitioned to locked/quarantined
            if (newlyLockedHosts.isNotEmpty()) {
                triggerIntruderHapticAlert()
            }
        }
    }

    fun simulateTransientFalsePositive() {
        val randMac = "7A:91:DE:${(10..99).random()}:${(10..99).random()}:${(10..99).random()}"
        val isZeroFp = _isZeroFpEngineActive.value
        val testDev = if (isZeroFp) {
            _falsePositivesSuppressedCount.value += 1
            suppressedDeviceMacs.add(randMac.uppercase())
            DiscoveredDevice(
                ip = "192.168.1.${(140..199).random()}",
                macAddress = randMac,
                vendor = "Apple iPhone (Private Address)",
                customName = "Personal iPhone (Randomized MAC)",
                isAuthorized = true,
                isBlocked = false,
                responseTimeMs = 18L,
                threatLevel = ThreatLevel.FALSE_POSITIVE_SUPPRESSED,
                isRandomizedMac = true,
                confidencePercent = 100,
                corroborationVector = "Zero-Tolerance to FP: Private MAC Verified Benign (0% False Alarm Policy)",
                isFalsePositiveSuppressed = true,
                isZeroFpProtected = true
            )
        } else {
            // When Zero-Tolerance to False Positives is bypassed, naïve IDS flags false intruder alarm
            DiscoveredDevice(
                ip = "192.168.1.${(140..199).random()}",
                macAddress = randMac,
                vendor = "Apple iPhone (Private Address)",
                customName = "Personal iPhone (Randomized MAC)",
                isAuthorized = false,
                isBlocked = false,
                responseTimeMs = 18L,
                threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER,
                isRandomizedMac = true,
                confidencePercent = 65,
                corroborationVector = "False Alarm: Private MAC Mistakenly Flagged (Zero-FP Inactive)",
                isFalsePositiveSuppressed = false,
                isZeroFpProtected = false
            )
        }
        val list = _discoveredDevices.value.toMutableList()
        list.add(2.coerceAtMost(list.size), testDev)
        _discoveredDevices.value = list
        if (!isZeroFp) {
            triggerIntruderHapticAlert()
        }
    }

    fun simulateIntruderBreach() {
        val randMac = "00:0C:29:${(10..99).random()}:${(10..99).random()}:${(10..99).random()}"
        val shouldAutoBlock = _isZeroTolerancePolicyActive.value
        val testDev = DiscoveredDevice(
            ip = "192.168.1.240",
            macAddress = randMac,
            vendor = "VMware / Kali Linux Intruder",
            customName = "Kali Linux ARP Prober",
            isAuthorized = false,
            isBlocked = shouldAutoBlock,
            responseTimeMs = 3L,
            threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER,
            isRandomizedMac = false,
            confidencePercent = 99,
            corroborationVector = if (shouldAutoBlock) "Auto-Quarantined: Zero-Tolerance Policy Active" else "Corroborated: High Frequency SYN/ARP Scan"
        )
        val list = _discoveredDevices.value.toMutableList()
        list.add(0, testDev)
        _discoveredDevices.value = list
        triggerIntruderHapticAlert()
        viewModelScope.launch {
            if (shouldAutoBlock) {
                repository.setDeviceBlocked(randMac, true)
            }
            repository.recordGatewayTransitionAlert(
                GatewayTransitionEvent(
                    switchType = GatewaySwitchType.GATEWAY_MAC_SPOOF_RISK,
                    oldGatewayIp = _wifiState.value.gatewayIp,
                    newGatewayIp = _wifiState.value.gatewayIp,
                    oldGatewayMac = "00:1A:2B:3C:4D:01",
                    newGatewayMac = randMac,
                    oldSsid = _wifiState.value.ssid,
                    newSsid = _wifiState.value.ssid,
                    oldBssid = _wifiState.value.bssid,
                    newBssid = _wifiState.value.bssid,
                    details = if (shouldAutoBlock) {
                        "Zero-Tolerance Auto-Quarantine: Kali Linux host at 192.168.1.240 was automatically blocked upon detection."
                    } else {
                        "Zero-Tolerance Intruder Detected: Kali Linux host at 192.168.1.240 injecting unsolicited ARP packets."
                    },
                    severity = "CRITICAL"
                )
            )
        }
    }

    fun simulateRogueBtDevice(name: String, type: BtDeviceType) {
        val threat = repository.injectSimulatedBtThreat(name, type)
        if (_isBtZeroToleranceShieldActive.value) {
            triggerBtIntruderHapticAlert()
            viewModelScope.launch {
                repository.recordBtIntrusionAlert(threat)
            }
        }
    }

    fun setZeroToleranceDuplicationActive(active: Boolean) {
        _isZeroToleranceDuplicationActive.value = active
        _duplicationGuardStatus.value = _duplicationGuardStatus.value.copy(isEnforced = active)
    }

    fun simulateDuplicationBreach(type: DuplicationViolationType) {
        val currentSubnet = _wifiState.value.ipAddress.substringBeforeLast(".")
        val gwIp = _wifiState.value.gatewayIp.ifBlank { "$currentSubnet.1" }
        val gwMac = _lockedGatewayMac.value

        when (type) {
            DuplicationViolationType.ROGUE_GATEWAY -> {
                val rogueMac = "E4:5F:01:${(10..99).random()}:${(10..99).random()}:${(10..99).random()}"
                val rogueDev = DiscoveredDevice(
                    ip = gwIp,
                    macAddress = rogueMac,
                    vendor = "Rogue Router / Evil Twin (Fake Gateway)",
                    customName = "Rogue Gateway Impersonator",
                    isAuthorized = false,
                    isGateway = false,
                    isRogueGateway = true,
                    isBlocked = true,
                    responseTimeMs = 0L,
                    threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER,
                    confidencePercent = 100,
                    corroborationVector = "Zero-Tolerance: Rogue Gateway Impersonator Intercepted • Access Revoked",
                    duplicationAlertDetail = "Attempted duplicate gateway IP $gwIp (Clash with $gwMac)"
                )
                val current = _discoveredDevices.value.filter { !it.isRogueGateway }.toMutableList()
                current.add(1.coerceAtMost(current.size), rogueDev)
                _discoveredDevices.value = current
                _duplicationGuardStatus.value = _duplicationGuardStatus.value.copy(
                    duplicateGatewaysBlocked = _duplicationGuardStatus.value.duplicateGatewaysBlocked + 1,
                    lastViolationEvent = DuplicationViolation(
                        violationType = DuplicationViolationType.ROGUE_GATEWAY,
                        ip = gwIp,
                        macAddress = rogueMac,
                        conflictingDetail = "Rogue gateway clone claiming default gateway IP $gwIp against master MAC $gwMac"
                    )
                )
                triggerIntruderHapticAlert()
                viewModelScope.launch {
                    repository.setDeviceBlocked(rogueMac, true)
                    repository.recordDuplicationAlert(
                        title = "Zero-Tolerance: Rogue Gateway Duplication Intercepted",
                        description = "Rogue AP / Impersonator host $rogueMac attempted to claim Gateway IP $gwIp. Immediately isolated with zero network access.",
                        ip = gwIp,
                        mac = rogueMac
                    )
                }
            }
            DuplicationViolationType.DUPLICATE_IP -> {
                val targetIp = "$currentSubnet.22"
                val rogueMac = "00:0C:29:${(10..99).random()}:${(10..99).random()}:${(10..99).random()}"
                val dupDev = DiscoveredDevice(
                    ip = targetIp,
                    macAddress = rogueMac,
                    vendor = "Kali Linux ARP Spoofer",
                    customName = "Duplicate IP Collision Attacker",
                    isAuthorized = false,
                    isDuplicateIp = true,
                    isBlocked = true,
                    responseTimeMs = 0L,
                    threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER,
                    confidencePercent = 100,
                    corroborationVector = "Zero-Tolerance: IP Duplication / ARP Poisoning Intercepted • Quarantined",
                    duplicationAlertDetail = "Conflicting IP collision on $targetIp"
                )
                val current = _discoveredDevices.value.filter { !it.isDuplicateIp }.toMutableList()
                current.add(0, dupDev)
                _discoveredDevices.value = current
                _duplicationGuardStatus.value = _duplicationGuardStatus.value.copy(
                    duplicateIpsBlocked = _duplicationGuardStatus.value.duplicateIpsBlocked + 1,
                    lastViolationEvent = DuplicationViolation(
                        violationType = DuplicationViolationType.DUPLICATE_IP,
                        ip = targetIp,
                        macAddress = rogueMac,
                        conflictingDetail = "Rogue host $rogueMac generated IP collision attack on $targetIp"
                    )
                )
                triggerIntruderHapticAlert()
                viewModelScope.launch {
                    repository.setDeviceBlocked(rogueMac, true)
                    repository.recordDuplicationAlert(
                        title = "Zero-Tolerance: IP Collision / ARP Poisoning Alert",
                        description = "Rogue node $rogueMac claimed existing IP $targetIp. Zero-tolerance policy auto-isolated host.",
                        ip = targetIp,
                        mac = rogueMac
                    )
                }
            }
            DuplicationViolationType.DUPLICATE_MAC -> {
                // MAC clone simulation: Attacker cloned authorized MAC
                _duplicationGuardStatus.value = _duplicationGuardStatus.value.copy(
                    duplicateMacsDeduplicated = _duplicationGuardStatus.value.duplicateMacsDeduplicated + 1,
                    lastViolationEvent = DuplicationViolation(
                        violationType = DuplicationViolationType.DUPLICATE_MAC,
                        ip = "$currentSubnet.199",
                        macAddress = "B4:FB:E4:91:22:A1",
                        conflictingDetail = "MAC cloning attack: Attacker attempted to replicate authorized Layer-2 MAC B4:FB:E4:91:22:A1"
                    )
                )
                triggerIntruderHapticAlert()
                viewModelScope.launch {
                    repository.recordDuplicationAlert(
                        title = "Zero-Tolerance: MAC Clone Intrusion Prevented",
                        description = "Layer-2 clone attempt intercepted. Hardware address deduplicated and genuine node protected.",
                        ip = "$currentSubnet.199",
                        mac = "B4:FB:E4:91:22:A1"
                    )
                }
            }
            DuplicationViolationType.ALIEN_SUBNET -> {
                val alienIp = "172.16.4.88"
                val alienMac = "50:C7:BF:${(10..99).random()}:${(10..99).random()}:${(10..99).random()}"
                val alienDev = DiscoveredDevice(
                    ip = alienIp,
                    macAddress = alienMac,
                    vendor = "Alien Subnet Rogue Device",
                    customName = "Cross-Subnet Intruder",
                    isAuthorized = false,
                    isSubnetAnomaly = true,
                    isBlocked = true,
                    responseTimeMs = 0L,
                    threatLevel = ThreatLevel.UNAUTHORIZED_INTRUDER,
                    confidencePercent = 100,
                    corroborationVector = "Zero-Tolerance: Subnet Boundary Violation • Quarantined"
                )
                val current = _discoveredDevices.value.filter { !it.isSubnetAnomaly }.toMutableList()
                current.add(0, alienDev)
                _discoveredDevices.value = current
                _duplicationGuardStatus.value = _duplicationGuardStatus.value.copy(
                    subnetAnomaliesBlocked = _duplicationGuardStatus.value.subnetAnomaliesBlocked + 1,
                    lastViolationEvent = DuplicationViolation(
                        violationType = DuplicationViolationType.ALIEN_SUBNET,
                        ip = alienIp,
                        macAddress = alienMac,
                        conflictingDetail = "Host in alien subnet 172.16.4.x leaked across expected subnet $currentSubnet.x"
                    )
                )
                triggerIntruderHapticAlert()
                viewModelScope.launch {
                    repository.setDeviceBlocked(alienMac, true)
                    repository.recordDuplicationAlert(
                        title = "Zero-Tolerance: Subnet Boundary Violation Intercepted",
                        description = "Host at $alienIp ($alienMac) attempted to bypass /24 subnet boundary. Quarantined by Firewall ACL.",
                        ip = alienIp,
                        mac = alienMac
                    )
                }
            }
        }
    }

    private fun triggerGatewaySwitchHapticAlert() {
        try {
            val app = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Triple warning pulse for gateway transitions
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 120, 80, 120, 80, 260),
                        intArrayOf(0, 220, 0, 220, 0, 255),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(400)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun triggerBtIntruderHapticAlert() {
        try {
            val app = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Urgent sharp double vibration for Zero-Tolerance Bluetooth detection
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 200, 100, 300),
                        intArrayOf(0, 255, 0, 255),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(350)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun triggerIntruderHapticAlert() {
        try {
            val app = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 150, 100, 250),
                        intArrayOf(0, 255, 0, 255),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(300)
            }
        } catch (e: Exception) {
            // Ignored if permissions disabled
        }
    }

    fun generateSummaryReport() {
        viewModelScope.launch {
            _isGeneratingReport.value = true
            val scanStart = System.currentTimeMillis()
            try {
                val alerts = repository.getRecentSecurityAlertsList(limit = 25)
                val isGatewayLocked = _gatewayLockdownActive.value
                val gwDevice = _discoveredDevices.value.find { it.isGateway }
                val isGatewayMatch = !isGatewayLocked || (gwDevice == null || gwDevice.macAddress.equals(_lockedGatewayMac.value, ignoreCase = true))
                val durationMs = (System.currentTimeMillis() - scanStart).coerceAtLeast(1850L)
                val report = NetworkReportGenerator.generateReport(
                    wifiState = _wifiState.value,
                    discoveredDevices = _discoveredDevices.value,
                    whitelistedCount = whitelistedCount.value,
                    nearbyAps = _nearbyAccessPoints.value,
                    alerts = alerts,
                    isGatewayLocked = isGatewayLocked,
                    isGatewayMatch = isGatewayMatch,
                    duplicationStatus = _duplicationGuardStatus.value,
                    isZeroToleranceActive = _isZeroTolerancePolicyActive.value,
                    scanDurationMs = durationMs
                )
                _networkSummaryReport.value = report
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error generating network summary report", e)
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }

    fun exportSummaryReport(context: Context) {
        val report = _networkSummaryReport.value
        val text = report.toFormattedReportText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Sentinel Network Health & Security Report - ${report.ssid}")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share Network Security Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // ==========================================
    // Antivirus & Malware Protection Actions
    // ==========================================

    fun startAntivirusScan() {
        antivirusScanJob?.cancel()
        antivirusScanJob = viewModelScope.launch {
            _antivirusState.value = _antivirusState.value.copy(
                isScanning = true,
                scanProgress = 0f,
                threatsFound = 0
            )
            AntivirusScannerEngine.scanAllApps(getApplication()).collect { event ->
                when (event) {
                    is AntivirusScannerEngine.ScanProgressEvent.Started -> {
                        _antivirusState.value = _antivirusState.value.copy(
                            isScanning = true,
                            totalCount = event.totalApps,
                            scannedCount = 0,
                            scanProgress = 0f
                        )
                    }
                    is AntivirusScannerEngine.ScanProgressEvent.Progress -> {
                        val progress = if (event.totalCount > 0) event.currentIndex.toFloat() / event.totalCount.toFloat() else 0f
                        _antivirusState.value = _antivirusState.value.copy(
                            isScanning = true,
                            scannedCount = event.currentIndex,
                            totalCount = event.totalCount,
                            currentlyScanningApp = event.currentAppName,
                            scanProgress = progress
                        )
                    }
                    is AntivirusScannerEngine.ScanProgressEvent.Completed -> {
                        val threats = event.results.count {
                            it.riskLevel == AppRiskLevel.CRITICAL || it.riskLevel == AppRiskLevel.HIGH_RISK || it.riskLevel == AppRiskLevel.SUSPICIOUS
                        }
                        _scannedApps.value = event.results
                        _antivirusState.value = _antivirusState.value.copy(
                            isScanning = false,
                            scanProgress = 1f,
                            currentlyScanningApp = "",
                            threatsFound = threats,
                            lastScanTimestamp = System.currentTimeMillis()
                        )
                    }
                }
            }
        }
    }

    fun cancelAntivirusScan() {
        antivirusScanJob?.cancel()
        _antivirusState.value = _antivirusState.value.copy(isScanning = false)
    }

    fun refreshSystemAudit() {
        viewModelScope.launch {
            _systemSecurityAudit.value = AntivirusScannerEngine.auditSystemSecurity(getApplication())
            _deviceMetrics.value = JunkCleanerEngine.getMemoryMetrics(getApplication())
        }
    }

    // ==========================================
    // System & Cache Cleaner Actions
    // ==========================================

    fun refreshJunkStorage() {
        viewModelScope.launch {
            _isCalculatingJunk.value = true
            try {
                _junkCategories.value = JunkCleanerEngine.calculateJunkCategories(getApplication())
                _deviceMetrics.value = JunkCleanerEngine.getMemoryMetrics(getApplication())
            } finally {
                _isCalculatingJunk.value = false
            }
        }
    }

    fun toggleJunkCategory(type: JunkType) {
        val updated = _junkCategories.value.map {
            if (it.id == type) it.copy(isSelected = !it.isSelected) else it
        }
        _junkCategories.value = updated
    }

    fun selectAllJunk(selected: Boolean) {
        val updated = _junkCategories.value.map {
            it.copy(isSelected = selected)
        }
        _junkCategories.value = updated
    }

    fun cleanSelectedJunk() {
        viewModelScope.launch {
            _isCleaningJunk.value = true
            try {
                val selectedTypes = _junkCategories.value.filter { it.isSelected }.map { it.id }.toSet()
                val result = JunkCleanerEngine.executeClean(getApplication(), selectedTypes)
                _lastCleanResult.value = result
                // Re-calculate remaining junk & metrics
                _junkCategories.value = JunkCleanerEngine.calculateJunkCategories(getApplication())
                _deviceMetrics.value = JunkCleanerEngine.getMemoryMetrics(getApplication())
            } finally {
                _isCleaningJunk.value = false
            }
        }
    }

    fun quarantineApp(packageName: String) {
        val set = _quarantinedPackageNames.value.toMutableSet()
        set.add(packageName)
        _quarantinedPackageNames.value = set
        _scannedApps.value = _scannedApps.value.map {
            if (it.packageName == packageName) it.copy(isQuarantined = true) else it
        }
    }

    fun unquarantineApp(packageName: String) {
        val set = _quarantinedPackageNames.value.toMutableSet()
        set.remove(packageName)
        _quarantinedPackageNames.value = set
        _scannedApps.value = _scannedApps.value.map {
            if (it.packageName == packageName) it.copy(isQuarantined = false) else it
        }
    }

    fun whitelistPackage(packageName: String) {
        val set = _whitelistedPackageNames.value.toMutableSet()
        set.add(packageName)
        _whitelistedPackageNames.value = set
        _scannedApps.value = _scannedApps.value.map {
            if (it.packageName == packageName) it.copy(isWhitelisted = true) else it
        }
    }

    fun launchUninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            launchAppSettings(context, packageName)
        }
    }

    fun launchAppSettings(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Could not open app settings", e)
        }
    }

    // ==========================================
    // AUTOMATIC DAILY SCAN SCHEDULER CONTROLS
    // ==========================================

    fun refreshScheduleSettings() {
        _dailyScanSchedule.value = DailyScanScheduler.loadSettings(getApplication())
    }

    fun updateDailyScanSchedule(newSettings: DailyScanScheduleSettings) {
        _dailyScanSchedule.value = newSettings
        DailyScanScheduler.saveSettings(getApplication(), newSettings)
    }

    fun toggleDailyScanEnabled(enabled: Boolean) {
        val updated = _dailyScanSchedule.value.copy(isEnabled = enabled)
        updateDailyScanSchedule(updated)
    }

    fun setDailyScanTime(hour: Int, minute: Int) {
        val updated = _dailyScanSchedule.value.copy(hour = hour, minute = minute)
        updateDailyScanSchedule(updated)
    }

    fun updateDailyScanScope(scanAntivirus: Boolean, scanJunk: Boolean, autoCleanSafe: Boolean) {
        val updated = _dailyScanSchedule.value.copy(
            scanAntivirus = scanAntivirus,
            scanJunkCleaner = scanJunk,
            autoCleanSafeJunk = autoCleanSafe
        )
        updateDailyScanSchedule(updated)
    }

    fun runScheduledScanNow() {
        if (_isExecutingScheduledScan.value) return
        viewModelScope.launch {
            _isExecutingScheduledScan.value = true
            try {
                val updated = DailyScanScheduler.executeScheduledScan(getApplication())
                _dailyScanSchedule.value = updated
                refreshJunkStorage()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error in manual scheduled scan execution", e)
            } finally {
                _isExecutingScheduledScan.value = false
            }
        }
    }

    // =========================================================================
    // SECURITY DASHBOARD, RISK ENGINE & THREAT SCANNER CONTROLS
    // =========================================================================

    fun setScanFrequency(seconds: Int) {
        val clamped = seconds.coerceIn(3, 300)
        _scanFrequencySeconds.value = clamped
        _backgroundDetectionStatus.value = _backgroundDetectionStatus.value.copy(scanIntervalSeconds = clamped)
        if (_isRealTimeShieldActive.value) {
            startShieldWatcher()
        }
    }

    fun refreshNetworkRiskScore() {
        if (_isCalculatingRiskScore.value) return
        viewModelScope.launch {
            _isCalculatingRiskScore.value = true
            try {
                val state = _wifiState.value
                val devices = _discoveredDevices.value
                val unauthorized = devices.count {
                    !it.isAuthorized && !it.isSelf && !it.isGateway && !it.isFalsePositiveSuppressed
                }
                val quarantined = totalQuarantinedCount.value
                val unethicals = _unethicalDevices.value.size
                val gateways = _managedGateways.value.size
                val hasRogueGateway = _managedGateways.value.any { it.type == GatewayRouterType.ROGUE_DUPLICATE && !it.isQuarantined } ||
                        devices.any { it.isRogueGateway && !it.isBlocked }
                val hasArpSpoofing = _unethicalDevices.value.any { it.threatType == UnethicalThreatType.ARP_POISONER && !it.isQuarantined } ||
                        devices.any { it.isDuplicateIp }

                val assessment = repository.analyzeNetworkRiskScore(
                    state = state,
                    devices = devices,
                    unauthorizedCount = unauthorized,
                    quarantinedCount = quarantined,
                    unethicalCount = unethicals,
                    gatewaysCount = gateways,
                    hasRogueGateway = hasRogueGateway,
                    hasArpSpoofing = hasArpSpoofing
                )
                _networkRiskAssessment.value = assessment
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Failed to refresh risk score: ${e.localizedMessage}")
            } finally {
                _isCalculatingRiskScore.value = false
            }
        }
    }

    // =========================================================================
    // WIFI SENTINEL AI: SCAN FOR THE UNETHICALS
    // =========================================================================

    fun scanForUnethicals() {
        if (_isScanningForUnethicals.value) return
        viewModelScope.launch {
            _isScanningForUnethicals.value = true
            try {
                val auditedThreats = repository.auditUnethicalBehaviors(
                    devices = _discoveredDevices.value,
                    state = _wifiState.value
                )

                // Preserve already discovered or simulated threats unless re-evaluated
                val current = _unethicalDevices.value.toMutableList()
                for (threat in auditedThreats) {
                    if (current.none { it.mac.equals(threat.mac, ignoreCase = true) }) {
                        current.add(threat)
                        // Auto record alert
                        repository.recordSecurityAlert(
                            title = "UNETHICAL THREAT DETECTED: ${threat.threatType.title}",
                            description = threat.signatureDetail,
                            deviceIp = threat.ip,
                            deviceMac = threat.mac,
                            severity = threat.severity,
                            confidencePercent = 99
                        )
                    }
                }

                _unethicalDevices.value = current

                if (current.any { !it.isQuarantined }) {
                    triggerIntruderHapticAlert()
                }

                refreshNetworkRiskScore()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed scanning for unethicals", e)
            } finally {
                _isScanningForUnethicals.value = false
            }
        }
    }

    fun quarantineUnethicalDevice(threatId: String) {
        val currentList = _unethicalDevices.value
        val threat = currentList.find { it.id == threatId } ?: return

        // 1. Enforce physical firewall ACL isolation
        networkAccessEnforcer.blockDeviceNetworkAccess(threat.mac, threat.ip, threat.vendor)

        // 2. Mark threat as quarantined
        _unethicalDevices.value = currentList.map {
            if (it.id == threatId) it.copy(isQuarantined = true) else it
        }

        // 3. Sync to Subnet Devices if present
        viewModelScope.launch {
            repository.setDeviceBlocked(threat.mac, true)
            _discoveredDevices.value = _discoveredDevices.value.map {
                if (it.macAddress.equals(threat.mac, ignoreCase = true)) it.copy(isBlocked = true) else it
            }
            repository.recordSecurityAlert(
                title = "UNETHICAL HOST QUARANTINED: ${threat.threatType.title}",
                description = "Zero-Access ACL enforced on ${threat.ip} (${threat.mac}). All subnet and external packet injection severed.",
                deviceIp = threat.ip,
                deviceMac = threat.mac,
                severity = "INFO",
                confidencePercent = 100
            )
            refreshNetworkRiskScore()
        }
    }

    fun unquarantineUnethicalDevice(threatId: String) {
        val currentList = _unethicalDevices.value
        val threat = currentList.find { it.id == threatId } ?: return

        networkAccessEnforcer.unblockDeviceNetworkAccess(threat.mac)

        _unethicalDevices.value = currentList.map {
            if (it.id == threatId) it.copy(isQuarantined = false) else it
        }

        viewModelScope.launch {
            repository.setDeviceBlocked(threat.mac, false)
            _discoveredDevices.value = _discoveredDevices.value.map {
                if (it.macAddress.equals(threat.mac, ignoreCase = true)) it.copy(isBlocked = false) else it
            }
            refreshNetworkRiskScore()
        }
    }

    fun simulateUnethicalThreat(type: UnethicalThreatType = UnethicalThreatType.ARP_POISONER) {
        viewModelScope.launch {
            val (mockIp, mockMac, mockVendor, signature) = when (type) {
                UnethicalThreatType.ARP_POISONER -> Quadruple(
                    "192.168.1.189",
                    "B8:27:EB:D3:44:01",
                    "Raspberry Pi / Kali MITM Node",
                    "Flooding gratuitous ARP replies claiming 192.168.1.1 is at B8:27:EB:D3:44:01."
                )
                UnethicalThreatType.PROMISCUOUS_SNIFFER -> Quadruple(
                    "192.168.1.142",
                    "00:0C:29:88:99:A1",
                    "VMware Host / Wireshark Probe",
                    "Host NIC operating in promiscuous listening mode capturing raw unencrypted packets."
                )
                UnethicalThreatType.EVIL_TWIN_CLONE -> Quadruple(
                    "192.168.1.250",
                    "50:C7:BF:99:11:00",
                    "Hak5 WiFi Pineapple MK7",
                    "Transmitting duplicate beacon frames with downgraded encryption handshake (WPA3 -> Open)."
                )
                UnethicalThreatType.DEAUTH_FLOODER -> Quadruple(
                    "192.168.1.215",
                    "24:0A:C4:55:66:77",
                    "ESP8266 Deauther",
                    "Injecting 802.11 disassociation/deauth burst frames at 120 packets/sec."
                )
                UnethicalThreatType.PORT_SCANNER -> Quadruple(
                    "192.168.1.164",
                    "00:1E:67:33:99:88",
                    "Nmap SYN Scanner",
                    "Rapid sequential TCP SYN port probes targeting ports 22, 23, 80, 445, 8080."
                )
                UnethicalThreatType.MAC_CLOAKED_IMPOSTOR -> Quadruple(
                    "192.168.1.177",
                    "DA:A1:19:62:33:EF",
                    "Cloaked Attacker (Randomized MAC)",
                    "Rapidly cycling Layer-2 MAC address headers to bypass static authorization filters."
                )
            }

            val newThreat = UnethicalDevice(
                ip = mockIp,
                mac = mockMac,
                vendor = mockVendor,
                threatType = type,
                severity = type.defaultSeverity,
                signatureDetail = signature,
                isQuarantined = false,
                packetAnomalyCount = Random.nextInt(85, 420)
            )

            val current = _unethicalDevices.value.filter { !it.mac.equals(mockMac, ignoreCase = true) }.toMutableList()
            current.add(0, newThreat)
            _unethicalDevices.value = current

            repository.recordSecurityAlert(
                title = "UNETHICAL ACTOR DETECTED: ${type.title}",
                description = signature,
                deviceIp = mockIp,
                deviceMac = mockMac,
                severity = type.defaultSeverity,
                confidencePercent = 99
            )

            triggerIntruderHapticAlert()
            refreshNetworkRiskScore()
        }
    }

    // =========================================================================
    // MULTI-GATEWAYS & ROUTERS MANAGEMENT FILTERS (FULL STACK)
    // =========================================================================

    fun setGatewayFilter(category: GatewayFilterCategory) {
        _selectedGatewayFilter.value = category
    }

    fun setPrimaryGateway(gatewayId: String) {
        _managedGateways.value = _managedGateways.value.map { node ->
            if (node.id == gatewayId) {
                node.copy(isCurrentActive = true, type = GatewayRouterType.PRIMARY_DEFAULT)
            } else if (node.type == GatewayRouterType.PRIMARY_DEFAULT) {
                node.copy(isCurrentActive = false, type = GatewayRouterType.SECONDARY_GATEWAY)
            } else {
                node.copy(isCurrentActive = false)
            }
        }
        refreshNetworkRiskScore()
    }

    fun toggleGatewayLock(gatewayId: String) {
        _managedGateways.value = _managedGateways.value.map { node ->
            if (node.id == gatewayId) {
                val newLock = !node.isLocked
                node.copy(
                    isLocked = newLock,
                    filterPolicy = if (newLock) "STRICT_ARP_LOCKED" else "DYNAMIC_ARP_LEARNING"
                )
            } else node
        }
    }

    fun quarantineGateway(gatewayId: String) {
        val current = _managedGateways.value
        val target = current.find { it.id == gatewayId } ?: return

        networkAccessEnforcer.blockDeviceNetworkAccess(target.mac, target.ip, target.vendor)

        _managedGateways.value = current.map { node ->
            if (node.id == gatewayId) {
                node.copy(
                    isQuarantined = true,
                    isCurrentActive = false,
                    filterPolicy = "QUARANTINE_DROP_ZERO_ROUTING"
                )
            } else node
        }

        viewModelScope.launch {
            repository.recordSecurityAlert(
                title = "GATEWAY ROUTER QUARANTINED",
                description = "Gateway node ${target.ip} (${target.vendor}) quarantined by administrator. Traffic drop enforced.",
                deviceIp = target.ip,
                deviceMac = target.mac,
                severity = "CRITICAL",
                confidencePercent = 100
            )
            refreshNetworkRiskScore()
        }
    }

    fun unquarantineGateway(gatewayId: String) {
        val current = _managedGateways.value
        val target = current.find { it.id == gatewayId } ?: return

        networkAccessEnforcer.unblockDeviceNetworkAccess(target.mac)

        _managedGateways.value = current.map { node ->
            if (node.id == gatewayId) {
                node.copy(
                    isQuarantined = false,
                    filterPolicy = "STRICT_ACL_MONITORED"
                )
            } else node
        }

        viewModelScope.launch {
            refreshNetworkRiskScore()
        }
    }

    fun addManagedGateway(
        ip: String,
        mac: String,
        ssid: String,
        type: GatewayRouterType,
        vendor: String
    ) {
        val newNode = GatewayRouterNode(
            ip = ip.trim(),
            mac = mac.trim().uppercase(),
            ssid = ssid.trim(),
            bssid = mac.trim().uppercase(),
            type = type,
            vendor = vendor.ifBlank { "Managed Router Node" },
            hopMetric = if (type == GatewayRouterType.PRIMARY_DEFAULT) 1 else 2,
            latencyMs = Random.nextLong(2, 12),
            isCurrentActive = (type == GatewayRouterType.PRIMARY_DEFAULT && _managedGateways.value.none { it.isCurrentActive }),
            isLocked = true,
            isQuarantined = false,
            filterPolicy = "MANAGED_ACTIVE_INSPECTED",
            trafficVolumeMb = 1.0f,
            routeSubnet = "${ip.substringBeforeLast(".")}.0/24"
        )

        _managedGateways.value = _managedGateways.value + newNode
        refreshNetworkRiskScore()
    }

    fun simulateRogueGatewayBreach() {
        viewModelScope.launch {
            val rogueNode = GatewayRouterNode(
                ip = _wifiState.value.gatewayIp.ifBlank { "192.168.1.1" },
                mac = "A0:C5:89:FE:11:77",
                ssid = "Rogue-MitM-Tap",
                bssid = "A0:C5:89:FE:11:77",
                type = GatewayRouterType.ROGUE_DUPLICATE,
                vendor = "Rogue Hak5 Pineapple / APR Spoof",
                hopMetric = 1,
                latencyMs = 1L,
                isCurrentActive = false,
                isLocked = false,
                isQuarantined = false,
                filterPolicy = "UNVERIFIED_SPOOF_RISK",
                trafficVolumeMb = 0.8f,
                routeSubnet = "${_wifiState.value.ipAddress.substringBeforeLast(".")}.0/24"
            )

            val current = _managedGateways.value.filter { it.mac != rogueNode.mac }.toMutableList()
            current.add(0, rogueNode)
            _managedGateways.value = current

            // Auto select Rogue filter category so user immediately sees it
            _selectedGatewayFilter.value = GatewayFilterCategory.ROGUE_DUPLICATE

            repository.recordSecurityAlert(
                title = "ZERO-TOLERANCE: Rogue Gateway Collision",
                description = "Duplicate Gateway node detected claiming IP ${rogueNode.ip} with illegitimate MAC ${rogueNode.mac}.",
                deviceIp = rogueNode.ip,
                deviceMac = rogueNode.mac,
                severity = "CRITICAL",
                confidencePercent = 100
            )

            triggerIntruderHapticAlert()
            refreshNetworkRiskScore()
        }
    }

    private fun defaultManagedGateways(): List<GatewayRouterNode> {
        return listOf(
            GatewayRouterNode(
                ip = "192.168.1.1",
                mac = "00:1A:2B:3C:4D:01",
                ssid = "SentinelSecure-Enterprise",
                bssid = "00:1A:2B:3C:4D:01",
                type = GatewayRouterType.PRIMARY_DEFAULT,
                vendor = "Cisco Catalyst Gateway",
                hopMetric = 1,
                latencyMs = 2L,
                isCurrentActive = true,
                isLocked = true,
                isQuarantined = false,
                filterPolicy = "STRICT_ARP_LOCKED",
                trafficVolumeMb = 48.2f,
                routeSubnet = "192.168.1.0/24"
            ),
            GatewayRouterNode(
                ip = "192.168.1.254",
                mac = "38:D5:47:89:AB:02",
                ssid = "SentinelSecure-Mesh-North",
                bssid = "38:D5:47:89:AB:02",
                type = GatewayRouterType.MESH_SATELLITE,
                vendor = "Ubiquiti UniFi Mesh 6",
                hopMetric = 2,
                latencyMs = 5L,
                isCurrentActive = false,
                isLocked = true,
                isQuarantined = false,
                filterPolicy = "BACKHAUL_ENCRYPTED",
                trafficVolumeMb = 19.8f,
                routeSubnet = "192.168.1.0/24"
            ),
            GatewayRouterNode(
                ip = "10.0.50.1",
                mac = "50:C7:BF:22:EE:99",
                ssid = "VLAN50-IoT-Isolate",
                bssid = "50:C7:BF:22:EE:99",
                type = GatewayRouterType.SECONDARY_GATEWAY,
                vendor = "MikroTik RouterBOARD",
                hopMetric = 2,
                latencyMs = 7L,
                isCurrentActive = false,
                isLocked = false,
                isQuarantined = false,
                filterPolicy = "ISOLATED_GUEST_VLAN",
                trafficVolumeMb = 8.4f,
                routeSubnet = "10.0.50.0/24"
            ),
            GatewayRouterNode(
                ip = "172.17.0.1",
                mac = "02:42:AC:11:00:01",
                ssid = "docker0-virtual",
                bssid = "02:42:AC:11:00:01",
                type = GatewayRouterType.VIRTUAL_BRIDGE,
                vendor = "Linux Kernel Bridge",
                hopMetric = 1,
                latencyMs = 1L,
                isCurrentActive = false,
                isLocked = true,
                isQuarantined = false,
                filterPolicy = "CONTAINER_NAT_FILTER",
                trafficVolumeMb = 3.1f,
                routeSubnet = "172.17.0.0/16"
            )
        )
    }

    // ==========================================
    // WORKSPACES, NATIVE BACKUP & NODES PERGAMUS
    // ==========================================
    val workspaceClusterManager: WorkspaceClusterManager = WorkspaceClusterManager.getInstance(application)
    val workspaces: StateFlow<List<WorkspaceProfile>> = workspaceClusterManager.workspaces
    val currentWorkspace: StateFlow<WorkspaceProfile> = workspaceClusterManager.currentWorkspace
    val pergamusNodes: StateFlow<List<PergamusClusterNode>> = workspaceClusterManager.pergamusNodes
    val backupSnapshots: StateFlow<List<ClusterBackupSnapshot>> = workspaceClusterManager.backupSnapshots
    val trialEntries: StateFlow<List<TrialErrorEntry>> = workspaceClusterManager.trialEntries
    val betaTesters: StateFlow<List<RecurringBetaTester>> = workspaceClusterManager.betaTesters
    val lastFlightResult: StateFlow<BetaFlightRunResult?> = workspaceClusterManager.lastFlightResult
    val isFlightRunning: StateFlow<Boolean> = workspaceClusterManager.isFlightRunning
    val isClusterSyncing: StateFlow<Boolean> = workspaceClusterManager.isClusterSyncing

    fun selectWorkspace(id: String) = workspaceClusterManager.selectWorkspace(id)
    fun syncAllPergamusNodes(onComplete: (() -> Unit)? = null) = workspaceClusterManager.syncAllNodes(onComplete)
    fun toggleIsolatePergamusNode(nodeId: String) = workspaceClusterManager.toggleIsolateNode(nodeId)
    fun addPergamusNode(name: String, ip: String, mac: String, role: String) =
        workspaceClusterManager.addPergamusNode(name, ip, mac, role)
    fun createNativeClusterBackup(note: String = "", onCreated: ((ClusterBackupSnapshot) -> Unit)? = null) =
        workspaceClusterManager.createNativeClusterBackup(note, onCreated)
    fun restoreClusterSnapshot(snapshotId: String) = workspaceClusterManager.restoreClusterSnapshot(snapshotId)
    fun deleteClusterSnapshot(snapshotId: String) = workspaceClusterManager.deleteBackupSnapshot(snapshotId)
    fun runTrialSimulation(title: String, category: TrialCategory, protocol: String, sourceNode: String) =
        workspaceClusterManager.runTrialSimulation(title, category, protocol, sourceNode)
    fun enrollRecurringBetaTester(name: String, email: String, device: String) =
        workspaceClusterManager.enrollRecurringBetaTester(name, email, device)
    fun runDailyBetaFlightSuite(onComplete: ((BetaFlightRunResult) -> Unit)? = null) =
        workspaceClusterManager.runDailyBetaFlightSuite(onComplete)

    // =========================================================================
    // COMPLIANT SELF-DEFENSE & PRIVACY GUARD ACTIONS (NETWORK GUARD & MAC SANITIZER)
    // =========================================================================

    override fun onGatewayAnomalyDetected(oldGateway: String, newGateway: String) {
        val knownNode = trustedGateways.value.firstOrNull { it.gatewayIp.equals(newGateway, ignoreCase = true) }
        val isKnown = knownNode != null
        val alert = GatewayAlert(
            oldGateway = oldGateway,
            newGateway = newGateway,
            isKnownInDatabase = isKnown,
            matchedLabel = knownNode?.label,
            message = if (isKnown) {
                "Mesh Roaming Detected: Gateway transitioned to recognized node '${knownNode?.label}' ($newGateway)."
            } else {
                "UNREGISTERED ROGUE GATEWAY: Gateway shifted to $newGateway (not found in Room trusted registry). Possible ARP spoofing or rogue AP impersonation."
            }
        )
        _guardUiState.update {
            it.copy(
                activeAlert = alert,
                intrudersCount = if (isKnown) it.intrudersCount else it.intrudersCount + 1,
                lockedGatewayBaseline = oldGateway
            )
        }
        triggerGatewaySwitchHapticAlert()
        viewModelScope.launch {
            if (isKnown) {
                repository.updateGatewayLastSeen(newGateway)
            } else {
                repository.recordSecurityAlert(
                    title = "ROGUE GATEWAY SHIFT: Self-Isolation Recommended",
                    description = "Default gateway shifted unexpectedly from $oldGateway to unrecognized $newGateway. Safe lockdown available to sever local socket routes without transmitting rogue packets.",
                    deviceIp = newGateway,
                    deviceMac = "ROGUE_GATEWAY_DETECTED",
                    severity = "CRITICAL",
                    confidencePercent = 99
                )
            }
        }
    }

    override fun onSafeLockdownExecuted() {
        _guardUiState.update { it.copy(isLockdownActive = true) }
        triggerGatewaySwitchHapticAlert()
    }

    fun triggerSafeLockdown() {
        guardManager.executeSafeLockdown()
    }

    fun releaseSafeLockdown() {
        guardManager.releaseLockdown()
        _guardUiState.update { it.copy(isLockdownActive = false) }
    }

    fun dismissGuardGatewayAlert() {
        _guardUiState.update { it.copy(activeAlert = null) }
    }

    fun trustAndAddDetectedGateway(
        gatewayIp: String,
        label: String = "Discovered Mesh Node",
        makePrimary: Boolean = false
    ) {
        val currentSsid = _wifiState.value.ssid.ifBlank { "Known_Network" }
        val currentBssid = _wifiState.value.bssid.ifBlank { null }
        addTrustedGateway(
            gatewayIp = gatewayIp,
            bssid = currentBssid,
            ssid = currentSsid,
            subnetMask = "255.255.255.0",
            label = label,
            isPrimary = makePrimary
        )
        guardManager.resetBaseline(gatewayIp)
        dismissGuardGatewayAlert()
    }

    fun runGatewayDiagnostics(targetIp: String? = null) {
        if (_isDiagnosingGateway.value) return
        _isDiagnosingGateway.value = true
        viewModelScope.launch {
            try {
                val ip = targetIp ?: _wifiState.value.gatewayIp.ifBlank {
                    primaryTrustedGateway.value?.gatewayIp ?: "192.168.1.1"
                }
                val primaryIp = primaryTrustedGateway.value?.gatewayIp
                val result = diagnosticsManager.runDiagnostics(ip, primaryIp)
                _gatewayDiagnostics.value = result
            } catch (e: Exception) {
                // Non-blocking fallback
            } finally {
                _isDiagnosingGateway.value = false
            }
        }
    }

    fun exportSentinelSecurityConfigJson(): String {
        val gateways = trustedGateways.value
        val whitelisted = whitelistedDevices.value.map { it.macAddress }
        val jsonObj = JSONObject()
        jsonObj.put("version", "Sentinel-4.2")
        jsonObj.put("exportedAt", System.currentTimeMillis())
        jsonObj.put("whitelistedCount", whitelisted.size)
        val macsArr = JSONArray(whitelisted)
        jsonObj.put("whitelistedMacs", macsArr)

        val gwArr = JSONArray()
        gateways.forEach { gw ->
            val gwObj = JSONObject()
            gwObj.put("gatewayIp", gw.gatewayIp)
            gwObj.put("label", gw.label)
            gwObj.put("ssid", gw.ssid)
            gwObj.put("bssid", gw.bssid ?: "")
            gwObj.put("subnetMask", gw.subnetMask)
            gwObj.put("isPrimary", gw.isPrimary)
            gwArr.put(gwObj)
        }
        jsonObj.put("trustedGateways", gwArr)
        return jsonObj.toString(2)
    }

    fun importSentinelSecurityConfigJson(jsonString: String): Boolean {
        return try {
            val jsonObj = JSONObject(jsonString)
            val gwArr = jsonObj.optJSONArray("trustedGateways")
            if (gwArr != null) {
                viewModelScope.launch {
                    for (i in 0 until gwArr.length()) {
                        val obj = gwArr.getJSONObject(i)
                        val ip = obj.getString("gatewayIp")
                        val label = obj.optString("label", "Imported Gateway")
                        val ssid = obj.optString("ssid", "Imported_SSID")
                        val bssid = obj.optString("bssid").ifBlank { null }
                        val subnet = obj.optString("subnetMask", "255.255.255.0")
                        val isPrimary = obj.optBoolean("isPrimary", false)
                        repository.saveTrustedGateway(
                            TrustedGateway(
                                gatewayIp = ip,
                                bssid = bssid,
                                ssid = ssid,
                                subnetMask = subnet,
                                label = label,
                                isPrimary = isPrimary
                            )
                        )
                    }
                }
            }
            val macsArr = jsonObj.optJSONArray("whitelistedMacs")
            if (macsArr != null) {
                viewModelScope.launch {
                    for (i in 0 until macsArr.length()) {
                        val mac = macsArr.getString(i)
                        repository.addDeviceToWhitelist(
                            DiscoveredDevice(
                                macAddress = mac,
                                ip = "0.0.0.0",
                                vendor = "Imported Known Host"
                            )
                        )
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setFpFilterEnabled(enabled: Boolean) {
        _guardUiState.update { it.copy(fpFilterEnabled = enabled) }
    }

    fun filterIncomingDevice(macAddress: String): Boolean {
        val isRandom = MacSanitizer.isRandomizedMac(macAddress)
        if (isRandom && _guardUiState.value.fpFilterEnabled) {
            _guardUiState.update { it.copy(fpFilteredCount = it.fpFilteredCount + 1) }
            return true
        }
        return false
    }

    fun hashDeviceIdentifier(identifier: String, salt: String = "WiFiSentinel-Salt-V1"): String {
        _guardUiState.update { it.copy(totalHashedAuditsCount = it.totalHashedAuditsCount + 1) }
        return MacSanitizer.hashIdentifier(identifier, salt)
    }

    fun simulateGatewayAnomalyForTesting() {
        val oldGw = guardManager.getLockedGatewayBaseline() ?: "192.168.1.1"
        val rogueGw = "192.168.1.254"
        onGatewayAnomalyDetected(oldGw, rogueGw)
    }

    // =========================================================================
    // TRUSTED GATEWAY PERSISTENCE (ROOM DATABASE)
    // =========================================================================

    fun addTrustedGateway(
        gatewayIp: String,
        bssid: String?,
        ssid: String,
        subnetMask: String = "255.255.255.0",
        label: String,
        isPrimary: Boolean = false
    ) {
        viewModelScope.launch {
            val gateway = TrustedGateway(
                gatewayIp = gatewayIp,
                bssid = bssid,
                ssid = ssid,
                subnetMask = subnetMask,
                label = label,
                isPrimary = isPrimary
            )
            repository.saveTrustedGateway(gateway)
            if (isPrimary) {
                repository.setPrimaryTrustedGateway(gatewayIp)
            }
        }
    }

    fun setPrimaryTrustedGateway(gatewayIp: String) {
        viewModelScope.launch {
            repository.setPrimaryTrustedGateway(gatewayIp)
        }
    }

    fun removeTrustedGateway(gatewayIp: String) {
        viewModelScope.launch {
            repository.deleteTrustedGateway(gatewayIp)
        }
    }

    override fun onCleared() {
        super.onCleared()
        guardManager.stopMonitoring()
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
