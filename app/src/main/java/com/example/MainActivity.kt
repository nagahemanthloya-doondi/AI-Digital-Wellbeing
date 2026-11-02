package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AegisTab
import com.example.ui.AegisViewModel
import com.example.ui.overlay.BlockScreenOverlayContent
import com.example.ui.screens.AiContextGuardianScreen
import com.example.ui.screens.BlockScreenStudioScreen
import com.example.ui.screens.CommandDashboardScreen
import com.example.ui.screens.RulesAndBlockersScreen
import com.example.ui.screens.UsageScrollAnalyticsScreen
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmeraldBright
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.StrictAmberBright

class MainActivity : ComponentActivity() {

    private val viewModel: AegisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                AegisControlApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AegisControlApp(viewModel: AegisViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val rules by viewModel.allRules.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val totalInterceptions by viewModel.totalInterceptionsCount.collectAsStateWithLifecycle()
    val telemetryList by viewModel.allTelemetry.collectAsStateWithLifecycle()
    val summary by viewModel.usageScrollSummary.collectAsStateWithLifecycle()
    val installedBrowsers by viewModel.installedBrowsers.collectAsStateWithLifecycle()
    val capturedNotifications by viewModel.capturedNotifications.collectAsStateWithLifecycle()
    val notificationDigests by viewModel.notificationDigests.collectAsStateWithLifecycle()
    val discoveredEdgeModels by viewModel.discoveredEdgeModels.collectAsStateWithLifecycle()
    val isSummarizing by viewModel.isSummarizingNotifications.collectAsStateWithLifecycle()
    val latestScreenContext by viewModel.latestScreenContext.collectAsStateWithLifecycle()
    val activeSimulatedDecision by viewModel.activeSimulatedBlockDecision.collectAsStateWithLifecycle()
    val activeSnoozes by viewModel.activeSnoozes.collectAsStateWithLifecycle()
    val clockTick by viewModel.clockTickEpochMillis.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    BackHandler(enabled = currentTab != AegisTab.COMMAND && activeSimulatedDecision == null) {
        viewModel.selectTab(AegisTab.COMMAND)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Scaffold(
            containerColor = ObsidianBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianBg,
                        titleContentColor = Color.White
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Aegis Logo",
                                tint = AegisEmeraldBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Aegis Control",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Digital Wellbeing & Self-Governing Engine",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AegisCyan
                                )
                            }
                        }
                    },
                    actions = {
                        if (settings.isStrictCurrentlyLocked) {
                            Surface(
                                color = StrictAmberBright.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Strict Lock",
                                        tint = StrictAmberBright,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "STRICT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StrictAmberBright,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = ObsidianSurface,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        Triple(AegisTab.COMMAND, "Command", Icons.Default.Shield),
                        Triple(AegisTab.ANALYTICS, "Usage", Icons.Default.Analytics),
                        Triple(AegisTab.RULES, "Blockers", Icons.Default.GppBad),
                        Triple(AegisTab.AI_GUARD, "AI & Digest", Icons.Default.AutoAwesome),
                        Triple(AegisTab.STUDIO, "Block UI", Icons.Default.DashboardCustomize)
                    )
                    items.forEach { (tab, label, icon) ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF022C22),
                                selectedTextColor = AegisEmeraldBright,
                                indicatorColor = AegisEmeraldBright,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    AegisTab.COMMAND -> {
                        CommandDashboardScreen(
                            permissionStatus = permissionStatus,
                            settings = settings,
                            summary = summary,
                            totalInterceptions = totalInterceptions,
                            recentLogs = recentLogs,
                            activeSnoozes = activeSnoozes,
                            clockTick = clockTick,
                            onRefreshPermissions = { viewModel.refreshSystemState() },
                            onToggleWebsiteBlocker = { viewModel.toggleWebsiteKeywordBlocker(it) },
                            onToggleIncognitoBlocker = { viewModel.toggleIncognitoBlocker(it) },
                            onToggleYoutubeShorts = { viewModel.toggleYoutubeShortsBlocker(it) },
                            onToggleInstagramReels = { viewModel.toggleInstagramReelsBlocker(it) },
                            onToggleAiGuardian = {
                                viewModel.updateAiGuardianSettings(
                                    enabled = it,
                                    instructions = settings.aiUserInstructions,
                                    threshold = settings.aiSensitivityThreshold,
                                    autoBlock = settings.aiAutoBlockOnVerdict
                                )
                            },
                            onToggleNotificationSummary = {
                                viewModel.toggleNotificationSummaryQuick(it)
                            },
                            onTriggerNotificationSummaryNow = {
                                viewModel.summarizeAllNotificationsAndPostToPanel()
                            },
                            onStartStrictMode = { mins, tamper, snooze ->
                                viewModel.startStrictMode(mins, tamper, snooze)
                            },
                            onEmergencyUnlockStrict = { code ->
                                viewModel.emergencyUnlockStrictMode(code)
                            },
                            onSimulateLiveBlock = { type, label, app, snippet ->
                                viewModel.triggerLiveBlockScreenTest(type, label, app, snippet)
                            },
                            onClearLogs = { viewModel.clearAllLogs() }
                        )
                    }

                    AegisTab.ANALYTICS -> {
                        UsageScrollAnalyticsScreen(
                            permissionStatus = permissionStatus,
                            summary = summary,
                            telemetryList = telemetryList,
                            onSyncDeviceUsage = { viewModel.refreshSystemState() },
                            onToggleAvoidStatus = { viewModel.togglePackageAvoidStatus(it) },
                            onLogSampleScroll = { pkg, name, mins, meters ->
                                viewModel.logSimulatedScrollSession(pkg, name, mins, meters)
                            }
                        )
                    }

                    AegisTab.RULES -> {
                        RulesAndBlockersScreen(
                            settings = settings,
                            rules = rules,
                            installedBrowsers = installedBrowsers,
                            onAddRule = { pattern, type, cat ->
                                viewModel.addBlockRule(pattern, type, cat)
                            },
                            onToggleRule = { rule, enabled ->
                                viewModel.toggleRule(rule, enabled)
                            },
                            onDeleteRule = { rule ->
                                viewModel.deleteRule(rule)
                            },
                            onAddPresetPack = { pack ->
                                viewModel.addQuickPresetPack(pack)
                            },
                            onToggleIncognito = { viewModel.toggleIncognitoBlocker(it) },
                            onToggleYoutubeShorts = { viewModel.toggleYoutubeShortsBlocker(it) },
                            onToggleInstagramReels = { viewModel.toggleInstagramReelsBlocker(it) },
                            onToggleTiktok = { viewModel.toggleTiktokBlocker(it) },
                            onToggleFacebookReels = { viewModel.toggleFacebookReelsBlocker(it) },
                            onToggleFuzzyMatching = { viewModel.toggleAggressiveFuzzyMatching(it) },
                            onTriggerBlockOverlay = { type, label, app, snippet ->
                                viewModel.triggerLiveBlockScreenTest(type, label, app, snippet)
                            }
                        )
                    }

                    AegisTab.AI_GUARD -> {
                        AiContextGuardianScreen(
                            permissionStatus = permissionStatus,
                            settings = settings,
                            capturedNotifications = capturedNotifications,
                            notificationDigests = notificationDigests,
                            discoveredEdgeModels = discoveredEdgeModels,
                            isSummarizing = isSummarizing,
                            latestLiveSnapshot = latestScreenContext,
                            onSaveNotificationSummaryConfig = { enabled, autoPanel, suppress, backend, modelId, customPath, provider, model, endpoint, apiKey ->
                                viewModel.saveNotificationSummaryConfig(
                                    enabled = enabled,
                                    autoPostPanel = autoPanel,
                                    suppressPromos = suppress,
                                    backend = backend,
                                    onDeviceModelId = modelId,
                                    customModelPath = customPath,
                                    cloudProvider = provider,
                                    cloudModel = model,
                                    cloudEndpoint = endpoint,
                                    cloudApiKey = apiKey
                                )
                            },
                            onSummarizeAllAndPostNow = {
                                viewModel.summarizeAllNotificationsAndPostToPanel()
                            },
                            onSimulateIncomingBatch = {
                                viewModel.simulateIncomingNotificationBatchAndSummarize()
                            },
                            onClearCapturedNotifications = {
                                viewModel.clearCapturedNotifications()
                            },
                            onRefreshPermissions = {
                                viewModel.refreshSystemState()
                            },
                            onSaveAiSettings = { enabled, instr, thresh, auto ->
                                viewModel.updateAiGuardianSettings(enabled, instr, thresh, auto)
                            },
                            onEvaluateSample = { url, text ->
                                viewModel.evaluateSampleWithAi(url, text)
                            },
                            onTriggerAiBlockOverlay = { verdict, snippet ->
                                viewModel.triggerLiveBlockScreenTest(
                                    triggerType = "AI_CONTEXT",
                                    matchedLabel = "AI Guardian (${verdict.distractionScore}% Risk • ${verdict.detectedCategory})",
                                    appName = "Chrome Browser",
                                    snippet = snippet,
                                    aiReasoning = verdict.reasoning
                                )
                            }
                        )
                    }

                    AegisTab.STUDIO -> {
                        BlockScreenStudioScreen(
                            settings = settings,
                            onSaveStudioConfig = { mode, code, rand, head, msg, img, vid, redir, autoRedir ->
                                viewModel.saveBlockScreenConfiguration(
                                    mode = mode,
                                    challengeCode = code,
                                    useRandomCode = rand,
                                    headline = head,
                                    message = msg,
                                    customImageUri = img,
                                    customVideoUri = vid,
                                    redirectUrl = redir,
                                    autoRedirect = autoRedir
                                )
                            },
                            onPreviewFullScreenBlock = {
                                viewModel.triggerLiveBlockScreenTest(
                                    triggerType = "STUDIO_PREVIEW",
                                    matchedLabel = "Custom Block Studio Live Preview",
                                    appName = "Google Chrome",
                                    snippet = "Testing your editable Block Screen layout & zero-loophole controls"
                                )
                            }
                        )
                    }
                }

                if (bannerMessage != null) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.dismissBannerMessage() }) {
                                Text("OK", color = AegisEmeraldBright)
                            }
                        }
                    ) {
                        Text(bannerMessage!!)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = activeSimulatedDecision != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            activeSimulatedDecision?.let { decision ->
                BlockScreenOverlayContent(
                    decision = decision,
                    settings = settings,
                    onGoBackToSafety = {
                        viewModel.dismissSimulatedBlockScreen()
                    },
                    onGrantFiveMinAccess = {
                        viewModel.grantFiveMinSnoozeFromSimulation(decision)
                    },
                    onTriggerRedirectUrl = { url ->
                        viewModel.openExternalRedirectUrl(url)
                    },
                    isPreviewMode = decision.triggerType == "STUDIO_PREVIEW"
                )
            }
        }
    }
}
