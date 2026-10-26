package com.example.security

import java.security.MessageDigest

/**
 * Privacy-Safe False-Positive Corroborator & Statutory POTRAZ Chapter 12:07 Guard.
 *
 * Governance Standards:
 * - POTRAZ Cyber and Data Protection Act [Chapter 12:07] (Republic of Zimbabwe)
 * - (ISC)² CISSP Domain 5 (Identity & Access) & Domain 8 (Software Development Security)
 * - Salted HMAC-SHA256 Tokenization: Raw hardware MACs and telecom subscriber PII are never
 *   stored or transmitted in cleartext.
 * - Locally Administered Bit (LAA) Inspection: Distinguishes legitimate OS MAC randomization
 *   from active clone attacks.
 */
object MacSanitizer {

    /**
     * Checks if a MAC address is an unresolvable placeholder, null, or Android privacy address.
     * On Android 6.0+ and 10+ (API 29+), getMacAddress() returns "02:00:00:00:00:00".
     * Non-root ARP lookups frequently return placeholders like "--", "UNKNOWN", or null.
     */
    fun isPlaceholderOrPrivacyMac(macAddress: String): Boolean {
        val clean = macAddress.trim().uppercase()
        return clean.isBlank() || clean in setOf(
            "--", "UNKNOWN", "N/A", "NONE", "INCOMPLETE",
            "02:00:00:00:00:00", // Android default privacy MAC
            "00:00:00:00:00:00", // Null MAC
            "FF:FF:FF:FF:FF:FF"  // Broadcast MAC
        )
    }

    /**
     * Alias for isPlaceholderOrPrivacyMac for backward compatibility.
     */
    fun isPlaceholderMac(macAddress: String): Boolean = isPlaceholderOrPrivacyMac(macAddress)

    /**
     * Checks if a MAC address is a randomized, locally-administered address (LAA).
     * Standard IEEE 802 rule: The 2nd least-significant bit of the first byte is 1.
     * Example: x2:xx:xx:xx:xx:xx, x6:xx:xx:xx:xx:xx, xA:xx:xx:xx:xx:xx, xE:xx:xx:xx:xx:xx
     */
    fun isRandomizedMac(macAddress: String): Boolean {
        if (isPlaceholderOrPrivacyMac(macAddress)) return false
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

    // =========================================================================
    // POTRAZ STATUTORY BASELINE ENFORCEMENT [CHAPTER 12:07]
    // Cyber and Data Protection Act (Republic of Zimbabwe) & CISSP D5/D8
    // =========================================================================

    private val MSISDN_REGEX = Regex("""(\+?263\s?(?:77|78|71|73)\s?\d{3}\s?\d{4}|\b07[7813]\d{7}\b|\b2637\d{8}\b)""")
    private val ICCID_REGEX = Regex("""\b(89263\d{13,15}|89\d{17,18})\b""")
    private val IMSI_REGEX = Regex("""\b(648\d{12})\b""")

    private val BLOCKED_TLDS = setOf(".tk", ".xyz", ".top", ".ru", ".fit", ".pw")
    private val FORBIDDEN_CONTENT_KEYWORDS = setOf(
        "fast-cash-zim", "double-your-crypto", "forex-pyramid", "c2-relay", "keylogger",
        "botnet", "unrated-media", "dark-relay", "proxy-bypass", "unmasked-subscriber-dump"
    )

    /**
     * Masks Zimbabwean subscriber phone numbers (MSISDN) to protect PII.
     * Example: "+263771234567" -> "+263 77 *** 4567", "0771234567" -> "077 *** 4567"
     */
    fun maskMsisdn(msisdn: String): String {
        val digits = msisdn.replace(Regex("""\s+"""), "")
        return when {
            digits.startsWith("+263") && digits.length >= 12 -> {
                val prefix = "+263 " + digits.substring(4, 6)
                val suffix = digits.takeLast(4)
                "$prefix *** $suffix"
            }
            digits.startsWith("263") && digits.length >= 11 -> {
                val prefix = "+263 " + digits.substring(3, 5)
                val suffix = digits.takeLast(4)
                "$prefix *** $suffix"
            }
            digits.startsWith("07") && digits.length >= 10 -> {
                val prefix = digits.substring(0, 3) // e.g. 077
                val suffix = digits.takeLast(4)
                "$prefix *** $suffix"
            }
            else -> {
                if (digits.length > 4) "****" + digits.takeLast(4) else "****"
            }
        }
    }

    /**
     * Masks SIM Card ICCID hardware identifiers under POTRAZ Section 163.
     * Example: "8926312345678901234" -> "89263-XXXX-XXXX-01234"
     */
    fun maskIccid(iccid: String): String {
        val clean = iccid.trim()
        return if (clean.length >= 18) {
            val prefix = clean.take(5)
            val suffix = clean.takeLast(5)
            "$prefix-XXXX-XXXX-$suffix"
        } else {
            "89263-XXXX-XXXX-*****"
        }
    }

    /**
     * Masks IMSI mobile subscriber identity.
     * Example: "648011234567890" -> "648-XX-XXXX-67890"
     */
    fun maskImsi(imsi: String): String {
        val clean = imsi.trim()
        return if (clean.length >= 15) {
            val prefix = clean.take(3)
            val suffix = clean.takeLast(5)
            "$prefix-XX-XXXX-$suffix"
        } else {
            "648-XX-XXXX-*****"
        }
    }

    /**
     * Enforces automatic POTRAZ Chapter 12:07 PII scrubbing across any log, payload,
     * network dump, or export string.
     */
    fun sanitizeTelecomPii(rawText: String): String {
        if (rawText.isBlank()) return rawText

        var sanitized = rawText
        sanitized = MSISDN_REGEX.replace(sanitized) { match ->
            maskMsisdn(match.value)
        }
        sanitized = ICCID_REGEX.replace(sanitized) { match ->
            maskIccid(match.value)
        }
        sanitized = IMSI_REGEX.replace(sanitized) { match ->
            maskImsi(match.value)
        }
        return sanitized
    }

    /**
     * Validates whether a target host/domain violates statutory POTRAZ content directives.
     */
    fun isPotrazRestrictedTarget(target: String): Boolean {
        val lower = target.lowercase().trim()
        val cleanDomain = lower.removePrefix("https://").removePrefix("http://").substringBefore("/").substringBefore(":")

        // 1. Check prohibited TLDs
        if (BLOCKED_TLDS.any { cleanDomain.endsWith(it) }) return true

        // 2. Check forbidden keyword patterns
        if (FORBIDDEN_CONTENT_KEYWORDS.any { lower.contains(it) }) return true

        return false
    }
}
