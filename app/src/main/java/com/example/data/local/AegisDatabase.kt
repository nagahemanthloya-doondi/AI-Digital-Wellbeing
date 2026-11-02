package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BlockRuleEntity::class,
        InterceptionLogEntity::class,
        ScrollTelemetryEntity::class,
        CapturedNotificationEntity::class,
        NotificationDigestEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun aegisDao(): AegisDao

    companion object {
        @Volatile
        private var INSTANCE: AegisDatabase? = null

        fun getInstance(context: Context): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AegisDatabase::class.java,
                    "aegis_self_control.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                seedInitialDataIfNeeded(instance.aegisDao())
                instance
            }
        }

        private fun seedInitialDataIfNeeded(dao: AegisDao) {
            CoroutineScope(Dispatchers.IO).launch {
                if (dao.getRuleCount() == 0) {
                    val starterRules = listOf(
                        BlockRuleEntity(
                            pattern = "tiktok.com",
                            ruleType = "WEBSITE",
                            category = "Social",
                            isEnabled = true,
                            hitCount = 14
                        ),
                        BlockRuleEntity(
                            pattern = "reddit.com/r/all",
                            ruleType = "WEBSITE",
                            category = "News/Doomscroll",
                            isEnabled = true,
                            hitCount = 9
                        ),
                        BlockRuleEntity(
                            pattern = "x.com/explore",
                            ruleType = "WEBSITE",
                            category = "Social",
                            isEnabled = true,
                            hitCount = 11
                        ),
                        BlockRuleEntity(
                            pattern = "instagram.com/reels",
                            ruleType = "WEBSITE",
                            category = "Social",
                            isEnabled = true,
                            hitCount = 19
                        ),
                        BlockRuleEntity(
                            pattern = "youtube.com/shorts",
                            ruleType = "WEBSITE",
                            category = "Social",
                            isEnabled = true,
                            hitCount = 23
                        ),
                        BlockRuleEntity(
                            pattern = "stake.com",
                            ruleType = "WEBSITE",
                            category = "Gambling",
                            isEnabled = true,
                            hitCount = 4
                        ),
                        BlockRuleEntity(
                            pattern = "casino",
                            ruleType = "KEYWORD",
                            category = "Gambling",
                            isEnabled = true,
                            hitCount = 6
                        ),
                        BlockRuleEntity(
                            pattern = "nsfw",
                            ruleType = "KEYWORD",
                            category = "Adult/NSFW",
                            isEnabled = true,
                            hitCount = 8
                        ),
                        BlockRuleEntity(
                            pattern = "onlyfans",
                            ruleType = "KEYWORD",
                            category = "Adult/NSFW",
                            isEnabled = true,
                            hitCount = 5
                        ),
                        BlockRuleEntity(
                            pattern = "celebrity gossip",
                            ruleType = "KEYWORD",
                            category = "News/Doomscroll",
                            isEnabled = true,
                            hitCount = 7
                        ),
                        BlockRuleEntity(
                            pattern = "betting odds",
                            ruleType = "KEYWORD",
                            category = "Gambling",
                            isEnabled = true,
                            hitCount = 3
                        ),
                        BlockRuleEntity(
                            pattern = "doomscroll",
                            ruleType = "KEYWORD",
                            category = "News/Doomscroll",
                            isEnabled = true,
                            hitCount = 5
                        )
                    )
                    dao.insertRules(starterRules)

                    val now = System.currentTimeMillis()
                    dao.insertInterceptionLog(
                        InterceptionLogEntity(
                            triggerType = "REELS_SHORTS",
                            matchedRule = "YouTube Shorts Vertical Feed",
                            packageName = "com.google.android.youtube",
                            appName = "YouTube",
                            screenSnippet = "Shorts player container detected (reel_player_page_container)",
                            actionTaken = "BLOCKED_OVERLAY",
                            timestamp = now - 18 * 60 * 1000L
                        )
                    )
                    dao.insertInterceptionLog(
                        InterceptionLogEntity(
                            triggerType = "INCOGNITO",
                            matchedRule = "Browser Incognito Tab Lock",
                            packageName = "com.android.chrome",
                            appName = "Chrome",
                            screenSnippet = "New incognito tab toolbar & private badge intercepted",
                            actionTaken = "BLOCKED_OVERLAY",
                            timestamp = now - 47 * 60 * 1000L
                        )
                    )
                    dao.insertInterceptionLog(
                        InterceptionLogEntity(
                            triggerType = "KEYWORD",
                            matchedRule = "Keyword: \"celebrity gossip\"",
                            packageName = "com.brave.browser",
                            appName = "Brave Browser",
                            screenSnippet = "Trending viral celebrity gossip thread...",
                            actionTaken = "REDIRECTED",
                            timestamp = now - 95 * 60 * 1000L
                        )
                    )
                    dao.insertInterceptionLog(
                        InterceptionLogEntity(
                            triggerType = "AI_CONTEXT",
                            matchedRule = "AI Context: High Distraction Score (88%)",
                            packageName = "com.android.chrome",
                            appName = "Chrome",
                            screenSnippet = "Endless viral reaction compilation & drama feed",
                            aiReasoning = "Matched 4 high-dopamine entertainment signals conflicting with user Deep Work goal.",
                            actionTaken = "BLOCKED_OVERLAY",
                            timestamp = now - 140 * 60 * 1000L
                        )
                    )
                }

                if (dao.getTelemetryCount() == 0) {
                    val now = System.currentTimeMillis()
                    val initialTelemetry = listOf(
                        ScrollTelemetryEntity(
                            packageName = "com.google.android.youtube",
                            appName = "YouTube (Shorts & Feed)",
                            category = "WASTED_RISK",
                            scrollEventsCount = 412,
                            scrollDurationMillis = 38 * 60 * 1000L,
                            estimatedScrollMeters = 148.4f,
                            foregroundUsageMillis = 54 * 60 * 1000L,
                            isAvoidMarked = true,
                            lastUpdated = now
                        ),
                        ScrollTelemetryEntity(
                            packageName = "com.instagram.android",
                            appName = "Instagram (Reels & Explore)",
                            category = "WASTED_RISK",
                            scrollEventsCount = 530,
                            scrollDurationMillis = 44 * 60 * 1000L,
                            estimatedScrollMeters = 192.6f,
                            foregroundUsageMillis = 49 * 60 * 1000L,
                            isAvoidMarked = true,
                            lastUpdated = now
                        ),
                        ScrollTelemetryEntity(
                            packageName = "com.android.chrome",
                            appName = "Chrome Browser",
                            category = "NEUTRAL",
                            scrollEventsCount = 215,
                            scrollDurationMillis = 19 * 60 * 1000L,
                            estimatedScrollMeters = 64.2f,
                            foregroundUsageMillis = 41 * 60 * 1000L,
                            isAvoidMarked = false,
                            lastUpdated = now
                        ),
                        ScrollTelemetryEntity(
                            packageName = "com.zhiliaoapp.musically",
                            appName = "TikTok Short Feed",
                            category = "WASTED_RISK",
                            scrollEventsCount = 310,
                            scrollDurationMillis = 27 * 60 * 1000L,
                            estimatedScrollMeters = 118.0f,
                            foregroundUsageMillis = 29 * 60 * 1000L,
                            isAvoidMarked = true,
                            lastUpdated = now
                        ),
                        ScrollTelemetryEntity(
                            packageName = "md.obsidian",
                            appName = "Obsidian Notes & Study",
                            category = "PRODUCTIVE",
                            scrollEventsCount = 68,
                            scrollDurationMillis = 7 * 60 * 1000L,
                            estimatedScrollMeters = 18.5f,
                            foregroundUsageMillis = 62 * 60 * 1000L,
                            isAvoidMarked = false,
                            lastUpdated = now
                        )
                    )
                    dao.upsertTelemetryList(initialTelemetry)
                }

                if (dao.getTotalCapturedNotificationCount() == 0) {
                    val now = System.currentTimeMillis()
                    val starterNotifications = listOf(
                        CapturedNotificationEntity(
                            packageName = "com.Slack",
                            appName = "Slack",
                            title = "#android-release • Priya Sharma",
                            content = "Can you review PR #418 for the Accessibility overlay state machine before 4 PM?",
                            priorityBucket = "URGENT",
                            isSummarized = false,
                            timestamp = now - 4 * 60 * 1000L
                        ),
                        CapturedNotificationEntity(
                            packageName = "com.google.android.gm",
                            appName = "Gmail",
                            title = "Cloud Architecture Summit",
                            content = "Your speaker confirmation and slide deck submission deadline is tomorrow at noon.",
                            priorityBucket = "WORK_STUDY",
                            isSummarized = false,
                            timestamp = now - 9 * 60 * 1000L
                        ),
                        CapturedNotificationEntity(
                            packageName = "com.whatsapp",
                            appName = "WhatsApp",
                            title = "Family Group (4 messages)",
                            content = "Mom: Dinner at 7:30 PM tonight, let me know if you're bringing dessert.",
                            priorityBucket = "URGENT",
                            isSummarized = false,
                            timestamp = now - 14 * 60 * 1000L
                        ),
                        CapturedNotificationEntity(
                            packageName = "com.instagram.android",
                            appName = "Instagram",
                            title = "Trending Reels for You",
                            content = "Watch 15 new viral Reels from creators you might like! Tap to scroll now.",
                            priorityBucket = "PROMO_NOISE",
                            isSummarized = false,
                            timestamp = now - 22 * 60 * 1000L
                        ),
                        CapturedNotificationEntity(
                            packageName = "com.google.android.youtube",
                            appName = "YouTube",
                            title = "Recommended Shorts Compilation",
                            content = "Trending gaming moments and celebrity reaction clips are live now!",
                            priorityBucket = "PROMO_NOISE",
                            isSummarized = false,
                            timestamp = now - 28 * 60 * 1000L
                        )
                    )
                    dao.insertCapturedNotifications(starterNotifications)

                    dao.insertNotificationDigest(
                        NotificationDigestEntity(
                            headline = "5 Notifications Condensed • 2 Action Items, 2 Distractions Muted",
                            executiveSummary = "Review PR #418 on Slack before 4 PM and reply to Mom regarding 7:30 PM dinner. Slide deck due tomorrow at noon. 2 viral Reels/Shorts promotions were automatically suppressed.",
                            bulletPoints = "• [Urgent] Slack (Priya): Review PR #418 for Accessibility overlay before 4 PM\n• [Personal] WhatsApp (Mom): Dinner at 7:30 PM tonight — confirm dessert\n• [Work] Gmail: Speaker slide deck deadline tomorrow 12:00 PM\n• [Suppressed] 2 promotional pings from Instagram Reels & YouTube Shorts",
                            actionItems = "1. Approve PR #418 by 16:00 • 2. Confirm 19:30 dinner on WhatsApp",
                            totalCount = 5,
                            mutedNoiseCount = 2,
                            engineBadge = "On-Device • gemma-4-e2b-it.rtlm",
                            postedToNotificationPanel = true,
                            timestamp = now - 2 * 60 * 1000L
                        )
                    )
                }
            }
        }
    }
}
