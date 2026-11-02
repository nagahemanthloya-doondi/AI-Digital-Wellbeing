package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AegisRepository
import com.example.data.local.BlockRuleEntity
import com.example.data.local.CapturedNotificationEntity
import com.example.data.local.InterceptionLogEntity
import com.example.data.local.NotificationDigestEntity
import com.example.data.local.ScrollTelemetryEntity
import com.example.data.preferences.AegisSettingsState
import com.example.data.preferences.BlockScreenVisualMode
import com.example.data.preferences.LlmExecutionBackend
import com.example.domain.AiContextVerdict
import com.example.domain.BlockingEngine
import com.example.domain.InstalledBrowserInfo
import com.example.domain.InterceptionDecision
import com.example.domain.NotificationSummaryEngine
import com.example.domain.ScreenContextSnapshot
import com.example.domain.SystemPermissionStatus
import com.example.domain.UsageAnalyticsManager
import com.example.service.AegisAccessibilityService
import com.example.service.AegisNotificationListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AegisTab {
    COMMAND,
    ANALYTICS,
    RULES,
    AI_GUARD,
    STUDIO
}

data class UsageScrollSummary(
    val totalScreenMinutes: Int = 0,
    val totalScrollMinutes: Int = 0,
    val wastedMinutes: Int = 0,
    val productiveMinutes: Int = 0,
    val totalScrollMeters: Float = 0f,
    val totalScrollSwipes: Int = 0,
    val focusHealthScore: Int = 82
)

class AegisViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = AegisRepository.getInstance(appContext)

    private val _currentTab = MutableStateFlow(AegisTab.COMMAND)
    val currentTab: StateFlow<AegisTab> = _currentTab.asStateFlow()

    private val _permissionStatus = MutableStateFlow(
        UsageAnalyticsManager.checkPermissions(appContext)
    )
    val permissionStatus: StateFlow<SystemPermissionStatus> = _permissionStatus.asStateFlow()

    private val _installedBrowsers = MutableStateFlow<List<InstalledBrowserInfo>>(emptyList())
    val installedBrowsers: StateFlow<List<InstalledBrowserInfo>> = _installedBrowsers.asStateFlow()

    private val _discoveredEdgeModels = MutableStateFlow<List<String>>(emptyList())
    val discoveredEdgeModels: StateFlow<List<String>> = _discoveredEdgeModels.asStateFlow()

    private val _isSummarizingNotifications = MutableStateFlow(false)
    val isSummarizingNotifications: StateFlow<Boolean> = _isSummarizingNotifications.asStateFlow()

    val settingsState: StateFlow<AegisSettingsState> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AegisSettingsState()
    )

    val allRules: StateFlow<List<BlockRuleEntity>> = repository.allRules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentLogs: StateFlow<List<InterceptionLogEntity>> = repository.recentLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalInterceptionsCount: StateFlow<Int> = repository.totalInterceptionsCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val allTelemetry: StateFlow<List<ScrollTelemetryEntity>> = repository.allTelemetry.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val capturedNotifications: StateFlow<List<CapturedNotificationEntity>> =
        repository.capturedNotifications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notificationDigests: StateFlow<List<NotificationDigestEntity>> =
        repository.notificationDigests.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val usageScrollSummary: StateFlow<UsageScrollSummary> = allTelemetry.combine(totalInterceptionsCount) { list, blocks ->
        val totalUsageMs = list.sumOf { maxOf(it.foregroundUsageMillis, it.scrollDurationMillis) }
        val totalScrollMs = list.sumOf { it.scrollDurationMillis }
        val wastedMs = list.filter { it.isAvoidMarked || it.category == "WASTED_RISK" }
            .sumOf { maxOf(it.foregroundUsageMillis, it.scrollDurationMillis) }
        val productiveMs = (totalUsageMs - wastedMs).coerceAtLeast(0L)
        val totalMeters = list.sumOf { it.estimatedScrollMeters.toDouble() }.toFloat()
        val totalSwipes = list.sumOf { it.scrollEventsCount }

        val totalMins = (totalUsageMs / 60_000L).toInt()
        val wastedMins = (wastedMs / 60_000L).toInt()
        val rawScore = if (totalMins > 0) {
            100 - ((wastedMins * 75) / totalMins) + (blocks.coerceAtMost(15))
        } else {
            88
        }

        UsageScrollSummary(
            totalScreenMinutes = totalMins,
            totalScrollMinutes = (totalScrollMs / 60_000L).toInt(),
            wastedMinutes = wastedMins,
            productiveMinutes = (productiveMs / 60_000L).toInt(),
            totalScrollMeters = totalMeters,
            totalScrollSwipes = totalSwipes,
            focusHealthScore = rawScore.coerceIn(12, 99)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UsageScrollSummary()
    )

    val latestScreenContext: StateFlow<ScreenContextSnapshot> = AegisAccessibilityService.latestScreenContext

    private val _activeSimulatedBlockDecision = MutableStateFlow<InterceptionDecision?>(null)
    val activeSimulatedBlockDecision: StateFlow<InterceptionDecision?> = _activeSimulatedBlockDecision.asStateFlow()

    private val _activeSnoozes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val activeSnoozes: StateFlow<Map<String, Int>> = _activeSnoozes.asStateFlow()

    private val _clockTickEpochMillis = MutableStateFlow(System.currentTimeMillis())
    val clockTickEpochMillis: StateFlow<Long> = _clockTickEpochMillis.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    init {
        refreshSystemState()
        startLiveTelemetryTicker()
    }

    private fun startLiveTelemetryTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _clockTickEpochMillis.value = System.currentTimeMillis()
                _activeSnoozes.value = BlockingEngine.getActiveSnoozes()
            }
        }
    }

    fun selectTab(tab: AegisTab) {
        _currentTab.value = tab
    }

    fun dismissBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun showBannerMessage(message: String) {
        _statusBannerMessage.value = message
    }

    fun refreshSystemState() {
        viewModelScope.launch(Dispatchers.IO) {
            _permissionStatus.value = UsageAnalyticsManager.checkPermissions(appContext)
            _installedBrowsers.value = UsageAnalyticsManager.detectInstalledBrowsers(appContext)
            _discoveredEdgeModels.value = NotificationSummaryEngine.scanLocalEdgeGalleryModels(appContext)
            val usageMap = UsageAnalyticsManager.queryLast24HoursDeviceUsage(appContext)
            if (usageMap.isNotEmpty()) {
                repository.syncDeviceUsageStats(usageMap)
            }
        }
    }

    // --- Blocker Feature Toggles (Protected by Strict Mode) ---
    fun toggleWebsiteKeywordBlocker(enabled: Boolean) {
        if (!enabled && settingsState.value.isStrictCurrentlyLocked) {
            showBannerMessage("Strict Mode Lock is active: Website & Keyword Blocker cannot be turned off.")
            return
        }
        viewModelScope.launch {
            repository.settingsDataStore.setWebsiteKeywordBlocker(enabled)
        }
    }

    fun toggleIncognitoBlocker(enabled: Boolean) {
        if (!enabled && settingsState.value.isStrictCurrentlyLocked) {
            showBannerMessage("Strict Mode Lock is active: Incognito Mode Blocker cannot be disabled.")
            return
        }
        viewModelScope.launch {
            repository.settingsDataStore.setIncognitoBlocker(enabled)
        }
    }

    fun toggleYoutubeShortsBlocker(enabled: Boolean) {
        if (!enabled && settingsState.value.isStrictCurrentlyLocked) {
            showBannerMessage("Strict Mode Lock is active: YouTube Shorts Blocker cannot be disabled.")
            return
        }
        viewModelScope.launch {
            repository.settingsDataStore.setYoutubeShortsBlocker(enabled)
        }
    }

    fun toggleInstagramReelsBlocker(enabled: Boolean) {
        if (!enabled && settingsState.value.isStrictCurrentlyLocked) {
            showBannerMessage("Strict Mode Lock is active: Instagram Reels Blocker cannot be disabled.")
            return
        }
        viewModelScope.launch {
            repository.settingsDataStore.setInstagramReelsBlocker(enabled)
        }
    }

    fun toggleTiktokBlocker(enabled: Boolean) {
        if (!enabled && settingsState.value.isStrictCurrentlyLocked) {
            showBannerMessage("Strict Mode Lock is active: TikTok Blocker cannot be disabled.")
            return
        }
        viewModelScope.launch {
            repository.settingsDataStore.setTiktokBlocker(enabled)
        }
    }

    fun toggleFacebookReelsBlocker(enabled: Boolean) {
        if (!enabled && settingsState.value.isStrictCurrentlyLocked) {
            showBannerMessage("Strict Mode Lock is active: Facebook Reels Blocker cannot be disabled.")
            return
        }
        viewModelScope.launch {
            repository.settingsDataStore.setFacebookReelsBlocker(enabled)
        }
    }

    fun toggleAggressiveFuzzyMatching(enabled: Boolean) {
        if (!enabled && settingsState.value.isStrictCurrentlyLocked) {
            showBannerMessage("Strict Mode Lock is active: Anti-obfuscation matching cannot be weakened.")
            return
        }
        viewModelScope.launch {
            repository.settingsDataStore.setAggressiveFuzzyMatching(enabled)
        }
    }

    // --- Website & Keyword Rule Actions ---
    fun addBlockRule(pattern: String, ruleType: String, category: String) {
        if (pattern.isBlank()) return
        viewModelScope.launch {
            repository.addRule(pattern, ruleType, category)
            showBannerMessage("Added $ruleType rule: \"${pattern.trim()}\"")
        }
    }

    fun toggleRule(rule: BlockRuleEntity, enabled: Boolean) {
        if (!enabled && settingsState.value.isStrictCurrentlyLocked) {
            showBannerMessage("Strict Mode Lock is active: Rules cannot be disabled until timer completes.")
            return
        }
        viewModelScope.launch {
            repository.toggleRule(rule, enabled)
        }
    }

    fun deleteRule(rule: BlockRuleEntity) {
        if (settingsState.value.isStrictCurrentlyLocked) {
            showBannerMessage("Strict Mode Lock is active: Rules cannot be deleted during lock window.")
            return
        }
        viewModelScope.launch {
            repository.deleteRule(rule.id)
        }
    }

    fun addQuickPresetPack(category: String) {
        viewModelScope.launch {
            when (category) {
                "Adult/NSFW" -> {
                    repository.addRule("pornhub.com", "WEBSITE", "Adult/NSFW")
                    repository.addRule("xvideos.com", "WEBSITE", "Adult/NSFW")
                    repository.addRule("explicit content", "KEYWORD", "Adult/NSFW")
                    repository.addRule("adult video", "KEYWORD", "Adult/NSFW")
                }
                "Gambling" -> {
                    repository.addRule("draftkings.com", "WEBSITE", "Gambling")
                    repository.addRule("bet365.com", "WEBSITE", "Gambling")
                    repository.addRule("roulette spin", "KEYWORD", "Gambling")
                    repository.addRule("slot machine", "KEYWORD", "Gambling")
                }
                "Social" -> {
                    repository.addRule("facebook.com", "WEBSITE", "Social")
                    repository.addRule("threads.net", "WEBSITE", "Social")
                    repository.addRule("snapchat.com", "WEBSITE", "Social")
                }
                "Gaming" -> {
                    repository.addRule("twitch.tv", "WEBSITE", "Gaming")
                    repository.addRule("discord.com", "WEBSITE", "Gaming")
                    repository.addRule("live stream gaming", "KEYWORD", "Gaming")
                }
            }
            showBannerMessage("Activated $category protection pack across all browsers.")
        }
    }

    // --- Strict Mode Actions ---
    fun startStrictMode(durationMinutes: Int, preventTamper: Boolean, disableSnooze: Boolean) {
        viewModelScope.launch {
            repository.settingsDataStore.activateStrictMode(
                durationMinutes = durationMinutes,
                preventTamper = preventTamper,
                disableSnooze = disableSnooze
            )
            BlockingEngine.clearAllSnoozes()
            showBannerMessage("Strict Mode Locked for $durationMinutes minutes! No bypasses allowed.")
        }
    }

    fun emergencyUnlockStrictMode(enteredChallengeCode: String): Boolean {
        val expected = settingsState.value.defaultChallengeCode
        return if (enteredChallengeCode.trim().equals(expected.trim(), ignoreCase = true)) {
            viewModelScope.launch {
                repository.settingsDataStore.emergencyUnlockStrictModeWithCode()
                showBannerMessage("Strict Mode unlocked via Master Challenge Code. Stayed minutes recorded.")
            }
            true
        } else {
            false
        }
    }

    // --- Usage & Scroll Avoid List Actions ---
    fun togglePackageAvoidStatus(item: ScrollTelemetryEntity) {
        viewModelScope.launch {
            repository.setPackageAvoidStatus(item.packageName, !item.isAvoidMarked)
        }
    }

    fun logSimulatedScrollSession(packageName: String, appName: String, minutes: Int, meters: Float) {
        viewModelScope.launch {
            repository.recordScrollEvent(
                packageName = packageName,
                appName = appName,
                addedDurationMs = minutes * 60_000L,
                addedMeters = meters,
                isAvoidDefault = true
            )
            showBannerMessage("Logged +${minutes}m scroll (${meters.toInt()}m distance) for $appName")
        }
    }

    // --- AI Screen Context Guardian Actions ---
    fun updateAiGuardianSettings(
        enabled: Boolean,
        instructions: String,
        threshold: Int,
        autoBlock: Boolean
    ) {
        if (!enabled && settingsState.value.isStrictCurrentlyLocked) {
            showBannerMessage("Strict Mode Lock is active: AI Guardian cannot be turned off.")
            return
        }
        viewModelScope.launch {
            repository.settingsDataStore.updateAiGuardianSettings(
                enabled = enabled,
                instructions = instructions,
                threshold = threshold,
                autoBlock = autoBlock
            )
            showBannerMessage("Updated AI Screen Context Guardian rules.")
        }
    }

    fun evaluateSampleWithAi(sampleUrl: String, sampleScreenText: String): AiContextVerdict {
        val current = settingsState.value
        val snapshot = ScreenContextSnapshot(
            packageName = "com.android.chrome",
            appName = "Chrome Browser",
            detectedUrl = sampleUrl,
            visibleTexts = listOf(sampleScreenText)
        )
        return BlockingEngine.evaluateAiScreenContext(
            snapshot = snapshot,
            userInstructions = current.aiUserInstructions,
            threshold = current.aiSensitivityThreshold
        )
    }

    // --- Notification Summary & LLM Engine Actions ---
    fun toggleNotificationSummaryQuick(enabled: Boolean) {
        val s = settingsState.value
        viewModelScope.launch {
            repository.settingsDataStore.updateNotificationSummarySettings(
                enabled = enabled,
                autoPostPanel = s.autoPostSummaryToNotificationPanel,
                suppressPromos = s.suppressPromoNotificationsWhenSummarized,
                backend = s.llmBackend,
                onDeviceModelId = s.onDeviceModelId,
                customModelPath = s.customOnDeviceModelPath,
                customModelExt = s.customOnDeviceModelExtension,
                cloudProvider = s.cloudProviderName,
                cloudModel = s.cloudModelName,
                cloudEndpoint = s.cloudCustomEndpoint,
                cloudApiKey = s.cloudApiKeyInput
            )
            showBannerMessage(
                if (enabled) "Notification Summary enabled (${s.activeLlmDisplayBadge})."
                else "Notification Summary paused."
            )
        }
    }

    fun saveNotificationSummaryConfig(
        enabled: Boolean,
        autoPostPanel: Boolean,
        suppressPromos: Boolean,
        backend: LlmExecutionBackend,
        onDeviceModelId: String,
        customModelPath: String,
        cloudProvider: String,
        cloudModel: String,
        cloudEndpoint: String,
        cloudApiKey: String
    ) {
        val ext = NotificationSummaryEngine.extractModelExtension(
            customModelPath.ifBlank { onDeviceModelId }
        )
        viewModelScope.launch {
            repository.settingsDataStore.updateNotificationSummarySettings(
                enabled = enabled,
                autoPostPanel = autoPostPanel,
                suppressPromos = suppressPromos,
                backend = backend,
                onDeviceModelId = onDeviceModelId,
                customModelPath = customModelPath,
                customModelExt = ext,
                cloudProvider = cloudProvider,
                cloudModel = cloudModel,
                cloudEndpoint = cloudEndpoint,
                cloudApiKey = cloudApiKey
            )
            showBannerMessage("Saved Notification Summary & LLM configuration.")
        }
    }

    /**
     * Summarizes all captured/active notifications at once using the configured On-Device Gemma 4
     * (.light, .rtlm, .guf, .litertlm) or Cloud LLM API key, and posts the consolidated summary
     * directly to the Android Notification Panel.
     */
    fun summarizeAllNotificationsAndPostToPanel() {
        viewModelScope.launch {
            _isSummarizingNotifications.value = true
            try {
                // Pull any live status bar notifications if the listener service is connected
                AegisNotificationListenerService.captureActiveStatusBarNotifications()
                val currentBatch = repository.getRecentCapturedNotificationsSnapshot()
                val settings = settingsState.value

                val digest = NotificationSummaryEngine.summarizeAllNotificationsAtOnce(
                    notifications = currentBatch,
                    settings = settings
                )
                repository.saveNotificationDigest(digest)

                val posted = NotificationSummaryEngine.postSummaryToNotificationPanel(
                    context = appContext,
                    digest = digest
                )
                refreshSystemState()
                if (posted) {
                    showBannerMessage("Summarized ${digest.totalCount} notifications & posted to Notification Panel!")
                } else {
                    showBannerMessage("Summary generated (${digest.totalCount} items). Grant Notification permission to display in panel.")
                }
            } finally {
                _isSummarizingNotifications.value = false
            }
        }
    }

    fun simulateIncomingNotificationBatchAndSummarize() {
        viewModelScope.launch {
            val samples = listOf(
                Triple(
                    "com.github.android" to "GitHub",
                    "PR #502: Merge Request Approved",
                    "Alex approved your pull request 'Add Gemma 4 .rtlm notification summarizer'. Ready to merge."
                ),
                Triple(
                    "com.google.android.calendar" to "Calendar",
                    "Product Architecture Sync in 15 mins",
                    "Room 4B / Google Meet — Agenda: Q4 Digital Wellbeing roadmap."
                ),
                Triple(
                    "org.telegram.messenger" to "Telegram",
                    "Dev Team Channel",
                    "Build 2.4 APK passed all automated regression tests."
                ),
                Triple(
                    "com.zhiliaoapp.musically" to "TikTok",
                    "Trending Viral Dance Challenge",
                    "Watch 20 new viral clips waiting in your feed right now!"
                )
            )
            for ((pkgPair, title, body) in samples) {
                val (pkg, appName) = pkgPair
                val bucket = NotificationSummaryEngine.classifyNotificationPriority(pkg, title, body)
                repository.insertCapturedNotification(
                    packageName = pkg,
                    appName = appName,
                    title = title,
                    content = body,
                    priorityBucket = bucket
                )
            }
            summarizeAllNotificationsAndPostToPanel()
        }
    }

    fun clearCapturedNotifications() {
        viewModelScope.launch {
            repository.clearCapturedNotifications()
            showBannerMessage("Cleared captured notification queue.")
        }
    }

    // --- Editable Block Screen Studio Actions ---
    fun saveBlockScreenConfiguration(
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
        viewModelScope.launch {
            repository.settingsDataStore.updateBlockScreenConfig(
                mode = mode,
                challengeCode = challengeCode,
                useRandomCode = useRandomCode,
                headline = headline,
                message = message,
                customImageUri = customImageUri,
                customVideoUri = customVideoUri,
                redirectUrl = redirectUrl,
                autoRedirect = autoRedirect
            )
            showBannerMessage("Saved Editable Block Screen configuration (${mode.name}).")
        }
    }

    // --- Trigger Live Block Screen Overlay / Simulation ---
    fun triggerLiveBlockScreenTest(
        triggerType: String = "WEBSITE",
        matchedLabel: String = "Blocked Website: tiktok.com",
        appName: String = "Google Chrome",
        snippet: String = "https://www.tiktok.com/@trending/video/9812412",
        aiReasoning: String = ""
    ) {
        val s = settingsState.value
        val decision = InterceptionDecision(
            shouldBlock = true,
            triggerType = triggerType,
            matchedRuleLabel = matchedLabel,
            packageName = "com.android.chrome",
            appName = appName,
            snippet = snippet,
            aiReasoning = aiReasoning,
            challengeCodeRequired = s.defaultChallengeCode
        )
        _activeSimulatedBlockDecision.value = decision

        viewModelScope.launch(Dispatchers.IO) {
            repository.recordInterception(
                triggerType = triggerType,
                matchedRule = matchedLabel,
                packageName = "com.android.chrome",
                appName = appName,
                screenSnippet = snippet,
                aiReasoning = aiReasoning,
                actionTaken = "BLOCKED_OVERLAY"
            )
        }
    }

    fun dismissSimulatedBlockScreen() {
        _activeSimulatedBlockDecision.value = null
    }

    fun grantFiveMinSnoozeFromSimulation(decision: InterceptionDecision) {
        BlockingEngine.grantFiveMinuteAccess(decision.packageName.ifBlank { "com.android.chrome" })
        _activeSnoozes.value = BlockingEngine.getActiveSnoozes()
        _activeSimulatedBlockDecision.value = null
        showBannerMessage("Unlocked 5-Minute Temporary Access. Auto-locks again when timer expires.")
    }

    fun openExternalRedirectUrl(rawUrl: String) {
        val formatted = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
        _activeSimulatedBlockDecision.value = null
    }

    fun clearAllLogs() {
        if (settingsState.value.isStrictCurrentlyLocked) {
            showBannerMessage("Strict Mode Lock is active: Interception logs cannot be cleared.")
            return
        }
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}
