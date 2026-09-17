package com.example.security

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Compliant Security Dashboard ViewModel wired directly to NetworkGuardManager and MacSanitizer.
 * Enforces host self-isolation during critical gateway shifts and privacy-safe MAC hashing.
 */
class SecurityDashboardViewModel(application: Application) : AndroidViewModel(application), GuardEventListener {

    private val _uiState = MutableStateFlow(SecurityDashboardUiState())
    val uiState: StateFlow<SecurityDashboardUiState> = _uiState.asStateFlow()

    private val guardManager = NetworkGuardManager(application, this)

    init {
        guardManager.startMonitoring()
    }

    override fun onGatewayAnomalyDetected(oldGateway: String, newGateway: String) {
        _uiState.update {
            it.copy(
                activeAlert = GatewayAlert(oldGateway = oldGateway, newGateway = newGateway),
                intrudersCount = it.intrudersCount + 1
            )
        }
    }

    override fun onSafeLockdownExecuted() {
        _uiState.update { it.copy(isLockdownActive = true) }
    }

    fun triggerLockdown() {
        guardManager.executeSafeLockdown()
    }

    fun releaseLockdown() {
        guardManager.releaseLockdown()
        _uiState.update { it.copy(isLockdownActive = false) }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(activeAlert = null) }
    }

    fun setFpFilterEnabled(enabled: Boolean) {
        _uiState.update { it.copy(fpFilterEnabled = enabled) }
    }

    fun filterIncomingDevice(macAddress: String): Boolean {
        // If it's an ephemeral/randomized MAC, treat as benign noise rather than an active attack
        val isRandom = MacSanitizer.isRandomizedMac(macAddress)
        if (isRandom && _uiState.value.fpFilterEnabled) {
            _uiState.update { it.copy(fpFilteredCount = it.fpFilteredCount + 1) }
            return true // Filtered out
        }
        return false // Requires inspection
    }

    fun hashIdentifier(identifier: String, salt: String = "WiFiSentinel-Salt-V1"): String {
        _uiState.update { it.copy(totalHashedAuditsCount = it.totalHashedAuditsCount + 1) }
        return MacSanitizer.hashIdentifier(identifier, salt)
    }

    fun simulateGatewayAnomaly(oldGateway: String = "192.168.1.1", newGateway: String = "192.168.1.254") {
        onGatewayAnomalyDetected(oldGateway, newGateway)
    }

    override fun onCleared() {
        super.onCleared()
        guardManager.stopMonitoring()
    }
}
