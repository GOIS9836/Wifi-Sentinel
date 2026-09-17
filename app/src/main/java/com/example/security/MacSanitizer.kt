package com.example.security

import java.security.MessageDigest

/**
 * Privacy-Safe False-Positive Corroborator.
 * Under GDPR/CCPA, storing raw MAC addresses is legally risky. This utility handles:
 * - One-Way Hashing: Identifiers are pseudonymized via SHA-256 with a local salt before storage.
 * - Locally Administered Bit Inspection: Detects randomized MAC addresses (used by modern smartphones for privacy)
 *   to avoid flagging legitimate personal devices as malicious intruders.
 */
object MacSanitizer {

    /**
     * Checks if a MAC address is a randomized, locally-administered address (LAA).
     * Standard IEEE 802 rule: The 2nd least-significant bit of the first byte is 1.
     * Example: x2:xx:xx:xx:xx:xx, x6:xx:xx:xx:xx:xx, xA:xx:xx:xx:xx:xx, xE:xx:xx:xx:xx:xx
     */
    fun isRandomizedMac(macAddress: String): Boolean {
        val cleanMac = macAddress.replace(":", "").replace("-", "").trim()
        if (cleanMac.length < 2) return false

        return try {
            val firstByte = cleanMac.substring(0, 2).toInt(16)
            (firstByte and 0x02) != 0 // Bit 1 indicates Locally Administered Address
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Privacy-Compliant Storage: Hashes identifiers so raw PII is never persisted.
     */
    fun hashIdentifier(identifier: String, localSalt: String = "WiFiSentinel-Salt-V1"): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$identifier:$localSalt".toByteArray(Charsets.UTF_8)
        val hashBytes = digest.digest(combined)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates an obfuscated display MAC for UI rendering (e.g., "AA:BB:xx:xx:xx:FF")
     * preserving vendor prefix while protecting end-user privacy.
     */
    fun maskMacAddress(macAddress: String): String {
        val parts = macAddress.split(":", "-")
        if (parts.size != 6) return macAddress
        return "${parts[0]}:${parts[1]}:**:**:**:${parts[5]}"
    }
}
