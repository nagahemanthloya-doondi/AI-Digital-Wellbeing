package com.example.data

import android.content.Context
import com.example.data.local.AegisDao
import com.example.data.local.AegisDatabase
import com.example.data.local.BlockRuleEntity
import com.example.data.local.CapturedNotificationEntity
import com.example.data.local.InterceptionLogEntity
import com.example.data.local.NotificationDigestEntity
import com.example.data.local.ScrollTelemetryEntity
import com.example.data.preferences.AegisSettingsDataStore
import com.example.data.preferences.AegisSettingsState
import kotlinx.coroutines.flow.Flow

class AegisRepository(
    private val dao: AegisDao,
    val settingsDataStore: AegisSettingsDataStore
) {
    val allRules: Flow<List<BlockRuleEntity>> = dao.observeAllRules()
    val recentLogs: Flow<List<InterceptionLogEntity>> = dao.observeRecentLogs()
    val totalInterceptionsCount: Flow<Int> = dao.observeTotalInterceptionsCount()
    val allTelemetry: Flow<List<ScrollTelemetryEntity>> = dao.observeAllTelemetry()
    val capturedNotifications: Flow<List<CapturedNotificationEntity>> = dao.observeCapturedNotifications()
    val notificationDigests: Flow<List<NotificationDigestEntity>> = dao.observeNotificationDigests()
    val settingsFlow: Flow<AegisSettingsState> = settingsDataStore.settingsFlow

    suspend fun getEnabledRulesSnapshot(): List<BlockRuleEntity> = dao.getEnabledRulesSnapshot()

    suspend fun addRule(pattern: String, ruleType: String, category: String) {
        val clean = pattern.trim().lowercase()
        if (clean.isEmpty()) return
        dao.insertRule(
            BlockRuleEntity(
                pattern = clean,
                ruleType = ruleType,
                category = category,
                isEnabled = true
            )
        )
    }

    suspend fun toggleRule(rule: BlockRuleEntity, enabled: Boolean) {
        dao.updateRule(rule.copy(isEnabled = enabled))
    }

    suspend fun deleteRule(ruleId: Long) {
        dao.deleteRuleById(ruleId)
    }

    suspend fun recordInterception(
        triggerType: String,
        matchedRule: String,
        packageName: String,
        appName: String,
        screenSnippet: String,
        aiReasoning: String = "",
        actionTaken: String = "BLOCKED_OVERLAY",
        ruleIdToIncrement: Long? = null
    ) {
        if (ruleIdToIncrement != null) {
            dao.incrementRuleHitCount(ruleIdToIncrement)
        }
        dao.insertInterceptionLog(
            InterceptionLogEntity(
                triggerType = triggerType,
                matchedRule = matchedRule,
                packageName = packageName,
                appName = appName,
                screenSnippet = screenSnippet.take(180),
                aiReasoning = aiReasoning,
                actionTaken = actionTaken
            )
        )
    }

    suspend fun clearLogs() {
        dao.clearAllLogs()
    }

    suspend fun recordScrollEvent(
        packageName: String,
        appName: String,
        addedDurationMs: Long,
        addedMeters: Float,
        isAvoidDefault: Boolean
    ) {
        val existing = dao.getTelemetryForPackage(packageName)
        if (existing != null) {
            dao.upsertTelemetry(
                existing.copy(
                    scrollEventsCount = existing.scrollEventsCount + 1,
                    scrollDurationMillis = existing.scrollDurationMillis + addedDurationMs,
                    estimatedScrollMeters = existing.estimatedScrollMeters + addedMeters,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else {
            dao.upsertTelemetry(
                ScrollTelemetryEntity(
                    packageName = packageName,
                    appName = appName,
                    category = if (isAvoidDefault) "WASTED_RISK" else "NEUTRAL",
                    scrollEventsCount = 1,
                    scrollDurationMillis = addedDurationMs,
                    estimatedScrollMeters = addedMeters,
                    foregroundUsageMillis = addedDurationMs,
                    isAvoidMarked = isAvoidDefault,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun setPackageAvoidStatus(packageName: String, avoid: Boolean) {
        val category = if (avoid) "WASTED_RISK" else "PRODUCTIVE"
        dao.setPackageAvoidStatus(packageName, avoid, category)
    }

    suspend fun syncDeviceUsageStats(usageMap: Map<String, Pair<String, Long>>) {
        for ((pkg, pair) in usageMap) {
            val (label, usageMs) = pair
            val existing = dao.getTelemetryForPackage(pkg)
            if (existing != null) {
                dao.upsertTelemetry(
                    existing.copy(
                        appName = label.ifBlank { existing.appName },
                        foregroundUsageMillis = maxOf(existing.foregroundUsageMillis, usageMs)
                    )
                )
            } else if (usageMs > 60_000L) {
                val isSocialOrShortForm = pkg.contains("youtube") ||
                    pkg.contains("instagram") ||
                    pkg.contains("tiktok") ||
                    pkg.contains("musically") ||
                    pkg.contains("facebook") ||
                    pkg.contains("twitter") ||
                    pkg.contains("reddit")
                dao.upsertTelemetry(
                    ScrollTelemetryEntity(
                        packageName = pkg,
                        appName = label,
                        category = if (isSocialOrShortForm) "WASTED_RISK" else "NEUTRAL",
                        scrollEventsCount = (usageMs / 15_000L).toInt(),
                        scrollDurationMillis = (usageMs * 0.45f).toLong(),
                        estimatedScrollMeters = (usageMs / 60_000f) * 2.4f,
                        foregroundUsageMillis = usageMs,
                        isAvoidMarked = isSocialOrShortForm
                    )
                )
            }
        }
    }

    // --- Notification Capture & Digest Operations ---
    suspend fun insertCapturedNotification(
        packageName: String,
        appName: String,
        title: String,
        content: String,
        priorityBucket: String
    ): Long {
        return dao.insertCapturedNotification(
            CapturedNotificationEntity(
                packageName = packageName,
                appName = appName,
                title = title,
                content = content,
                priorityBucket = priorityBucket,
                isSummarized = false
            )
        )
    }

    suspend fun getRecentCapturedNotificationsSnapshot(): List<CapturedNotificationEntity> {
        return dao.getRecentCapturedNotificationsSnapshot()
    }

    suspend fun getUnsummarizedNotificationCount(): Int {
        return dao.getUnsummarizedNotificationCount()
    }

    suspend fun saveNotificationDigest(digest: NotificationDigestEntity) {
        dao.insertNotificationDigest(digest)
        dao.markAllNotificationsSummarized()
    }

    suspend fun clearCapturedNotifications() {
        dao.clearCapturedNotifications()
    }

    companion object {
        @Volatile
        private var INSTANCE: AegisRepository? = null

        fun getInstance(context: Context): AegisRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AegisDatabase.getInstance(context)
                val settings = AegisSettingsDataStore(context.applicationContext)
                AegisRepository(db.aegisDao(), settings).also { INSTANCE = it }
            }
        }
    }
}
