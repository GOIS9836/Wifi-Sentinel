package com.example.wifisentinel

import com.example.service.IotChipsetSecurityEngine
import org.junit.Assert.*
import org.junit.Test

class IotChipsetSecurityEngineTest {

    @Test
    fun profileMac_detectsEspressifChipsets() {
        val profile1 = IotChipsetSecurityEngine.profileMac("74:AC:B9:11:22:33")
        assertTrue(profile1.isIotChipset)
        assertEquals("Espressif Systems", profile1.vendorName)
        assertEquals("HIGH_RISK_SHADOW_IOT", profile1.riskClassification)
        assertFalse(profile1.isOffensiveAuditHardware)
        assertTrue(profile1.potrazComplianceAdvisory.contains("POTRAZ Ch. 12:07"))

        val profile2 = IotChipsetSecurityEngine.profileMac("d8:3a:dd:44:55:66")
        assertTrue(profile2.isIotChipset)
        assertEquals("Espressif Systems", profile2.vendorName)
    }

    @Test
    fun profileMac_detectsTuyaRealtek() {
        val profile = IotChipsetSecurityEngine.profileMac("10:D5:61:AA:BB:CC")
        assertTrue(profile.isIotChipset)
        assertEquals("Tuya / Realtek IoT", profile.vendorName)
        assertEquals("HIGH_RISK_SHADOW_IOT", profile.riskClassification)
    }

    @Test
    fun profileMac_detectsRaspberryPiAuditHardware() {
        val profile = IotChipsetSecurityEngine.profileMac("E4:5F:01:12:34:56")
        assertTrue(profile.isIotChipset)
        assertTrue(profile.isOffensiveAuditHardware)
        assertEquals("CRITICAL_OFFENSIVE_HOST", profile.riskClassification)
        assertEquals("Raspberry Pi Trading Ltd", profile.vendorName)
        assertTrue(profile.potrazComplianceAdvisory.contains("Section 163"))
    }

    @Test
    fun isRogueIotViolation_strictlyEnforced() {
        val rogueEspressif = "74:AC:B9:AA:BB:CC"
        // Unauthorized -> Violation
        assertTrue(IotChipsetSecurityEngine.isRogueIotViolation(rogueEspressif, isAuthorized = false))
        // Authorized -> Not a violation
        assertFalse(IotChipsetSecurityEngine.isRogueIotViolation(rogueEspressif, isAuthorized = true))

        // Standard device unauthorized is not an IoT chipset violation
        val standardDevice = "00:1A:2B:CC:DD:EE"
        assertFalse(IotChipsetSecurityEngine.isRogueIotViolation(standardDevice, isAuthorized = false))
    }

    @Test
    fun standardEndpoint_returnsNominalProfile() {
        val profile = IotChipsetSecurityEngine.profileMac("00:1A:2B:99:88:77")
        assertFalse(profile.isIotChipset)
        assertFalse(profile.isOffensiveAuditHardware)
        assertEquals("STANDARD", profile.riskClassification)
    }
}
