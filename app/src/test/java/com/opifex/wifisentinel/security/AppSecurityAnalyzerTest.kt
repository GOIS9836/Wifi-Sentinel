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
@Config(sdk = [34])
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
    }
}
