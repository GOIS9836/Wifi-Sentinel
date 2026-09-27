package com.example.service

/**
 * Tactical Classification of an IoT Chipset / Embedded Microcontroller.
 */
data class IotChipsetProfile(
    val isIotChipset: Boolean,
    val vendorName: String,
    val chipsetFamily: String,
    val riskClassification: String,
    val isOffensiveAuditHardware: Boolean = false,
    val potrazComplianceAdvisory: String
)

/**
 * Dedicated Security Engine for detecting, categorizing, and quarantining
 * unauthorized IoT microcontrollers (Espressif, Tuya, Realtek, Raspberry Pi)
 * on private home and enterprise subnets under POTRAZ Chapter 12:07 guidelines.
 */
object IotChipsetSecurityEngine {

    // Comprehensive OUI Prefix Table for IoT & Embedded Hardware
    private val ESPRESSIF_PREFIXES = setOf(
        "74:AC:B9", "D8:3A:DD", "24:0A:C4", "30:AE:A4", "84:F3:EB", "A4:CF:12",
        "AC:67:B2", "EC:FA:BC", "60:01:94", "18:FE:34", "24:62:AB", "24:B2:DE",
        "34:94:54", "3C:61:05", "3C:71:BF", "40:22:D8", "40:91:51", "48:27:E2",
        "48:3F:DA", "48:55:19", "4C:11:AE", "4C:75:25", "50:02:91", "54:43:B2",
        "54:5A:A6", "5C:CF:7F", "68:C6:3A", "70:03:9F", "7C:DF:A1", "80:7D:3A",
        "84:0D:8E", "84:CC:A8", "8C:AA:B5", "90:97:D5", "94:B5:55", "94:B9:7E",
        "98:CD:AC", "A0:20:A6", "A0:B7:65", "A4:E5:7C", "AC:0B:FB", "B4:E6:2D",
        "BC:DD:C2", "C4:4F:33", "C4:DD:57", "CC:50:E3", "D4:D4:DA", "DC:4F:22",
        "E0:98:06", "E8:68:E7", "E8:9F:6D", "EC:62:60", "EC:64:C9", "F4:CF:A2"
    )

    private val TUYA_REALTEK_PREFIXES = setOf(
        "10:D5:61", "D4:A6:51", "70:89:76", "04:CF:8C", "20:F4:78", "50:02:91",
        "68:57:2D", "7C:25:DA", "A0:92:08", "C8:2E:47", "D0:27:06"
    )

    private val RASPBERRY_PI_AUDIT_PREFIXES = setOf(
        "E4:5F:01", "B8:27:EB", "DC:A6:32", "28:CD:C1"
    )

    /**
     * Inspects a MAC address and returns its IoT security profile.
     */
    fun profileMac(mac: String): IotChipsetProfile {
        val cleanMac = mac.trim().uppercase()
        val prefix = if (cleanMac.length >= 8) cleanMac.substring(0, 8) else cleanMac

        return when {
            ESPRESSIF_PREFIXES.contains(prefix) -> {
                IotChipsetProfile(
                    isIotChipset = true,
                    vendorName = "Espressif Systems",
                    chipsetFamily = "ESP8266 / ESP32 Microcontroller",
                    riskClassification = "HIGH_RISK_SHADOW_IOT",
                    isOffensiveAuditHardware = false,
                    potrazComplianceAdvisory = "POTRAZ Ch. 12:07: Unverified Espressif IoT Microcontroller detected on subnet. Potential unauthorized bridge or rogue hardware transmitter."
                )
            }
            TUYA_REALTEK_PREFIXES.contains(prefix) -> {
                IotChipsetProfile(
                    isIotChipset = true,
                    vendorName = "Tuya / Realtek IoT",
                    chipsetFamily = "Tuya Smart Home Controller",
                    riskClassification = "HIGH_RISK_SHADOW_IOT",
                    isOffensiveAuditHardware = false,
                    potrazComplianceAdvisory = "POTRAZ Ch. 12:07: Unverified Tuya/Realtek Smart IoT module on LAN. Subject to strict zero-tolerance isolation."
                )
            }
            RASPBERRY_PI_AUDIT_PREFIXES.contains(prefix) -> {
                IotChipsetProfile(
                    isIotChipset = true,
                    vendorName = "Raspberry Pi Trading Ltd",
                    chipsetFamily = "Single-Board Linux / Audit Platform",
                    riskClassification = "CRITICAL_OFFENSIVE_HOST",
                    isOffensiveAuditHardware = true,
                    potrazComplianceAdvisory = "POTRAZ Ch. 12:07 Section 163: Potential unauthorized network audit platform (Kali Linux / Pwnagotchi hardware) on subnet."
                )
            }
            else -> {
                IotChipsetProfile(
                    isIotChipset = false,
                    vendorName = "Standard Endpoint",
                    chipsetFamily = "Standard Host",
                    riskClassification = "STANDARD",
                    isOffensiveAuditHardware = false,
                    potrazComplianceAdvisory = "Nominal endpoint compliance."
                )
            }
        }
    }

    /**
     * Evaluates whether a discovered device violates the Strict Zero-Tolerance IoT Policy.
     */
    fun isRogueIotViolation(mac: String, isAuthorized: Boolean): Boolean {
        if (isAuthorized) return false
        val profile = profileMac(mac)
        return profile.isIotChipset
    }
}
