package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.VideoView
import com.example.R
import com.example.data.AegisRepository
import com.example.data.local.BlockRuleEntity
import com.example.data.preferences.AegisSettingsState
import com.example.data.preferences.BlockScreenVisualMode
import com.example.domain.BlockingEngine
import com.example.domain.InterceptionDecision
import com.example.domain.ScreenContextSnapshot
import com.example.ui.overlay.BlockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AegisAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: AegisRepository

    @Volatile
    private var cachedSettings: AegisSettingsState = AegisSettingsState()

    @Volatile
    private var cachedRules: List<BlockRuleEntity> = emptyList()

    private var lastEvaluatedTimestamp: Long = 0L
    private var lastScrollEventTimestamp: Long = 0L
    private var lastBlockedTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceRunning.value = true

        repository = AegisRepository.getInstance(applicationContext)

        serviceScope.launch(Dispatchers.IO) {
            repository.settingsFlow.collectLatest { state ->
                cachedSettings = state
            }
        }
        serviceScope.launch(Dispatchers.IO) {
            repository.allRules.collectLatest { rules ->
                cachedRules = rules.filter { it.isEnabled }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        // Ignore our own package UI so user can configure Aegis Control freely
        if (pkg == applicationContext.packageName) return

        val now = System.currentTimeMillis()

        // 1. Track Real Scroll Telemetry when TYPE_VIEW_SCROLLED occurs
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (now - lastScrollEventTimestamp > 350L) {
                lastScrollEventTimestamp = now
                val appLabel = resolveAppLabel(pkg)
                val isAvoidDefault = pkg.contains("youtube") ||
                    pkg.contains("instagram") ||
                    pkg.contains("tiktok") ||
                    pkg.contains("musically") ||
                    pkg.contains("facebook") ||
                    pkg.contains("reddit") ||
                    pkg.contains("twitter")
                serviceScope.launch(Dispatchers.IO) {
                    repository.recordScrollEvent(
                        packageName = pkg,
                        appName = appLabel,
                        addedDurationMs = 1_800L,
                        addedMeters = 0.38f,
                        isAvoidDefault = isAvoidDefault
                    )
                }
            }
        }

        // Throttle full hierarchy evaluation to once every 180ms for peak smoothness
        if (now - lastEvaluatedTimestamp < 180L &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }
        lastEvaluatedTimestamp = now

        val rootNode = rootInActiveWindow ?: event.source ?: return
        val snapshot = extractScreenContextSnapshot(
            root = rootNode,
            packageName = pkg,
            windowClass = event.className?.toString().orEmpty()
        )

        // Update live screen telemetry for the AI Screen Context Guardian inspector
        _latestScreenContext.value = snapshot

        val decision = BlockingEngine.evaluateScreenContext(
            snapshot = snapshot,
            settings = cachedSettings,
            enabledRules = cachedRules
        )

        if (decision.shouldBlock && now - lastBlockedTimestamp > 1_200L) {
            lastBlockedTimestamp = now
            enforceBlockInterception(decision)
        }
    }

    private fun enforceBlockInterception(decision: InterceptionDecision) {
        triggerTactileAlert()

        // Immediately back out of Incognito or Reels/Shorts surface inside the target app
        if (decision.triggerType == "INCOGNITO" || decision.triggerType == "REELS_SHORTS") {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }

        // Log interception in Room database
        serviceScope.launch(Dispatchers.IO) {
            val actionLabel = if (cachedSettings.blockScreenMode == BlockScreenVisualMode.REDIRECT_URL &&
                cachedSettings.autoRedirectImmediately
            ) {
                "REDIRECTED"
            } else {
                "BLOCKED_OVERLAY"
            }
            repository.recordInterception(
                triggerType = decision.triggerType,
                matchedRule = decision.matchedRuleLabel,
                packageName = decision.packageName,
                appName = decision.appName,
                screenSnippet = decision.snippet,
                aiReasoning = decision.aiReasoning,
                actionTaken = actionLabel,
                ruleIdToIncrement = decision.matchedRuleId
            )
        }

        // 1. If Display Over Other Apps (SYSTEM_ALERT_WINDOW) is granted, show un-bypassable TYPE_APPLICATION_OVERLAY window
        if (Settings.canDrawOverlays(applicationContext)) {
            showSystemOverlayWindow(
                context = applicationContext,
                decision = decision,
                settings = cachedSettings
            )
        }

        // 2. Also launch BlockOverlayActivity as full-screen Compose lock shield
        val overlayIntent = BlockOverlayActivity.createLaunchIntent(applicationContext, decision)
        runCatching { startActivity(overlayIntent) }
    }

    private fun extractScreenContextSnapshot(
        root: AccessibilityNodeInfo,
        packageName: String,
        windowClass: String
    ): ScreenContextSnapshot {
        val visibleTexts = ArrayList<String>(40)
        val viewIds = ArrayList<String>(30)
        var detectedUrl = ""

        fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 14 || visibleTexts.size >= 45) return

            val viewId = node.viewIdResourceName
            if (!viewId.isNullOrBlank()) {
                viewIds.add(viewId)
                if (detectedUrl.isEmpty() && BlockingEngine.isKnownUrlBarViewId(viewId)) {
                    val candidateUrl = node.text?.toString()?.trim().orEmpty()
                    if (candidateUrl.isNotBlank()) {
                        detectedUrl = candidateUrl
                    }
                }
            }

            val text = node.text?.toString()?.trim()
            if (!text.isNullOrBlank() && text.length <= 300) {
                visibleTexts.add(text)
                if (detectedUrl.isEmpty() && looksLikeUrl(text)) {
                    detectedUrl = text
                }
            }

            val desc = node.contentDescription?.toString()?.trim()
            if (!desc.isNullOrBlank() && desc.length <= 200) {
                visibleTexts.add(desc)
            }

            val childCount = node.childCount.coerceAtMost(25)
            for (i in 0 until childCount) {
                traverse(node.getChild(i), depth + 1)
            }
        }

        runCatching { traverse(root, 0) }

        return ScreenContextSnapshot(
            packageName = packageName,
            appName = resolveAppLabel(packageName),
            windowClassName = windowClass,
            detectedUrl = detectedUrl,
            visibleTexts = visibleTexts,
            viewIdResources = viewIds
        )
    }

    private fun looksLikeUrl(text: String): Boolean {
        val clean = text.trim().lowercase()
        if (clean.contains(" ") || clean.length < 4) return false
        return clean.startsWith("http://") ||
            clean.startsWith("https://") ||
            clean.contains(".com") ||
            clean.contains(".org") ||
            clean.contains(".net") ||
            clean.contains(".io") ||
            clean.contains(".co")
    }

    private fun resolveAppLabel(packageName: String): String {
        BlockingEngine.KNOWN_BROWSER_PACKAGES[packageName]?.let { return it }
        return runCatching {
            val pm = applicationContext.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })
    }

    private fun triggerTactileAlert() {
        runCatching {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(120L)
            }
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissSystemOverlayWindow()
        _isServiceRunning.value = false
        instance = null
        serviceScope.cancel()
    }

    companion object {
        @Volatile
        private var instance: AegisAccessibilityService? = null

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _latestScreenContext = MutableStateFlow(
            ScreenContextSnapshot(
                packageName = "com.android.chrome",
                appName = "Google Chrome",
                detectedUrl = "https://www.youtube.com/shorts/viral_feed",
                visibleTexts = listOf(
                    "YouTube Shorts",
                    "Trending viral celebrity gossip compilation #shorts #fyp",
                    "Watch till the end!"
                ),
                viewIdResources = listOf("com.android.chrome:id/url_bar", "reel_player_page_container")
            )
        )
        val latestScreenContext: StateFlow<ScreenContextSnapshot> = _latestScreenContext.asStateFlow()

        private var activeOverlayView: View? = null
        private var activeWindowManager: WindowManager? = null

        fun performReturnToSafety(context: Context) {
            dismissSystemOverlayWindow()
            val svc = instance
            if (svc != null) {
                svc.performGlobalAction(GLOBAL_ACTION_BACK)
                svc.performGlobalAction(GLOBAL_ACTION_HOME)
            } else {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching { context.startActivity(homeIntent) }
            }
        }

        fun dismissSystemOverlayWindow() {
            val view = activeOverlayView ?: return
            val wm = activeWindowManager
            runCatching {
                wm?.removeViewImmediate(view)
            }
            activeOverlayView = null
        }

        /**
         * Displays a direct WindowManager TYPE_APPLICATION_OVERLAY full-screen view over other apps
         * when SYSTEM_ALERT_WINDOW permission is granted, intercepting Back keys with zero loopholes.
         */
        fun showSystemOverlayWindow(
            context: Context,
            decision: InterceptionDecision,
            settings: AegisSettingsState
        ) {
            if (!Settings.canDrawOverlays(context)) return
            dismissSystemOverlayWindow()

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            activeWindowManager = wm

            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            // Custom FrameLayout that consumes KEYCODE_BACK so the user cannot bypass via Back key
            val rootFrame = object : FrameLayout(context) {
                override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                    if (event?.keyCode == KeyEvent.KEYCODE_BACK) {
                        return true // Consume back button completely
                    }
                    return super.dispatchKeyEvent(event)
                }
            }.apply {
                setBackgroundColor(Color.parseColor("#F5070C17"))
                isFocusableInTouchMode = true
                requestFocus()
            }

            val scroll = ScrollView(context).apply {
                isFillViewport = true
            }

            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(56, 96, 56, 80)
            }

            val badgeView = TextView(context).apply {
                text = "AEGIS OVERLAY LOCK • ${decision.triggerType}"
                setTextColor(Color.parseColor("#FFCDD2"))
                textSize = 12f
                typeface = Typeface.MONOSPACE
                setPadding(36, 16, 36, 16)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 50f
                    setColor(Color.parseColor("#33F43F5E"))
                    setStroke(2, Color.parseColor("#88F43F5E"))
                }
            }
            contentLayout.addView(badgeView)

            val titleView = TextView(context).apply {
                text = settings.blockHeadline
                setTextColor(Color.WHITE)
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, 36, 0, 12)
            }
            contentLayout.addView(titleView)

            val subtitleView = TextView(context).apply {
                text = "${decision.matchedRuleLabel}\n${settings.blockMessage}"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 36)
            }
            contentLayout.addView(subtitleView)

            // Editable Visual Centerpiece in System Overlay
            when (settings.blockScreenMode) {
                BlockScreenVisualMode.CUSTOM_VIDEO -> {
                    if (settings.customVideoUri.isNotBlank()) {
                        val vv = VideoView(context).apply {
                            setVideoURI(Uri.parse(settings.customVideoUri))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                        }
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            480
                        ).apply { bottomMargin = 36 }
                        contentLayout.addView(vv, lp)
                    }
                }
                BlockScreenVisualMode.CUSTOM_IMAGE -> {
                    val iv = ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        if (settings.customImageUri.startsWith("content://")) {
                            runCatching { setImageURI(Uri.parse(settings.customImageUri)) }
                                .onFailure { setImageResource(R.drawable.img_focus_guardian_1793632266911) }
                        } else {
                            setImageResource(R.drawable.img_focus_guardian_1793632266911)
                        }
                    }
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        460
                    ).apply { bottomMargin = 36 }
                    contentLayout.addView(iv, lp)
                }
                else -> {}
            }

            val requiredCode = decision.challengeCodeRequired.ifBlank { settings.defaultChallengeCode }

            // Go Back to Safety Button
            val goBackBtn = Button(context).apply {
                text = "Go Back to Safety"
                isAllCaps = false
                textSize = 16f
                setTextColor(Color.parseColor("#022C22"))
                background = GradientDrawable().apply {
                    cornerRadius = 36f
                    setColor(Color.parseColor("#34D399"))
                }
                setOnClickListener {
                    performReturnToSafety(context)
                }
            }
            val btnParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                140
            ).apply { bottomMargin = 28 }
            contentLayout.addView(goBackBtn, btnParams)

            // Optional Redirect Button
            if (settings.redirectUrl.isNotBlank()) {
                val redirectBtn = Button(context).apply {
                    text = "Redirect to ${settings.redirectUrl.removePrefix("https://").take(26)}"
                    isAllCaps = false
                    setTextColor(Color.parseColor("#06B6D4"))
                    background = GradientDrawable().apply {
                        cornerRadius = 36f
                        setColor(Color.parseColor("#16223B"))
                        setStroke(3, Color.parseColor("#06B6D4"))
                    }
                    setOnClickListener {
                        dismissSystemOverlayWindow()
                        val url = if (settings.redirectUrl.startsWith("http")) {
                            settings.redirectUrl
                        } else {
                            "https://${settings.redirectUrl}"
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(intent) }
                    }
                }
                contentLayout.addView(redirectBtn, btnParams)
            }

            // 5-Minute Challenge Code Box (Unless Strict Mode Disabled Snooze)
            if (!(settings.isStrictCurrentlyLocked && settings.strictModeDisableFiveMinAccess)) {
                val codeLabel = TextView(context).apply {
                    text = "To Access for 5 Minutes, enter code:\n$requiredCode"
                    setTextColor(Color.parseColor("#FBBF24"))
                    typeface = Typeface.MONOSPACE
                    gravity = Gravity.CENTER
                    setPadding(24, 24, 24, 16)
                }
                contentLayout.addView(codeLabel)

                val codeInput = EditText(context).apply {
                    hint = "Type exact challenge code"
                    setHintTextColor(Color.parseColor("#64748B"))
                    setTextColor(Color.WHITE)
                    inputType = InputType.TYPE_CLASS_TEXT
                    setPadding(32, 28, 32, 28)
                    background = GradientDrawable().apply {
                        cornerRadius = 24f
                        setColor(Color.parseColor("#0E172A"))
                        setStroke(2, Color.parseColor("#334155"))
                    }
                }
                val inputParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 24 }
                contentLayout.addView(codeInput, inputParams)

                val unlock5mBtn = Button(context).apply {
                    text = "Access for 5 Minutes"
                    isAllCaps = false
                    setTextColor(Color.parseColor("#1E1300"))
                    background = GradientDrawable().apply {
                        cornerRadius = 36f
                        setColor(Color.parseColor("#FBBF24"))
                    }
                    setOnClickListener {
                        val entered = codeInput.text?.toString()?.trim().orEmpty()
                        if (entered.equals(requiredCode.trim(), ignoreCase = true)) {
                            BlockingEngine.grantFiveMinuteAccess(decision.packageName)
                            decision.matchedRuleId?.let { BlockingEngine.grantFiveMinuteAccess("rule:$it") }
                            dismissSystemOverlayWindow()
                        } else {
                            codeInput.error = "Exact challenge code required"
                        }
                    }
                }
                contentLayout.addView(unlock5mBtn, btnParams)
            }

            scroll.addView(contentLayout)
            rootFrame.addView(scroll)

            runCatching {
                wm.addView(rootFrame, params)
                activeOverlayView = rootFrame
            }
        }
    }
}
