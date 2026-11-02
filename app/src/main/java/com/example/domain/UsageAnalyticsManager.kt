package com.example.domain

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.service.AegisAccessibilityService
import com.example.service.AegisNotificationListenerService

data class SystemPermissionStatus(
    val isAccessibilityEnabled: Boolean,
    val isUsageAccessGranted: Boolean,
    val isOverlayPermissionGranted: Boolean,
    val isNotificationAccessGranted: Boolean = false,
    val isPostNotificationsGranted: Boolean = true,
    val isServiceLiveConnected: Boolean = false
) {
    val activeCount: Int
        get() = listOf(
            isAccessibilityEnabled,
            isUsageAccessGranted,
            isOverlayPermissionGranted,
            isNotificationAccessGranted
        ).count { it }

    val allGranted: Boolean
        get() = isAccessibilityEnabled &&
            isUsageAccessGranted &&
            isOverlayPermissionGranted &&
            isNotificationAccessGranted
}

data class InstalledBrowserInfo(
    val packageName: String,
    val appName: String,
    val isInstalledOnDevice: Boolean
)

object UsageAnalyticsManager {

    fun checkPermissions(context: Context): SystemPermissionStatus {
        val accessibilityEnabled = isAccessibilityServiceEnabled(context)
        val usageAccess = isUsageStatsPermissionGranted(context)
        val overlayGranted = Settings.canDrawOverlays(context)
        val notifListenerGranted = isNotificationListenerEnabled(context) ||
            AegisNotificationListenerService.isNotificationListenerConnected.value
        val postNotifGranted = isPostNotificationsPermissionGranted(context)
        val liveConnected = AegisAccessibilityService.isServiceRunning.value

        return SystemPermissionStatus(
            isAccessibilityEnabled = accessibilityEnabled || liveConnected,
            isUsageAccessGranted = usageAccess,
            isOverlayPermissionGranted = overlayGranted,
            isNotificationAccessGranted = notifListenerGranted,
            isPostNotificationsGranted = postNotifGranted,
            isServiceLiveConnected = liveConnected
        )
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = "${context.packageName}/${AegisAccessibilityService::class.java.canonicalName}"
        val expectedShort = "${context.packageName}/.service.AegisAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any {
            it.equals(expectedComponent, ignoreCase = true) ||
                it.equals(expectedShort, ignoreCase = true) ||
                it.contains(context.packageName, ignoreCase = true)
        }
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.contains(context.packageName, ignoreCase = true)
    }

    fun isPostNotificationsPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isUsageStatsPermissionGranted(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openOverlayPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun queryLast24HoursDeviceUsage(context: Context): Map<String, Pair<String, Long>> {
        if (!isUsageStatsPermissionGranted(context)) return emptyMap()
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()
            val end = System.currentTimeMillis()
            val start = end - 24 * 60 * 60 * 1000L
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            if (stats.isNullOrEmpty()) return emptyMap()

            val pm = context.packageManager
            val result = mutableMapOf<String, Pair<String, Long>>()
            for (stat in stats) {
                val totalMs = stat.totalTimeInForeground
                if (totalMs > 30_000L && stat.packageName != context.packageName) {
                    val label = runCatching {
                        val appInfo = pm.getApplicationInfo(stat.packageName, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    }.getOrDefault(stat.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })

                    val prev = result[stat.packageName]?.second ?: 0L
                    result[stat.packageName] = label to maxOf(prev, totalMs)
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun detectInstalledBrowsers(context: Context): List<InstalledBrowserInfo> {
        val pm = context.packageManager
        val detectedPackages = mutableSetOf<String>()

        runCatching {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
            val resolved = pm.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL)
            for (info in resolved) {
                detectedPackages.add(info.activityInfo.packageName)
            }
        }

        return BlockingEngine.KNOWN_BROWSER_PACKAGES.map { (pkg, name) ->
            val installed = detectedPackages.contains(pkg) || runCatching {
                pm.getPackageInfo(pkg, 0)
                true
            }.getOrDefault(false)
            InstalledBrowserInfo(
                packageName = pkg,
                appName = name,
                isInstalledOnDevice = installed
            )
        }.sortedByDescending { it.isInstalledOnDevice }
    }
}
