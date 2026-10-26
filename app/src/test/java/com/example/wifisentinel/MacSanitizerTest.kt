package com.example.wifisentinel

import com.example.wifisentinel.security.MacSanitizer
import org.junit.Assert.*
import org.junit.Test

class MacSanitizerTest {

    @Test
    fun detectsRandomizedMacAddressesCorrectly() {
        // Locally administered / randomized MACs (2nd char: 2, 6, A, E)
        assertTrue(MacSanitizer.isRandomizedMac("da:a1:19:00:11:22"))
        assertTrue(MacSanitizer.isRandomizedMac("06:12:34:56:78:9a"))
        assertTrue(MacSanitizer.isRandomizedMac("3a:ff:fe:12:34:56"))

        // Globally unique / hardware burned-in MACs
        assertFalse(MacSanitizer.isRandomizedMac("00:1A:2B:3C:4D:5E"))
        assertFalse(MacSanitizer.isRandomizedMac("b4:2e:99:ab:cd:ef"))
    }

    @Test
    fun testIdentifierHashingProducesDeterministicOutput() {
        val hash1 = MacSanitizer.hashIdentifier("192.168.1.100", "local_salt_123")
        val hash2 = MacSanitizer.hashIdentifier("192.168.1.100", "local_salt_123")
        val diffHash = MacSanitizer.hashIdentifier("192.168.1.101", "local_salt_123")

        assertEquals(hash1, hash2)
        assertNotEquals(hash1, diffHash)
    }

    @Test
    fun maskMacAddress_preservesVendorPrefixAndObfuscatesMidOctets() {
        val maskedColon = MacSanitizer.maskMacAddress("AA:BB:CC:DD:EE:FF")
        assertEquals("AA:BB:**:**:**:FF", maskedColon)
        val maskedHyphen = MacSanitizer.maskMacAddress("11-22-33-44-55-66")
        assertEquals("11:22:**:**:**:66", maskedHyphen)
    }

    @Test
    fun malformedInputsAndEdgeCases_gracefullyHandledWithoutException() {
        assertFalse(MacSanitizer.isRandomizedMac(""))
        assertFalse(MacSanitizer.isRandomizedMac("X"))
        assertFalse(MacSanitizer.isRandomizedMac("INVALID:MAC:ADDR"))
        assertTrue(MacSanitizer.isPlaceholderMac("--"))
        assertTrue(MacSanitizer.isPlaceholderMac("02:00:00:00:00:00"))
        assertTrue(MacSanitizer.isPlaceholderMac("00:00:00:00:00:00"))
        assertFalse(MacSanitizer.isPlaceholderMac("00:1A:2B:3C:4D:5E"))
        assertEquals("SHORT", MacSanitizer.maskMacAddress("SHORT"))
        assertEquals("INVALID:MAC:LEN:EXTRA:BITS:HERE:NOW", MacSanitizer.maskMacAddress("INVALID:MAC:LEN:EXTRA:BITS:HERE:NOW"))
    }

    @Test
    fun maskMsisdn_redactsZimbabweanPhoneNumbersUnderChapter1207() {
        val maskedIntl = MacSanitizer.maskMsisdn("+263771234567")
        assertEquals("+263 77 *** 4567", maskedIntl)

        val maskedDomestic = MacSanitizer.maskMsisdn("0771234567")
        assertEquals("077 *** 4567", maskedDomestic)

        val maskedNoPlus = MacSanitizer.maskMsisdn("263771234567")
        assertEquals("+263 77 *** 4567", maskedNoPlus)
    }

    @Test
    fun maskIccidAndImsi_shieldsTelecomHardwareIdentities() {
        val iccid = "8926301234567890123"
        val maskedIccid = MacSanitizer.maskIccid(iccid)
        assertEquals("89263-XXXX-XXXX-90123", maskedIccid)

        val imsi = "648011234567890"
        val maskedImsi = MacSanitizer.maskImsi(imsi)
        assertEquals("648-XX-XXXX-67890", maskedImsi)
    }

    @Test
    fun sanitizeTelecomPii_replacesEmbeddedPiiInRawStrings() {
        val raw = "Subscriber with MSISDN +263771234567 and ICCID 8926301234567890123 connected."
        val sanitized = MacSanitizer.sanitizeTelecomPii(raw)
        assertFalse(sanitized.contains("+263771234567"))
        assertFalse(sanitized.contains("8926301234567890123"))
        assertTrue(sanitized.contains("+263 77 *** 4567"))
        assertTrue(sanitized.contains("89263-XXXX-XXXX-90123"))
    }

    @Test
    fun isPotrazRestrictedTarget_flagsMaliciousTldsAndKeywords() {
        assertTrue(MacSanitizer.isPotrazRestrictedTarget("http://malicious-node.xyz/api"))
        assertTrue(MacSanitizer.isPotrazRestrictedTarget("https://dark-relay.net/c2"))
        assertTrue(MacSanitizer.isPotrazRestrictedTarget("https://fast-cash-zim.org/invest"))
        assertFalse(MacSanitizer.isPotrazRestrictedTarget("https://potraz.gov.zw"))
        assertFalse(MacSanitizer.isPotrazRestrictedTarget("https://google.com"))
    }
}
