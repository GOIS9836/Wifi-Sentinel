package com.opifex.wifisentinel.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

data class SecurityFinding(
    val title: String,
    val description: String,
    val severityScore: Int,
    val isComplianceFlag: Boolean = false
)

data class ScanResult(
    val packageName: String,
    val appName: String,
    val threatLevel: ThreatLevel,
    val compositeScore: Int,
    val findings: List<SecurityFinding>,
    val sensitivePermissionsGranted: List<String>
)

enum class ThreatLevel {
    SAFE,
    ELEVATED,
    HIGH,
    CRITICAL
}

class AppSecurityAnalyzer(private val context: Context) {

    // Known reputable developer certificate SHA-256 fingerprints (e.g., Meta, Google)
    private val trustedCertHashes = setOf(
        "24:83:E6:E3:B3:28:B7:A9:E1:96:A2:3E:45:96:FF:A2:2C:9A:6E:9B:C9:47:DE:8E:B0:81:4A:23:45:11:AB:CD" // Sample SHA-256
    )

    fun evaluatePackage(packageName: String): ScanResult {
        val pm = context.packageManager
        val findings = mutableListOf<SecurityFinding>()
        val sensitivePerms = mutableListOf<String>()

        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(
                    (PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES).toLong()
                )
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES
            )
        }

        val appName = pkgInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageName
        val isSystemApp = (pkgInfo.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
        val isTrustedSigner = verifyDeveloperSignature(pkgInfo)

        // 1. Evaluate Requested Permissions
        val requestedPermissions = pkgInfo.requestedPermissions ?: emptyArray()
        for (perm in requestedPermissions) {
            when (perm) {
                android.Manifest.permission.RECEIVE_SMS -> {
                    sensitivePerms.add("RECEIVE_SMS")
                    findings.add(
                        SecurityFinding(
                            title = "High-Privilege Permission: SMS Monitoring",
                            description = "Application can intercept incoming SMS messages. Introduces potential Out-of-Band (2FA) verification risk.",
                            severityScore = 15
                        )
                    )
                }

                android.Manifest.permission.REQUEST_INSTALL_PACKAGES -> {
                    sensitivePerms.add("REQUEST_INSTALL_PACKAGES")
                    // If the app is signed by a verified vendor, it is usually an in-app updater, not a malicious dropper
                    val penalty = if (isTrustedSigner) 5 else 30
                    findings.add(
                        SecurityFinding(
                            title = "Package Installer Capability",
                            description = if (isTrustedSigner) {
                                "Application requests package installation capability for internal update delivery."
                            } else {
                                "Untrusted application requests package installation capability. Potential dropper vector."
                            },
                            severityScore = penalty
                        )
                    )
                }

                android.Manifest.permission.READ_PHONE_STATE -> {
                    sensitivePerms.add("READ_PHONE_STATE")
                    // Contextualize for Zimbabwe Chapter 12:07
                    findings.add(
                        SecurityFinding(
                            title = "POTRAZ Chapter 12:07 Notice: Telephony State Access",
                            description = "Application accesses cellular carrier and telephony state. Must comply with statutory data minimization requirements.",
                            severityScore = 10,
                            isComplianceFlag = true
                        )
                    )
                }

                android.Manifest.permission.CAMERA -> sensitivePerms.add("CAMERA")
                android.Manifest.permission.RECORD_AUDIO -> sensitivePerms.add("RECORD_AUDIO")
            }
        }

        // 2. Evaluate Cleartext Traffic Policy (Inspect Manifest Flag, Not Just Hardcoded Strings)
        val usesCleartext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pkgInfo.applicationInfo?.let {
                (it.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
            } ?: false
        } else {
            true
        }

        if (usesCleartext && !isSystemApp) {
            findings.add(
                SecurityFinding(
                    title = "Cleartext HTTP Permitted",
                    description = "NetworkSecurityConfig explicitly allows unencrypted HTTP traffic. Susceptible to local Wi-Fi MitM packet inspection.",
                    severityScore = 20
                )
            )
        }

        // 3. Compute Composite Score with Trust Dampeners
        var compositeScore = findings.sumOf { it.severityScore }

        // If from a globally signed, verified store or known developer, apply dampener to avoid false-positive panic
        if (isTrustedSigner) {
            compositeScore = (compositeScore * 0.4).toInt() // 60% reduction for verified legitimate signers
        }

        compositeScore = compositeScore.coerceIn(0, 100)

        val threatLevel = when {
            compositeScore >= 80 -> ThreatLevel.CRITICAL
            compositeScore >= 50 -> ThreatLevel.HIGH
            compositeScore >= 25 -> ThreatLevel.ELEVATED
            else -> ThreatLevel.SAFE
        }

        return ScanResult(
            packageName = packageName,
            appName = appName,
            threatLevel = threatLevel,
            compositeScore = compositeScore,
            findings = findings,
            sensitivePermissionsGranted = sensitivePerms
        )
    }

    private fun verifyDeveloperSignature(pkgInfo: PackageInfo): Boolean {
        try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return false

            val md = MessageDigest.getInstance("SHA-256")
            val certDigest = md.digest(signatures[0].toByteArray())
            val hexString = certDigest.joinToString(":") { String.format("%02X", it) }

            return trustedCertHashes.contains(hexString)
        } catch (e: Exception) {
            return false
        }
    }
}
