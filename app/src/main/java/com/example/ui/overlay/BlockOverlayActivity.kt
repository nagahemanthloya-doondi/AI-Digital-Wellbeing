package com.example.ui.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.AegisRepository
import com.example.data.preferences.AegisSettingsState
import com.example.data.preferences.BlockScreenVisualMode
import com.example.domain.BlockingEngine
import com.example.domain.InterceptionDecision
import com.example.service.AegisAccessibilityService
import com.example.ui.theme.MyApplicationTheme

class BlockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = AegisRepository.getInstance(applicationContext)
        val decision = extractDecisionFromIntent(intent)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                val settings by repository.settingsFlow.collectAsState(initial = AegisSettingsState())

                // If user configured Auto-Redirect on Block Screen, trigger redirect immediately
                LaunchedEffect(settings.blockScreenMode, settings.autoRedirectImmediately) {
                    if (settings.blockScreenMode == BlockScreenVisualMode.REDIRECT_URL &&
                        settings.autoRedirectImmediately &&
                        settings.redirectUrl.isNotBlank()
                    ) {
                        openRedirectUrl(settings.redirectUrl)
                    }
                }

                BlockScreenOverlayContent(
                    decision = decision,
                    settings = settings,
                    onGoBackToSafety = {
                        AegisAccessibilityService.performReturnToSafety(applicationContext)
                        finish()
                    },
                    onGrantFiveMinAccess = {
                        val snoozeKey = when (decision.triggerType) {
                            "INCOGNITO" -> "incognito:${decision.packageName}"
                            "REELS_SHORTS" -> "shorts:${decision.packageName}"
                            "AI_CONTEXT" -> "ai:${decision.packageName}"
                            else -> if (decision.matchedRuleId != null) {
                                "rule:${decision.matchedRuleId}"
                            } else {
                                decision.packageName
                            }
                        }
                        BlockingEngine.grantFiveMinuteAccess(snoozeKey)
                        BlockingEngine.grantFiveMinuteAccess(decision.packageName)
                        AegisAccessibilityService.dismissSystemOverlayWindow()
                        finish()
                    },
                    onTriggerRedirectUrl = { url ->
                        openRedirectUrl(url)
                        finish()
                    },
                    isPreviewMode = false
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun openRedirectUrl(rawUrl: String) {
        val formatted = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(viewIntent) }
    }

    private fun extractDecisionFromIntent(intent: Intent?): InterceptionDecision {
        return InterceptionDecision(
            shouldBlock = true,
            triggerType = intent?.getStringExtra(EXTRA_TRIGGER_TYPE) ?: "WEBSITE",
            matchedRuleLabel = intent?.getStringExtra(EXTRA_MATCHED_LABEL) ?: "Restricted Rule Triggered",
            matchedRuleId = intent?.getLongExtra(EXTRA_RULE_ID, -1L)?.takeIf { it != -1L },
            packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: "",
            appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: "Restricted App",
            snippet = intent?.getStringExtra(EXTRA_SNIPPET) ?: "",
            aiReasoning = intent?.getStringExtra(EXTRA_AI_REASONING) ?: "",
            challengeCodeRequired = intent?.getStringExtra(EXTRA_CHALLENGE_CODE) ?: ""
        )
    }

    companion object {
        private const val EXTRA_TRIGGER_TYPE = "extra_trigger_type"
        private const val EXTRA_MATCHED_LABEL = "extra_matched_label"
        private const val EXTRA_RULE_ID = "extra_rule_id"
        private const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val EXTRA_APP_NAME = "extra_app_name"
        private const val EXTRA_SNIPPET = "extra_snippet"
        private const val EXTRA_AI_REASONING = "extra_ai_reasoning"
        private const val EXTRA_CHALLENGE_CODE = "extra_challenge_code"

        fun createLaunchIntent(context: Context, decision: InterceptionDecision): Intent {
            return Intent(context, BlockOverlayActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                putExtra(EXTRA_TRIGGER_TYPE, decision.triggerType)
                putExtra(EXTRA_MATCHED_LABEL, decision.matchedRuleLabel)
                decision.matchedRuleId?.let { putExtra(EXTRA_RULE_ID, it) }
                putExtra(EXTRA_PACKAGE_NAME, decision.packageName)
                putExtra(EXTRA_APP_NAME, decision.appName)
                putExtra(EXTRA_SNIPPET, decision.snippet)
                putExtra(EXTRA_AI_REASONING, decision.aiReasoning)
                putExtra(EXTRA_CHALLENGE_CODE, decision.challengeCodeRequired)
            }
        }
    }
}
