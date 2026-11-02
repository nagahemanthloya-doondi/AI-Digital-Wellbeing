package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aegisDataStore: DataStore<Preferences> by preferencesDataStore(name = "aegis_control_prefs")

enum class BlockScreenVisualMode {
    DEFAULT_CODE,
    CUSTOM_IMAGE,
    CUSTOM_VIDEO,
    REDIRECT_URL
}

enum class LlmExecutionBackend {
    ON_DEVICE_EDGE, // AI Edge Gallery Gemma 4 / Small LLMs (.light, .rtlm, .litertlm, .guf, .gguf)
    CLOUD_API_KEY   // Any LLM Provider API Key (Gemini, OpenAI, Anthropic, Groq, OpenRouter, Custom)
}

data class AegisSettingsState(
    // Core Blocker Toggles
    val websiteKeywordBlockerEnabled: Boolean = true,
    val incognitoBlockerEnabled: Boolean = true,
    val youtubeShortsBlockerEnabled: Boolean = true,
    val instagramReelsBlockerEnabled: Boolean = true,
    val tiktokBlockerEnabled: Boolean = true,
    val facebookReelsBlockerEnabled: Boolean = true,
    val aggressiveFuzzyMatching: Boolean = true,

    // Strict Mode State
    val strictModeActive: Boolean = false,
    val strictModeStartTimestamp: Long = 0L,
    val strictModeEndTimestamp: Long = 0L,
    val strictModeTotalMinutesStayed: Long = 185L,
    val strictModePreventSettingsTamper: Boolean = true,
    val strictModeDisableFiveMinAccess: Boolean = false,

    // Optional AI Screen Context Guardian
    val aiGuardianEnabled: Boolean = true,
    val aiUserInstructions: String = "Allow software engineering, coding documentation, academic research, and educational tutorials. Block mindless short-form entertainment, celebrity gossip, gambling, viral drama, and NSFW distractions.",
    val aiSensitivityThreshold: Int = 65,
    val aiAutoBlockOnVerdict: Boolean = true,

    // Notification Summary & LLM Engine Settings
    val notificationSummaryEnabled: Boolean = true,
    val autoPostSummaryToNotificationPanel: Boolean = true,
    val suppressPromoNotificationsWhenSummarized: Boolean = true,
    val llmBackend: LlmExecutionBackend = LlmExecutionBackend.ON_DEVICE_EDGE,
    // AI Edge Gallery On-Device LLM (.light, .rtlm, .litertlm, .guf, .gguf)
    val onDeviceModelId: String = "gemma-4-e2b-it.rtlm",
    val customOnDeviceModelPath: String = "",
    val customOnDeviceModelExtension: String = "rtlm",
    // Cloud LLM Provider API Key settings
    val cloudProviderName: String = "GEMINI", // "GEMINI", "OPENAI", "ANTHROPIC", "GROQ", "OPENROUTER", "CUSTOM"
    val cloudModelName: String = "gemini-2.5-flash",
    val cloudCustomEndpoint: String = "https://api.openai.com/v1/chat/completions",
    val cloudApiKeyInput: String = "",

    // Editable Blocking Screen Configuration
    val blockScreenMode: BlockScreenVisualMode = BlockScreenVisualMode.DEFAULT_CODE,
    val defaultChallengeCode: String = "DISCIPLINE-OVER-REGRET-849",
    val useRandomChallengeCode: Boolean = false,
    val blockHeadline: String = "SANCTUARY SHIELD ENGAGED",
    val blockMessage: String = "This content conflicts with your self-control charter. Reclaim your focus.",
    val customImageUri: String = "PRESET_GUARDIAN",
    val customVideoUri: String = "",
    val redirectUrl: String = "https://en.wikipedia.org/wiki/Flow_(psychology)",
    val autoRedirectImmediately: Boolean = false,

    // Daily Scroll & Wasted Time Goals
    val dailyWastedTimeLimitMinutes: Int = 45,
    val dailyScrollMetersLimit: Int = 250
) {
    val isStrictCurrentlyLocked: Boolean
        get() = strictModeActive && System.currentTimeMillis() < strictModeEndTimestamp

    val activeLlmDisplayBadge: String
        get() = if (llmBackend == LlmExecutionBackend.ON_DEVICE_EDGE) {
            val fileLabel = customOnDeviceModelPath.substringAfterLast('/').ifBlank { onDeviceModelId }
            "On-Device • $fileLabel"
        } else {
            "Cloud API • $cloudProviderName ($cloudModelName)"
        }
}

class AegisSettingsDataStore(private val context: Context) {

    private object Keys {
        val WEBSITE_KEYWORD_ENABLED = booleanPreferencesKey("website_keyword_enabled")
        val INCOGNITO_ENABLED = booleanPreferencesKey("incognito_enabled")
        val YT_SHORTS_ENABLED = booleanPreferencesKey("yt_shorts_enabled")
        val IG_REELS_ENABLED = booleanPreferencesKey("ig_reels_enabled")
        val TIKTOK_ENABLED = booleanPreferencesKey("tiktok_enabled")
        val FB_REELS_ENABLED = booleanPreferencesKey("fb_reels_enabled")
        val AGGRESSIVE_FUZZY = booleanPreferencesKey("aggressive_fuzzy")

        val STRICT_ACTIVE = booleanPreferencesKey("strict_active")
        val STRICT_START_TS = longPreferencesKey("strict_start_ts")
        val STRICT_END_TS = longPreferencesKey("strict_end_ts")
        val STRICT_TOTAL_MINS = longPreferencesKey("strict_total_mins")
        val STRICT_PREVENT_TAMPER = booleanPreferencesKey("strict_prevent_tamper")
        val STRICT_DISABLE_SNOOZE = booleanPreferencesKey("strict_disable_snooze")

        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_INSTRUCTIONS = stringPreferencesKey("ai_instructions")
        val AI_THRESHOLD = intPreferencesKey("ai_threshold")
        val AI_AUTO_BLOCK = booleanPreferencesKey("ai_auto_block")

        val NOTIF_SUMMARY_ENABLED = booleanPreferencesKey("notif_summary_enabled")
        val AUTO_POST_PANEL = booleanPreferencesKey("auto_post_panel")
        val SUPPRESS_PROMO_NOTIFS = booleanPreferencesKey("suppress_promo_notifs")
        val LLM_BACKEND = stringPreferencesKey("llm_backend")
        val ON_DEVICE_MODEL_ID = stringPreferencesKey("on_device_model_id")
        val CUSTOM_MODEL_PATH = stringPreferencesKey("custom_model_path")
        val CUSTOM_MODEL_EXT = stringPreferencesKey("custom_model_ext")
        val CLOUD_PROVIDER = stringPreferencesKey("cloud_provider")
        val CLOUD_MODEL = stringPreferencesKey("cloud_model")
        val CLOUD_ENDPOINT = stringPreferencesKey("cloud_endpoint")
        val CLOUD_API_KEY_INPUT = stringPreferencesKey("cloud_api_key_input")

        val BLOCK_MODE = stringPreferencesKey("block_mode")
        val CHALLENGE_CODE = stringPreferencesKey("challenge_code")
        val RANDOM_CODE = booleanPreferencesKey("random_code")
        val BLOCK_HEADLINE = stringPreferencesKey("block_headline")
        val BLOCK_MESSAGE = stringPreferencesKey("block_message")
        val CUSTOM_IMAGE_URI = stringPreferencesKey("custom_image_uri")
        val CUSTOM_VIDEO_URI = stringPreferencesKey("custom_video_uri")
        val REDIRECT_URL = stringPreferencesKey("redirect_url")
        val AUTO_REDIRECT = booleanPreferencesKey("auto_redirect")

        val DAILY_WASTE_LIMIT = intPreferencesKey("daily_waste_limit")
        val DAILY_SCROLL_LIMIT = intPreferencesKey("daily_scroll_limit")
    }

    val settingsFlow: Flow<AegisSettingsState> = context.aegisDataStore.data.map { prefs ->
        val modeStr = prefs[Keys.BLOCK_MODE] ?: BlockScreenVisualMode.DEFAULT_CODE.name
        val mode = runCatching { BlockScreenVisualMode.valueOf(modeStr) }
            .getOrDefault(BlockScreenVisualMode.DEFAULT_CODE)

        val backendStr = prefs[Keys.LLM_BACKEND] ?: LlmExecutionBackend.ON_DEVICE_EDGE.name
        val llmBackend = runCatching { LlmExecutionBackend.valueOf(backendStr) }
            .getOrDefault(LlmExecutionBackend.ON_DEVICE_EDGE)

        val endTs = prefs[Keys.STRICT_END_TS] ?: 0L
        val rawStrictActive = prefs[Keys.STRICT_ACTIVE] ?: false
        val effectiveStrictActive = rawStrictActive && System.currentTimeMillis() < endTs

        AegisSettingsState(
            websiteKeywordBlockerEnabled = prefs[Keys.WEBSITE_KEYWORD_ENABLED] ?: true,
            incognitoBlockerEnabled = prefs[Keys.INCOGNITO_ENABLED] ?: true,
            youtubeShortsBlockerEnabled = prefs[Keys.YT_SHORTS_ENABLED] ?: true,
            instagramReelsBlockerEnabled = prefs[Keys.IG_REELS_ENABLED] ?: true,
            tiktokBlockerEnabled = prefs[Keys.TIKTOK_ENABLED] ?: true,
            facebookReelsBlockerEnabled = prefs[Keys.FB_REELS_ENABLED] ?: true,
            aggressiveFuzzyMatching = prefs[Keys.AGGRESSIVE_FUZZY] ?: true,

            strictModeActive = effectiveStrictActive,
            strictModeStartTimestamp = prefs[Keys.STRICT_START_TS] ?: 0L,
            strictModeEndTimestamp = endTs,
            strictModeTotalMinutesStayed = prefs[Keys.STRICT_TOTAL_MINS] ?: 185L,
            strictModePreventSettingsTamper = prefs[Keys.STRICT_PREVENT_TAMPER] ?: true,
            strictModeDisableFiveMinAccess = prefs[Keys.STRICT_DISABLE_SNOOZE] ?: false,

            aiGuardianEnabled = prefs[Keys.AI_ENABLED] ?: true,
            aiUserInstructions = prefs[Keys.AI_INSTRUCTIONS]
                ?: "Allow software engineering, coding documentation, academic research, and educational tutorials. Block mindless short-form entertainment, celebrity gossip, gambling, viral drama, and NSFW distractions.",
            aiSensitivityThreshold = prefs[Keys.AI_THRESHOLD] ?: 65,
            aiAutoBlockOnVerdict = prefs[Keys.AI_AUTO_BLOCK] ?: true,

            notificationSummaryEnabled = prefs[Keys.NOTIF_SUMMARY_ENABLED] ?: true,
            autoPostSummaryToNotificationPanel = prefs[Keys.AUTO_POST_PANEL] ?: true,
            suppressPromoNotificationsWhenSummarized = prefs[Keys.SUPPRESS_PROMO_NOTIFS] ?: true,
            llmBackend = llmBackend,
            onDeviceModelId = prefs[Keys.ON_DEVICE_MODEL_ID] ?: "gemma-4-e2b-it.rtlm",
            customOnDeviceModelPath = prefs[Keys.CUSTOM_MODEL_PATH] ?: "",
            customOnDeviceModelExtension = prefs[Keys.CUSTOM_MODEL_EXT] ?: "rtlm",
            cloudProviderName = prefs[Keys.CLOUD_PROVIDER] ?: "GEMINI",
            cloudModelName = prefs[Keys.CLOUD_MODEL] ?: "gemini-2.5-flash",
            cloudCustomEndpoint = prefs[Keys.CLOUD_ENDPOINT] ?: "https://api.openai.com/v1/chat/completions",
            cloudApiKeyInput = prefs[Keys.CLOUD_API_KEY_INPUT] ?: "",

            blockScreenMode = mode,
            defaultChallengeCode = prefs[Keys.CHALLENGE_CODE] ?: "DISCIPLINE-OVER-REGRET-849",
            useRandomChallengeCode = prefs[Keys.RANDOM_CODE] ?: false,
            blockHeadline = prefs[Keys.BLOCK_HEADLINE] ?: "SANCTUARY SHIELD ENGAGED",
            blockMessage = prefs[Keys.BLOCK_MESSAGE]
                ?: "This content conflicts with your self-control charter. Reclaim your focus.",
            customImageUri = prefs[Keys.CUSTOM_IMAGE_URI] ?: "PRESET_GUARDIAN",
            customVideoUri = prefs[Keys.CUSTOM_VIDEO_URI] ?: "",
            redirectUrl = prefs[Keys.REDIRECT_URL] ?: "https://en.wikipedia.org/wiki/Flow_(psychology)",
            autoRedirectImmediately = prefs[Keys.AUTO_REDIRECT] ?: false,

            dailyWastedTimeLimitMinutes = prefs[Keys.DAILY_WASTE_LIMIT] ?: 45,
            dailyScrollMetersLimit = prefs[Keys.DAILY_SCROLL_LIMIT] ?: 250
        )
    }

    suspend fun setWebsiteKeywordBlocker(enabled: Boolean) {
        context.aegisDataStore.edit { it[Keys.WEBSITE_KEYWORD_ENABLED] = enabled }
    }

    suspend fun setIncognitoBlocker(enabled: Boolean) {
        context.aegisDataStore.edit { it[Keys.INCOGNITO_ENABLED] = enabled }
    }

    suspend fun setYoutubeShortsBlocker(enabled: Boolean) {
        context.aegisDataStore.edit { it[Keys.YT_SHORTS_ENABLED] = enabled }
    }

    suspend fun setInstagramReelsBlocker(enabled: Boolean) {
        context.aegisDataStore.edit { it[Keys.IG_REELS_ENABLED] = enabled }
    }

    suspend fun setTiktokBlocker(enabled: Boolean) {
        context.aegisDataStore.edit { it[Keys.TIKTOK_ENABLED] = enabled }
    }

    suspend fun setFacebookReelsBlocker(enabled: Boolean) {
        context.aegisDataStore.edit { it[Keys.FB_REELS_ENABLED] = enabled }
    }

    suspend fun setAggressiveFuzzyMatching(enabled: Boolean) {
        context.aegisDataStore.edit { it[Keys.AGGRESSIVE_FUZZY] = enabled }
    }

    suspend fun activateStrictMode(durationMinutes: Int, preventTamper: Boolean, disableSnooze: Boolean) {
        val now = System.currentTimeMillis()
        val end = now + durationMinutes * 60_000L
        context.aegisDataStore.edit { prefs ->
            prefs[Keys.STRICT_ACTIVE] = true
            prefs[Keys.STRICT_START_TS] = now
            prefs[Keys.STRICT_END_TS] = end
            prefs[Keys.STRICT_PREVENT_TAMPER] = preventTamper
            prefs[Keys.STRICT_DISABLE_SNOOZE] = disableSnooze
            prefs[Keys.WEBSITE_KEYWORD_ENABLED] = true
            prefs[Keys.INCOGNITO_ENABLED] = true
            prefs[Keys.YT_SHORTS_ENABLED] = true
            prefs[Keys.IG_REELS_ENABLED] = true
        }
    }

    suspend fun emergencyUnlockStrictModeWithCode() {
        context.aegisDataStore.edit { prefs ->
            val start = prefs[Keys.STRICT_START_TS] ?: 0L
            if (start > 0L) {
                val stayedMins = ((System.currentTimeMillis() - start).coerceAtLeast(0L)) / 60_000L
                val prev = prefs[Keys.STRICT_TOTAL_MINS] ?: 185L
                prefs[Keys.STRICT_TOTAL_MINS] = prev + stayedMins
            }
            prefs[Keys.STRICT_ACTIVE] = false
            prefs[Keys.STRICT_END_TS] = 0L
        }
    }

    suspend fun updateAiGuardianSettings(
        enabled: Boolean,
        instructions: String,
        threshold: Int,
        autoBlock: Boolean
    ) {
        context.aegisDataStore.edit { prefs ->
            prefs[Keys.AI_ENABLED] = enabled
            prefs[Keys.AI_INSTRUCTIONS] = instructions
            prefs[Keys.AI_THRESHOLD] = threshold.coerceIn(20, 95)
            prefs[Keys.AI_AUTO_BLOCK] = autoBlock
        }
    }

    suspend fun updateNotificationSummarySettings(
        enabled: Boolean,
        autoPostPanel: Boolean,
        suppressPromos: Boolean,
        backend: LlmExecutionBackend,
        onDeviceModelId: String,
        customModelPath: String,
        customModelExt: String,
        cloudProvider: String,
        cloudModel: String,
        cloudEndpoint: String,
        cloudApiKey: String
    ) {
        context.aegisDataStore.edit { prefs ->
            prefs[Keys.NOTIF_SUMMARY_ENABLED] = enabled
            prefs[Keys.AUTO_POST_PANEL] = autoPostPanel
            prefs[Keys.SUPPRESS_PROMO_NOTIFS] = suppressPromos
            prefs[Keys.LLM_BACKEND] = backend.name
            prefs[Keys.ON_DEVICE_MODEL_ID] = onDeviceModelId
            prefs[Keys.CUSTOM_MODEL_PATH] = customModelPath
            prefs[Keys.CUSTOM_MODEL_EXT] = customModelExt
            prefs[Keys.CLOUD_PROVIDER] = cloudProvider
            prefs[Keys.CLOUD_MODEL] = cloudModel
            prefs[Keys.CLOUD_ENDPOINT] = cloudEndpoint
            prefs[Keys.CLOUD_API_KEY_INPUT] = cloudApiKey
        }
    }

    suspend fun updateBlockScreenConfig(
        mode: BlockScreenVisualMode,
        challengeCode: String,
        useRandomCode: Boolean,
        headline: String,
        message: String,
        customImageUri: String,
        customVideoUri: String,
        redirectUrl: String,
        autoRedirect: Boolean
    ) {
        context.aegisDataStore.edit { prefs ->
            prefs[Keys.BLOCK_MODE] = mode.name
            prefs[Keys.CHALLENGE_CODE] = challengeCode.ifBlank { "FOCUS-NOW-108" }
            prefs[Keys.RANDOM_CODE] = useRandomCode
            prefs[Keys.BLOCK_HEADLINE] = headline.ifBlank { "SANCTUARY SHIELD ENGAGED" }
            prefs[Keys.BLOCK_MESSAGE] = message.ifBlank { "Reclaim your attention." }
            prefs[Keys.CUSTOM_IMAGE_URI] = customImageUri
            prefs[Keys.CUSTOM_VIDEO_URI] = customVideoUri
            prefs[Keys.REDIRECT_URL] = redirectUrl.ifBlank { "https://en.wikipedia.org/wiki/Deep_Work" }
            prefs[Keys.AUTO_REDIRECT] = autoRedirect
        }
    }
}
