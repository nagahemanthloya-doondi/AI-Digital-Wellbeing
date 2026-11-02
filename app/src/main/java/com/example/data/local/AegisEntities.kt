package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_rules")
data class BlockRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val ruleType: String, // "WEBSITE" or "KEYWORD"
    val category: String, // "Social", "Adult/NSFW", "Gambling", "News/Doomscroll", "Gaming", "Custom"
    val isEnabled: Boolean = true,
    val hitCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "interception_logs")
data class InterceptionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerType: String, // "WEBSITE", "KEYWORD", "INCOGNITO", "REELS_SHORTS", "AI_CONTEXT", "STRICT_TAMPER"
    val matchedRule: String,
    val packageName: String,
    val appName: String,
    val screenSnippet: String,
    val aiReasoning: String = "",
    val actionTaken: String = "BLOCKED_OVERLAY", // "BLOCKED_OVERLAY", "SNOOZED_5M", "REDIRECTED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "scroll_telemetry")
data class ScrollTelemetryEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String, // "WASTED_RISK", "NEUTRAL", "PRODUCTIVE"
    val scrollEventsCount: Int = 0,
    val scrollDurationMillis: Long = 0L,
    val estimatedScrollMeters: Float = 0f,
    val foregroundUsageMillis: Long = 0L,
    val isAvoidMarked: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "captured_notifications")
data class CapturedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val priorityBucket: String, // "URGENT", "WORK_STUDY", "SOCIAL", "PROMO_NOISE"
    val isSummarized: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_digests")
data class NotificationDigestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val headline: String,
    val executiveSummary: String,
    val bulletPoints: String, // newline-separated condensed bullets
    val actionItems: String,
    val totalCount: Int,
    val mutedNoiseCount: Int,
    val engineBadge: String, // e.g. "On-Device • gemma-4-2b-it.rtlm" or "Cloud API • Gemini / OpenAI"
    val postedToNotificationPanel: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
