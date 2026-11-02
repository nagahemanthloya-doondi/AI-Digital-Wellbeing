package com.example

import com.example.data.local.BlockRuleEntity
import com.example.data.local.CapturedNotificationEntity
import com.example.data.preferences.AegisSettingsState
import com.example.domain.BlockingEngine
import com.example.domain.NotificationSummaryEngine
import com.example.domain.ScreenContextSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {

    private val sampleRules = listOf(
        BlockRuleEntity(id = 1, pattern = "tiktok.com", ruleType = "WEBSITE", category = "Social"),
        BlockRuleEntity(id = 2, pattern = "casino", ruleType = "KEYWORD", category = "Gambling")
    )

    @Before
    fun resetSnoozes() {
        BlockingEngine.clearAllSnoozes()
    }

    @Test
    fun websiteAndObfuscatedKeywordBlocker_blocksCorrectly() {
        val settings = AegisSettingsState()

        val webDecision = BlockingEngine.evaluateScreenContext(
            snapshot = ScreenContextSnapshot(
                packageName = "com.android.chrome",
                appName = "Chrome",
                detectedUrl = "https://www.tiktok.com/@trending"
            ),
            settings = settings,
            enabledRules = sampleRules
        )
        assertTrue(webDecision.shouldBlock)
        assertEquals("WEBSITE", webDecision.triggerType)

        val obfuscatedKeywordDecision = BlockingEngine.evaluateScreenContext(
            snapshot = ScreenContextSnapshot(
                packageName = "com.brave.browser",
                appName = "Brave",
                visibleTexts = listOf("Win big at c.4.s.1.n.0 spin today")
            ),
            settings = settings,
            enabledRules = sampleRules
        )
        assertTrue(obfuscatedKeywordDecision.shouldBlock)
        assertEquals("KEYWORD", obfuscatedKeywordDecision.triggerType)
    }

    @Test
    fun incognitoAndShortsBlocker_detectsCorrectly() {
        val settings = AegisSettingsState()

        val incogDecision = BlockingEngine.evaluateScreenContext(
            snapshot = ScreenContextSnapshot(
                packageName = "com.android.chrome",
                appName = "Chrome",
                visibleTexts = listOf("You've gone Incognito")
            ),
            settings = settings,
            enabledRules = emptyList()
        )
        assertTrue(incogDecision.shouldBlock)
        assertEquals("INCOGNITO", incogDecision.triggerType)

        val shortsDecision = BlockingEngine.evaluateScreenContext(
            snapshot = ScreenContextSnapshot(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                viewIdResources = listOf("com.google.android.youtube:id/reel_player_page_container")
            ),
            settings = settings,
            enabledRules = emptyList()
        )
        assertTrue(shortsDecision.shouldBlock)
        assertEquals("REELS_SHORTS", shortsDecision.triggerType)
    }

    @Test
    fun aiScreenContextGuardian_distinguishesStudyFromDrama() {
        val instructions = "Allow coding tutorials and algorithm documentation. Block celebrity gossip and casino."

        val dramaVerdict = BlockingEngine.evaluateAiScreenContext(
            snapshot = ScreenContextSnapshot(
                packageName = "com.android.chrome",
                appName = "Chrome",
                visibleTexts = listOf("Shocking viral celebrity gossip drama #shorts")
            ),
            userInstructions = instructions,
            threshold = 60
        )
        assertTrue(dramaVerdict.shouldBlock)

        val studyVerdict = BlockingEngine.evaluateAiScreenContext(
            snapshot = ScreenContextSnapshot(
                packageName = "com.android.chrome",
                appName = "Chrome",
                visibleTexts = listOf("Kotlin Coroutines documentation and algorithm tutorial lecture")
            ),
            userInstructions = instructions,
            threshold = 60
        )
        assertFalse(studyVerdict.shouldBlock)
    }

    @Test
    fun notificationSummaryEngine_summarizesAndDetectsEdgeExtensions() {
        assertEquals("rtlm", NotificationSummaryEngine.extractModelExtension("/storage/emulated/0/AIEdgeGallery/gemma-4-e2b-it.rtlm"))
        assertEquals("light", NotificationSummaryEngine.extractModelExtension("content://models/gemma-4-2b-q4.light"))
        assertEquals("guf", NotificationSummaryEngine.extractModelExtension("/sdcard/Download/gemma-4-1b-distill.guf"))

        val actionClassification = NotificationSummaryEngine.classifyNotificationPriority(
            packageName = "com.slack",
            title = "Maya Lin",
            content = "Urgent: Can you review PR before 4 PM?"
        )
        assertEquals("URGENT", actionClassification)

        val promoClassification = NotificationSummaryEngine.classifyNotificationPriority(
            packageName = "com.instagram.android",
            title = "Trending Reels",
            content = "50% off flash sale coupon and viral reels waiting for you"
        )
        assertEquals("PROMO_NOISE", promoClassification)

        val settings = AegisSettingsState()
        val batch = listOf(
            CapturedNotificationEntity(
                id = 1,
                packageName = "com.slack",
                appName = "Slack",
                title = "Maya Lin",
                content = "Urgent: Review PR before 4 PM",
                priorityBucket = "URGENT"
            ),
            CapturedNotificationEntity(
                id = 2,
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Reels Digest",
                content = "Viral reels waiting for you",
                priorityBucket = "PROMO_NOISE"
            )
        )
        val digest = NotificationSummaryEngine.synthesizeWithOnDeviceEdgeLlm(batch, settings)
        assertEquals(2, digest.totalCount)
        assertEquals(1, digest.mutedNoiseCount)
        assertTrue(digest.bulletPoints.isNotEmpty())
        assertTrue(digest.actionItems.isNotEmpty())
    }
}


