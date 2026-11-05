package com.opifex.wifisentinel.security

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppSecurityAnalyzerTest {

    private lateinit var context: Application
    private lateinit var analyzer: AppSecurityAnalyzer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        analyzer = AppSecurityAnalyzer(context)
    }

    @Test
    fun testEvaluateOwnPackageRunsCleanly() {
        val result = analyzer.evaluatePackage(context.packageName)
        assertNotNull("Scan result should not be null", result)
        assertEquals(context.packageName, result.packageName)
        assertTrue(result.compositeScore in 0..100)
        assertEquals("Verified own app package must be evaluated as SAFE without false positives",
            ThreatLevel.SAFE, result.threatLevel)
        assertTrue("Composite score for verified package must remain below 25",
            result.compositeScore < 25)
    }

    @Test
    fun testVerifiedSignerSuppressesFalsePositives() {
        val result = analyzer.evaluatePackage(context.packageName)
        // Ensure findings are either empty or flagged as compliance notices
        for (finding in result.findings) {
            if (finding.title.contains("POTRAZ Chapter 12:07 Notice") || finding.title.contains("Package Installer Capability")) {
                assertTrue("Finding should be marked as compliance flag", finding.isComplianceFlag)
                assertEquals("Verified origin findings must not carry high severity penalties", 0, finding.severityScore)
            }
        }
    }
}
