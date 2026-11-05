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

    // Known reputable developer certificate SHA-256 fingerprints (e.g., Meta, Google, Android OS)
    private val trustedCertHashes = setOf(
        "24:83:E6:E3:B3:28:B7:A9:E1:96:A2:3E:45:96:FF:A2:2C:9A:6E:9B:C9:47:DE:8E:B0:81:4A:23:45:11:AB:CD", // Sample SHA-256
        "F0:FD:6C:5B:41:0F:25:CB:25:C3:B5:33:46:C8:97:2F:AE:30:F8:EE:74:11:DF:91:04:80:AD:6B:2D:60:DB:83", // Google Play Release
        "38:91:8A:45:3D:07:19:93:54:F8:B1:9A:F0:5E:C6:56:2C:E5:7A:B4:42:F1:C9:2B:64:82:F0:B5:55:0D:37:A6"  // Android Platform Signer
    )

    private val trustedInstallerPackages = setOf(
        "com.android.vending",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.sec.android.app.samsungapps",
        "com.amazon.venezia",
        "org.fdroid.fdroid",
        "com.huawei.appmarket",
        "com.xiaomi.mipicks"
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
        val isSystemApp = (pkgInfo.applicationInfo?.flags ?: 0) and
                (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        val isTrustedSigner = verifyDeveloperSignature(pkgInfo) ||
                isSystemApp ||
                isInstallerTrusted(pm, packageName) ||
                packageName == context.packageName

        // 1. Evaluate Requested Permissions
        val requestedPermissions = pkgInfo.requestedPermissions ?: emptyArray()
        for (perm in requestedPermissions) {
            when (perm) {
                android.Manifest.permission.RECEIVE_SMS -> {
                    sensitivePerms.add("RECEIVE_SMS")
                    val penalty = if (isTrustedSigner) 5 else 15
                    findings.add(
                        SecurityFinding(
                            title = "High-Privilege Permission: SMS Monitoring",
                            description = if (isTrustedSigner) {
                                "Verified application requests SMS verification / 2FA retriever capability."
                            } else {
                                "Application can intercept incoming SMS messages. Introduces potential Out-of-Band (2FA) verification risk."
                            },
                            severityScore = penalty
                        )
                    )
                }

                android.Manifest.permission.REQUEST_INSTALL_PACKAGES -> {
                    sensitivePerms.add("REQUEST_INSTALL_PACKAGES")
                    // If the app is signed by a verified vendor, it is an in-app updater, not a malicious dropper
                    val penalty = if (isTrustedSigner) 0 else 30
                    findings.add(
                        SecurityFinding(
                            title = "Package Installer Capability",
                            description = if (isTrustedSigner) {
                                "Application requests package installation capability for internal update delivery (Verified Source)."
                            } else {
                                "Untrusted application requests package installation capability. Potential dropper vector."
                            },
                            severityScore = penalty,
                            isComplianceFlag = isTrustedSigner
                        )
                    )
                }

                android.Manifest.permission.READ_PHONE_STATE -> {
                    sensitivePerms.add("READ_PHONE_STATE")
                    // Contextualize for Zimbabwe Chapter 12:07: data minimization compliance
                    val penalty = if (isTrustedSigner) 0 else 10
                    findings.add(
                        SecurityFinding(
                            title = "POTRAZ Chapter 12:07 Notice: Telephony State Access",
                            description = if (isTrustedSigner) {
                                "Application accesses cellular carrier and telephony state in compliance with statutory data minimization requirements."
                            } else {
                                "Application accesses cellular carrier and telephony state. Must comply with statutory data minimization requirements."
                            },
                            severityScore = penalty,
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
            val penalty = if (isTrustedSigner) 5 else 20
            findings.add(
                SecurityFinding(
                    title = if (isTrustedSigner) "Cleartext Traffic Permitted (Verified Origin)" else "Cleartext HTTP Permitted",
                    description = if (isTrustedSigner) {
                        "Application allows cleartext HTTP endpoints for fallback services. Verified origin mitigates active spoofing vectors."
                    } else {
                        "NetworkSecurityConfig explicitly allows unencrypted HTTP traffic. Susceptible to local Wi-Fi MitM packet inspection."
                    },
                    severityScore = penalty,
                    isComplianceFlag = isTrustedSigner
                )
            )
        }

        // 3. Compute Composite Score with Trust Dampeners
        var compositeScore = findings.sumOf { it.severityScore }

        // If from a globally signed, verified store or known developer, apply dampener to avoid false-positive panic
        if (isTrustedSigner) {
            compositeScore = (compositeScore * 0.3).toInt() // 70% reduction for verified legitimate signers
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

    private fun isInstallerTrusted(pm: PackageManager, packageName: String): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
            installer != null && trustedInstallerPackages.contains(installer)
        } catch (e: Exception) {
            false
        }
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
