package com.example.wifisentinel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifisentinel.data.local.SentinelDeviceEntity
import com.example.wifisentinel.data.repository.SentinelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Tactical Architecture ViewModel coordinating UI State with SentinelRepository.
 * Manages POTRAZ compliance, live device inventory, and authorization policies.
 */
open class SentinelViewModel(
    private val repository: SentinelRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(DeviceFilter.ALL)
    private val _isAutonomousQuarantineRemovalActive = MutableStateFlow(true)
    private val _evictionMessage = MutableStateFlow("")

    private data class LocalControls(
        val filter: DeviceFilter,
        val autoRemove: Boolean,
        val evictionMsg: String
    )

    private val localControlsFlow = combine(
        _filterState,
        _isAutonomousQuarantineRemovalActive,
        _evictionMessage
    ) { filter, autoRemove, evictionMsg ->
        LocalControls(filter, autoRemove, evictionMsg)
    }

    val uiState: StateFlow<SentinelUiState> = combine(
        repository.allDevices,
        repository.whitelistedDevices,
        repository.blockedDevices,
        repository.securityAlerts,
        localControlsFlow
    ) { allDevs, whiteDevs, blockedDevs, alerts, controls ->
        val filteredList = when (controls.filter) {
            DeviceFilter.ALL -> allDevs
            DeviceFilter.WHITELISTED -> whiteDevs
            DeviceFilter.BLOCKED -> blockedDevs
            DeviceFilter.RANDOMIZED_LAA -> allDevs.filter { it.isRandomizedMac }
        }

        SentinelUiState(
            devices = filteredList,
            whitelistedDevices = whiteDevs,
            blockedDevices = blockedDevs,
            alerts = alerts,
            unacknowledgedAlertsCount = alerts.count { !it.isAcknowledged },
            isLoading = false,
            selectedFilter = controls.filter,
            isAutonomousQuarantineRemovalActive = controls.autoRemove,
            lastEvictionMessage = controls.evictionMsg,
            complianceStatus = "POTRAZ Chapter 12:07 Invariant 0% Drift",
            classification = "BENEDICTUS"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SentinelUiState(isLoading = true)
    )

    fun setFilter(filter: DeviceFilter) {
        _filterState.value = filter
    }

    fun setAutonomousQuarantineRemoval(enabled: Boolean) {
        _isAutonomousQuarantineRemovalActive.value = enabled
        _evictionMessage.value = if (enabled) {
            "Autonomous Quarantine Removal Enabled (G4035 Sentinel Policy Active)"
        } else {
            "Autonomous Quarantine Removal Suspended"
        }
    }

    /**
     * Actively evicts all quarantined devices from the network and deletes their records.
     * Complies with G4035 autonomous quarantine policy and POTRAZ guidelines.
     */
    fun removeQuarantinedDevicesFromNetwork() {
        viewModelScope.launch {
            val purgedCount = repository.removeQuarantinedDevicesFromNetwork()
            val msg = "Evicted $purgedCount quarantined device(s) from network (ACL Drop & ARP Eviction Enforced)"
            _evictionMessage.value = msg
            repository.recordSecurityAlert(
                title = "NETWORK QUARANTINE PURGE",
                description = msg,
                deviceIp = "255.255.255.255",
                deviceMac = "FF:FF:FF:FF:FF:FF",
                severity = "INFO"
            )
        }
    }

    /**
     * Removes an individual quarantined device from the network.
     */
    fun removeQuarantinedDevice(device: SentinelDeviceEntity) {
        viewModelScope.launch {
            val removed = repository.removeQuarantinedDevice(device.macAddress)
            if (removed) {
                val msg = "Severed and evicted quarantined host ${device.ipAddress} (${device.macAddress}) from network."
                _evictionMessage.value = msg
                repository.recordSecurityAlert(
                    title = "QUARANTINED HOST EVICTED",
                    description = msg,
                    deviceIp = device.ipAddress,
                    deviceMac = device.macAddress,
                    severity = "WARNING"
                )
            }
        }
    }

    fun toggleAuthorization(device: SentinelDeviceEntity) {
        viewModelScope.launch {
            val newStatus = !device.isAuthorized
            repository.setDeviceAuthorized(device.macAddress, newStatus)
            if (newStatus && device.isBlocked) {
                repository.setDeviceBlocked(device.macAddress, false)
            }
        }
    }

    fun toggleBlock(device: SentinelDeviceEntity) {
        viewModelScope.launch {
            val newStatus = !device.isBlocked
            if (newStatus && _isAutonomousQuarantineRemovalActive.value) {
                // Autonomous configuration rule: immediately sever and remove quarantined device from network
                repository.setDeviceBlocked(device.macAddress, true)
                repository.removeQuarantinedDevice(device.macAddress)
                val msg = "Autonomous Policy: Quarantined host ${device.ipAddress} (${device.macAddress}) severed and removed from network."
                _evictionMessage.value = msg
                repository.recordSecurityAlert(
                    title = "AUTONOMOUS QUARANTINE REMOVAL",
                    description = msg,
                    deviceIp = device.ipAddress,
                    deviceMac = device.macAddress,
                    severity = "CRITICAL"
                )
            } else {
                repository.setDeviceBlocked(device.macAddress, newStatus)
                if (newStatus && device.isAuthorized) {
                    repository.setDeviceAuthorized(device.macAddress, false)
                }
            }
        }
    }

    fun setCustomName(mac: String, name: String) {
        viewModelScope.launch {
            repository.setCustomName(mac, name)
        }
    }

    fun acknowledgeAlert(alertId: Long) {
        viewModelScope.launch {
            repository.acknowledgeAlert(alertId)
        }
    }

    fun ingestScannedDevice(rawMac: String, ipAddress: String, vendor: String = "Unknown") {
        viewModelScope.launch {
            repository.ingestDiscoveredDevice(
                rawMac = rawMac,
                ipAddress = ipAddress,
                vendor = vendor
            )
        }
    }
}
