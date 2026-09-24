package com.example.service

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.example.data.model.AppRiskLevel
import com.example.data.model.AppSecurityScanResult
import com.example.data.model.SystemSecurityAudit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

object AntivirusScannerEngine {

    private val DANGEROUS_PERMISSIONS = setOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.BIND_DEVICE_ADMIN",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.SEND_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.WRITE_SETTINGS",
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_PRIVILEGED_PHONE_STATE",
        "android.permission.ACCESS_BACKGROUND_LOCATION"
    )

    private val HIGH_RISK_PERMISSIONS_COMBINATIONS = listOf(
        setOf("android.permission.BIND_ACCESSIBILITY_SERVICE", "android.permission.SYSTEM_ALERT_WINDOW"),
        setOf("android.permission.REQUEST_INSTALL_PACKAGES", "android.permission.SYSTEM_ALERT_WINDOW"),
        setOf("android.permission.READ_SMS", "android.permission.RECEIVE_SMS", "android.permission.INTERNET")
    )

    fun scanAllApps(context: Context): Flow<ScanProgressEvent> = flow {
        val pm = context.packageManager
        val packages = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (e: Exception) {
            emptyList()
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val activeAdmins = try {
            dpm?.activeAdmins?.map { it.packageName }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }

        val total = packages.size
        emit(ScanProgressEvent.Started(total))

        val results = mutableListOf<AppSecurityScanResult>()

        packages.forEachIndexed { index, pkg ->
            val appName = try {
                pm.getApplicationLabel(pkg.applicationInfo ?: return@forEachIndexed).toString()
            } catch (e: Exception) {
                pkg.packageName
            }

            emit(ScanProgressEvent.Progress(
                currentIndex = index + 1,
                totalCount = total,
                currentAppName = appName,
                packageName = pkg.packageName
            ))

            val scanResult = analyzePackage(pm, pkg, activeAdmins)
            results.add(scanResult)

            // Small delay for smooth visual scanning feedback in UI
            if (total < 60) {
                kotlinx.coroutines.delay(18)
            } else {
                if (index % 5 == 0) kotlinx.coroutines.delay(5)
            }
        }

        // Sort: Critical & High Risk first, then Suspicious, then Safe
        val sortedResults = results.sortedWith(
            compareByDescending<AppSecurityScanResult> { it.riskLevel.ordinal }
                .thenByDescending { it.riskScore }
                .thenBy { it.appName.lowercase() }
        )

        emit(ScanProgressEvent.Completed(sortedResults))
    }.flowOn(Dispatchers.Default)

    /**
     * Synchronously scans all installed packages for background/scheduled execution without artificial UI delays.
     */
    fun scanAllAppsSync(context: Context): List<AppSecurityScanResult> {
        val pm = context.packageManager
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val activeAdmins = try {
            dpm?.activeAdmins?.map { it.packageName }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }

        val packages = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (e: Exception) {
            emptyList()
        }

        val results = packages.mapNotNull { pkg ->
            if (pkg.applicationInfo != null) {
                analyzePackage(pm, pkg, activeAdmins)
            } else null
        }

        return results.sortedWith(
            compareByDescending<AppSecurityScanResult> { it.riskLevel.ordinal }
                .thenByDescending { it.riskScore }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun analyzePackage(pm: PackageManager, pkg: PackageInfo, activeAdmins: Set<String> = emptySet()): AppSecurityScanResult {
        val appInfo = pkg.applicationInfo
        val isSystem = if (appInfo != null) {
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } else false

        val appName = try {
            if (appInfo != null) pm.getApplicationLabel(appInfo).toString() else pkg.packageName
        } catch (e: Exception) {
            pkg.packageName
        }

        // Determine installer source
        val installerSource = try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(pkg.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(pkg.packageName)
            }
            when {
                installer == "com.android.vending" -> "Google Play"
                installer == "com.google.android.packageinstaller" -> "Package Installer"
                installer.isNullOrEmpty() -> if (isSystem) "System Image" else "Sideloaded / APK"
                else -> installer
            }
        } catch (e: Exception) {
            if (isSystem) "System" else "Unknown Source"
        }

        val dexHeuristics = inspectDexHeuristics(appInfo?.sourceDir)
        val hasCleartextFlag = if (appInfo != null) {
            (appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
        } else false
        val finalCleartext = dexHeuristics.hasCleartextTraffic || hasCleartextFlag

        val requestedPermissions = pkg.requestedPermissions ?: emptyArray()
        val dangerousFound = requestedPermissions.filter { it in DANGEROUS_PERMISSIONS }

        val reasons = mutableListOf<String>()
        var riskScore = 0

        // Heuristics:
        // 1. Accessibility + Overlay combination (frequent banking trojan / RAT tactic)
        val hasAccessibility = "android.permission.BIND_ACCESSIBILITY_SERVICE" in requestedPermissions
        val hasOverlay = "android.permission.SYSTEM_ALERT_WINDOW" in requestedPermissions
        val hasInstallPackages = "android.permission.REQUEST_INSTALL_PACKAGES" in requestedPermissions

        if (!isSystem) {
            if (hasAccessibility && hasOverlay) {
                riskScore += 55
                reasons.add("Requests both Accessibility and Screen Overlay (Trojan / Screen hijacking vector)")
            } else if (hasAccessibility) {
                riskScore += 35
                reasons.add("Requests Accessibility Service (can monitor user taps and text inputs)")
            } else if (hasOverlay) {
                riskScore += 20
                reasons.add("Requests Draw Over Other Apps (can display deceptive overlay windows)")
            }

            if (hasInstallPackages) {
                riskScore += 25
                reasons.add("Can silently request installation of unknown APK packages (Dropper vector)")
            }

            if ("android.permission.BIND_DEVICE_ADMIN" in requestedPermissions) {
                if (pkg.packageName in activeAdmins) {
                    riskScore += 45
                    reasons.add("Active Device Administrator: App holds granted device management privileges (Uninstall lockout & persistence risk)")
                } else {
                    riskScore += 30
                    reasons.add("Requests Device Administrator privileges (can prevent uninstallation)")
                }
            }

            val smsPermissions = listOf("android.permission.READ_SMS", "android.permission.RECEIVE_SMS", "android.permission.SEND_SMS")
            val hasSms = smsPermissions.any { it in requestedPermissions }
            if (hasSms) {
                riskScore += 20
                reasons.add("Can access or intercept SMS text messages (2FA interception risk)")
            }

            // 7. Dynamic Code Loading (DCL)
            if (dexHeuristics.hasDynamicCodeLoading) {
                riskScore += 40
                reasons.add("Bytecode Alert: DEX Dynamic Code Loading Active (Evades static scanning / downloads runtime classes)")
            }

            // 8. POTRAZ Chapter 12:07 Hardware / Telephony Identifier Harvesting
            val hasPhoneState = "android.permission.READ_PHONE_STATE" in requestedPermissions ||
                    "android.permission.READ_PRIVILEGED_PHONE_STATE" in requestedPermissions
            if (hasPhoneState || dexHeuristics.hasPotrazIdentifierHarvesting) {
                riskScore += 25
                reasons.add("POTRAZ Chapter 12:07 Breach: Hardware/Subscriber Identifier Harvester (IMEI / Device ID / Telephony access)")
            }

            // 9. Cleartext HTTP Network Exfiltration
            if (finalCleartext) {
                riskScore += 20
                reasons.add("Network Security Violation: Cleartext HTTP endpoints detected (MiTM and wire sniffing exposure on Wi-Fi)")
            }

            // 10. Multi-vector Tapjacking & Invasive Surveillance
            val hasCamera = "android.permission.CAMERA" in requestedPermissions
            val hasAudio = "android.permission.RECORD_AUDIO" in requestedPermissions
            val hasWriteSettings = "android.permission.WRITE_SETTINGS" in requestedPermissions
            if (hasOverlay && (hasCamera || hasAudio) && hasWriteSettings) {
                riskScore += 35
                reasons.add("Multi-vector Tapjacking & Surveillance: SYSTEM_ALERT_WINDOW combined with Camera/Audio and WRITE_SETTINGS")
            }

            if (installerSource.contains("Sideloaded") || installerSource.contains("Unknown")) {
                riskScore += 15
                reasons.add("Installed from untrusted or non-official repository (sideloaded APK)")
            }

            if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                riskScore += 15
                reasons.add("Application built with debuggable flag enabled (increased vulnerability)")
            }
        } else {
            // System apps are generally trusted, but we note privileged rights
            if (hasAccessibility || hasOverlay) {
                riskScore = 10
            }
        }

        if (riskScore >= 100) {
            reasons.add("Extreme Threat Threshold Exceeded: Composite score exceeds 100/100 (Quarantine Recommended)")
        }

        val riskLevel = when {
            riskScore >= 60 -> AppRiskLevel.CRITICAL
            riskScore >= 40 -> AppRiskLevel.HIGH_RISK
            riskScore >= 20 -> AppRiskLevel.SUSPICIOUS
            else -> AppRiskLevel.SAFE
        }

        val appSize = try {
            if (appInfo != null && appInfo.sourceDir != null) {
                File(appInfo.sourceDir).length()
            } else 0L
        } catch (e: Exception) {
            0L
        }

        return AppSecurityScanResult(
            packageName = pkg.packageName,
            appName = appName,
            versionName = pkg.versionName ?: "1.0",
            isSystemApp = isSystem,
            installerSource = installerSource,
            riskLevel = riskLevel,
            riskScore = riskScore,
            riskReasons = reasons,
            dangerousPermissions = dangerousFound,
            isQuarantined = false,
            isWhitelisted = false,
            appSizeBytes = appSize,
            targetSdkVersion = appInfo?.targetSdkVersion ?: 34,
            hasDynamicCodeLoading = dexHeuristics.hasDynamicCodeLoading,
            hasCleartextTraffic = finalCleartext,
            hasPotrazIdentifierHarvesting = dexHeuristics.hasPotrazIdentifierHarvesting || ("android.permission.READ_PHONE_STATE" in requestedPermissions)
        )
    }

    data class DexHeuristicResult(
        val hasDynamicCodeLoading: Boolean = false,
        val hasCleartextTraffic: Boolean = false,
        val hasPotrazIdentifierHarvesting: Boolean = false
    )

    fun inspectDexHeuristics(sourceDir: String?): DexHeuristicResult {
        if (sourceDir.isNullOrEmpty()) return DexHeuristicResult()
        val apkFile = File(sourceDir)
        if (!apkFile.exists() || !apkFile.canRead()) return DexHeuristicResult()

        var dclFound = false
        var cleartextTraffic = false
        var potrazHarvesting = false

        try {
            java.util.zip.ZipFile(apkFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".dex") && !entry.isDirectory) {
                        zip.getInputStream(entry).use { stream ->
                            val buffer = ByteArray(65536)
                            var prevTail = ""
                            var bytesRead: Int
                            while (stream.read(buffer).also { bytesRead = it } != -1) {
                                val chunk = prevTail + String(buffer, 0, bytesRead, Charsets.ISO_8859_1).lowercase()
                                if (!dclFound && (chunk.contains("dexclassloader") || chunk.contains("inmemorydexclassloader"))) {
                                    dclFound = true
                                }
                                if (!potrazHarvesting && (chunk.contains("getdeviceid") || chunk.contains("getimei") || chunk.contains("getsubscriberid") || chunk.contains("getsimserialnumber"))) {
                                    potrazHarvesting = true
                                }
                                if (!cleartextTraffic && chunk.contains("http://") && !chunk.contains("http://schemas.android.com")) {
                                    cleartextTraffic = true
                                }
                                prevTail = if (chunk.length > 256) chunk.substring(chunk.length - 256) else chunk
                                if (dclFound && potrazHarvesting && cleartextTraffic) break
                            }
                        }
                    }
                    if (dclFound && potrazHarvesting && cleartextTraffic) break
                }
            }
        } catch (e: Exception) {
            // Graceful fallback for protected/inaccessible paths
        }

        return DexHeuristicResult(
            hasDynamicCodeLoading = dclFound,
            hasCleartextTraffic = cleartextTraffic,
            hasPotrazIdentifierHarvesting = potrazHarvesting
        )
    }

    fun auditSystemSecurity(context: Context): SystemSecurityAudit {
        val vulnerabilities = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        var securityScore = 100

        // 1. Root check
        val isRooted = checkRootBinaries()
        if (isRooted) {
            securityScore -= 40
            vulnerabilities.add("Device Root Binaries Detected ('su' binary accessible)")
            recommendations.add("Unroot device to restore Android sandbox and SELinux isolation.")
        }

        // 2. USB Debugging / ADB
        val isAdb = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
        if (isAdb) {
            securityScore -= 15
            vulnerabilities.add("USB Debugging (ADB) is actively enabled")
            recommendations.add("Disable USB Debugging in Developer Options when not testing.")
        }

        // 3. Developer Options
        val isDev = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
        if (isDev && !isAdb) {
            securityScore -= 5
            vulnerabilities.add("Android Developer Options enabled")
            recommendations.add("Turn off Developer Options to lock system debugging flags.")
        }

        // 4. Lock Screen Protection
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isSecureLock = keyguardManager?.isDeviceSecure ?: true
        if (!isSecureLock) {
            securityScore -= 20
            vulnerabilities.add("Device lock screen is unsecured (No PIN/Password/Biometrics)")
            recommendations.add("Set up a strong PIN, password, or biometric lock to safeguard device encryption keys.")
        }

        // 5. Unknown Sources
        val isUnknownSources = try {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
        } catch (e: Exception) {
            false
        }
        if (isUnknownSources) {
            securityScore -= 15
            vulnerabilities.add("Installation from Unknown Sources is permitted")
            recommendations.add("Disable install from unknown sources to block drive-by malware payloads.")
        }

        // 6. Mobile Admin Apps Protection (Zero-Trust Active Device Admin Audit)
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val activeAdmins = try {
            dpm?.activeAdmins?.map { it.packageName } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        // Whitelisted platform/security device administrator packages
        val trustedAdminPackages = setOf(
            "com.google.android.apps.adm", // Google Find My Device
            "com.google.android.gms",
            context.packageName
        )

        val unauthorizedAdmins = activeAdmins.filter { it !in trustedAdminPackages }
        if (unauthorizedAdmins.isNotEmpty()) {
            securityScore -= 25
            vulnerabilities.add("Active Device Administrator privileges held by ${unauthorizedAdmins.size} unauthorized app(s): ${unauthorizedAdmins.joinToString(", ")}")
            recommendations.add("Revoke unverified Device Administrator access in Android Security Settings to prevent persistence and uninstall lockout.")
        }

        return SystemSecurityAudit(
            isRootDetected = isRooted,
            isUsbDebuggingEnabled = isAdb,
            isDeveloperOptionsEnabled = isDev,
            isLockScreenSecure = isSecureLock,
            isUnknownSourcesEnabled = isUnknownSources,
            activeDeviceAdminsCount = activeAdmins.size,
            unauthorizedDeviceAdmins = unauthorizedAdmins,
            overallSecurityScore = securityScore.coerceIn(10, 100),
            activeIssuesCount = vulnerabilities.size,
            vulnerabilities = vulnerabilities,
            hardeningRecommendations = recommendations
        )
    }

    private fun checkRootBinaries(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }

        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        return false
    }

    sealed class ScanProgressEvent {
        data class Started(val totalApps: Int) : ScanProgressEvent()
        data class Progress(
            val currentIndex: Int,
            val totalCount: Int,
            val currentAppName: String,
            val packageName: String
        ) : ScanProgressEvent()
        data class Completed(val results: List<AppSecurityScanResult>) : ScanProgressEvent()
    }
}
