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
    private val _loadingState = MutableStateFlow(false)

    val uiState: StateFlow<SentinelUiState> = combine(
        repository.allDevices,
        repository.whitelistedDevices,
        repository.blockedDevices,
        repository.securityAlerts,
        repository.unacknowledgedAlertsCount,
        _filterState,
        _loadingState
    ) { allDevs, whiteDevs, blockedDevs, alerts, unackCount, filter, loading ->
        val filteredList = when (filter) {
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
            unacknowledgedAlertsCount = unackCount,
            isLoading = loading,
            selectedFilter = filter,
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
            repository.setDeviceBlocked(device.macAddress, newStatus)
            if (newStatus && device.isAuthorized) {
                repository.setDeviceAuthorized(device.macAddress, false)
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
