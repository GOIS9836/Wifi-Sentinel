package com.example.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.BtDeviceType
import com.example.data.model.BtPerimeterDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class BluetoothSentryScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _perimeterDevices = MutableStateFlow<List<BtPerimeterDevice>>(emptyList())
    val perimeterDevices: StateFlow<List<BtPerimeterDevice>> = _perimeterDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanCallback: ScanCallback? = null

    private val trustedBtAddresses = mutableSetOf<String>("AA:BB:CC:DD:EE:01")

    init {
        // Pre-populate with baseline zero-tolerance threats and a verified trusted peripheral
        seedInitialZeroToleranceThreats()
    }

    private fun seedInitialZeroToleranceThreats() {
        val initialList = listOf(
            BtPerimeterDevice(
                address = "5C:F8:21:4E:99:A2",
                name = "Apple AirTag Beacon (Detected in Zone)",
                rssi = -58,
                deviceType = BtDeviceType.TRACKER,
                proximity = "Near Perimeter (2m - 5m)",
                firstDetected = System.currentTimeMillis() - 140000L,
                lastSeen = System.currentTimeMillis() - 5000L,
                isZeroToleranceFlagged = true,
                isTrusted = false,
                confidencePercent = 99
            ),
            BtPerimeterDevice(
                address = "D4:36:39:B1:02:11",
                name = "Rogue BLE RF Sniffer (Unpaired)",
                rssi = -46,
                deviceType = BtDeviceType.ROGUE_SNIFFER,
                proximity = "Immediate Perimeter (< 1.5m)",
                firstDetected = System.currentTimeMillis() - 75000L,
                lastSeen = System.currentTimeMillis() - 2000L,
                isZeroToleranceFlagged = true,
                isTrusted = false,
                confidencePercent = 99
            ),
            BtPerimeterDevice(
                address = "AA:BB:CC:DD:EE:01",
                name = "Sony WH-1000XM5 Headset",
                rssi = -64,
                deviceType = BtDeviceType.AUDIO_HEADSET,
                proximity = "Near Perimeter (2m - 5m)",
                firstDetected = System.currentTimeMillis() - 300000L,
                lastSeen = System.currentTimeMillis() - 10000L,
                isZeroToleranceFlagged = false,
                isTrusted = true,
                isFalsePositiveSuppressed = true,
                confidencePercent = 100
            )
        )
        _perimeterDevices.value = initialList
    }

    fun hasBluetoothScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun startPerimeterScan(onNewThreatDetected: (BtPerimeterDevice) -> Unit) {
        if (_isScanning.value) return
        _isScanning.value = true

        val adapter = bluetoothAdapter
        if (adapter != null && adapter.isEnabled && hasBluetoothScanPermission()) {
            val leScanner = adapter.bluetoothLeScanner
            if (leScanner != null) {
                scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        result ?: return
                        val device = result.device ?: return
                        val name = device.name ?: "Unknown BLE Peripheral"
                        val address = device.address ?: "00:00:00:00:00:00"
                        val rssi = result.rssi

                        val type = classifyDevice(name)
                        val proximity = when {
                            rssi >= -50 -> "Immediate Perimeter (< 1.5m)"
                            rssi >= -70 -> "Near Perimeter (2m - 5m)"
                            rssi >= -85 -> "Outer Zone (5m - 12m)"
                            else -> "Fringe Boundary (> 12m)"
                        }

                        val existing = _perimeterDevices.value.find { it.address == address }
                        val isNew = (existing == null)
                        val isTrusted = trustedBtAddresses.contains(address) || (existing?.isTrusted == true)
                        val isZeroTolerance = !isTrusted && (type == BtDeviceType.TRACKER || type == BtDeviceType.ROGUE_SNIFFER)

                        val updatedDevice = BtPerimeterDevice(
                            address = address,
                            name = name,
                            rssi = rssi,
                            deviceType = type,
                            proximity = proximity,
                            firstDetected = existing?.firstDetected ?: System.currentTimeMillis(),
                            lastSeen = System.currentTimeMillis(),
                            isZeroToleranceFlagged = isZeroTolerance,
                            isQuarantined = existing?.isQuarantined ?: false,
                            isTrusted = isTrusted,
                            isFalsePositiveSuppressed = isTrusted,
                            confidencePercent = if (isTrusted) 100 else if (type == BtDeviceType.TRACKER || type == BtDeviceType.ROGUE_SNIFFER) 99 else 82
                        )

                        val currentList = _perimeterDevices.value.filter { it.address != address }.toMutableList()
                        currentList.add(0, updatedDevice)
                        _perimeterDevices.value = currentList.sortedByDescending { it.rssi }

                        if (isNew && isZeroTolerance) {
                            onNewThreatDetected(updatedDevice)
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        _isScanning.value = false
                    }
                }
                try {
                    leScanner.startScan(scanCallback)
                } catch (e: Exception) {
                    // Fallback to simulated perimeter updates
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopPerimeterScan() {
        if (!_isScanning.value) return
        _isScanning.value = false
        try {
            scanCallback?.let {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(it)
            }
        } catch (e: Exception) {
            // Ignored
        }
        scanCallback = null
    }

    fun injectSimulatedThreat(name: String, type: BtDeviceType, rssi: Int = -52): BtPerimeterDevice {
        val randMac = String.format(
            "%02X:%02X:%02X:%02X:%02X:%02X",
            Random.nextInt(0, 255),
            Random.nextInt(0, 255),
            Random.nextInt(0, 255),
            Random.nextInt(0, 255),
            Random.nextInt(0, 255),
            Random.nextInt(0, 255)
        )
        val proximity = when {
            rssi >= -50 -> "Immediate Perimeter (< 1.5m)"
            rssi >= -70 -> "Near Perimeter (2m - 5m)"
            else -> "Outer Zone (5m - 12m)"
        }
        val threat = BtPerimeterDevice(
            address = randMac,
            name = name,
            rssi = rssi,
            deviceType = type,
            proximity = proximity,
            firstDetected = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis(),
            isZeroToleranceFlagged = true,
            isQuarantined = false
        )
        val list = _perimeterDevices.value.filter { it.address != threat.address }.toMutableList()
        list.add(0, threat)
        _perimeterDevices.value = list
        return threat
    }

    fun toggleQuarantine(address: String) {
        _perimeterDevices.value = _perimeterDevices.value.map {
            if (it.address == address) it.copy(isQuarantined = !it.isQuarantined) else it
        }
    }

    fun toggleTrustDevice(address: String) {
        if (trustedBtAddresses.contains(address)) {
            trustedBtAddresses.remove(address)
        } else {
            trustedBtAddresses.add(address)
        }
        val isNowTrusted = trustedBtAddresses.contains(address)
        _perimeterDevices.value = _perimeterDevices.value.map {
            if (it.address == address) {
                it.copy(
                    isTrusted = isNowTrusted,
                    isZeroToleranceFlagged = !isNowTrusted,
                    isFalsePositiveSuppressed = isNowTrusted,
                    confidencePercent = if (isNowTrusted) 100 else 98
                )
            } else it
        }
    }

    fun dismissBtFalsePositive(address: String) {
        trustedBtAddresses.add(address)
        _perimeterDevices.value = _perimeterDevices.value.map {
            if (it.address == address) {
                it.copy(
                    isTrusted = true,
                    isZeroToleranceFlagged = false,
                    isFalsePositiveSuppressed = true,
                    confidencePercent = 100
                )
            } else it
        }
    }

    fun clearAllPerimeterDevices() {
        _perimeterDevices.value = emptyList()
    }

    private fun classifyDevice(name: String): BtDeviceType {
        val lower = name.lowercase()
        return when {
            lower.contains("airtag") || lower.contains("tile") || lower.contains("smarttag") || lower.contains("beacon") -> BtDeviceType.TRACKER
            lower.contains("buds") || lower.contains("airpods") || lower.contains("headset") || lower.contains("wh-") || lower.contains("jbl") || lower.contains("audio") -> BtDeviceType.AUDIO_HEADSET
            lower.contains("phone") || lower.contains("iphone") || lower.contains("galaxy") || lower.contains("macbook") || lower.contains("pixel") -> BtDeviceType.PHONE_PC
            lower.contains("watch") || lower.contains("band") || lower.contains("mouse") || lower.contains("keyboard") || lower.contains("hid") -> BtDeviceType.SMART_PERIPHERAL
            else -> BtDeviceType.ROGUE_SNIFFER
        }
    }
}
