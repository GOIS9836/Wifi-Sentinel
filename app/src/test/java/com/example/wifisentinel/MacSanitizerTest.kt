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
}
