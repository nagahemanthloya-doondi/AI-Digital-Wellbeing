package com.example.domain

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.example.data.local.CapturedNotificationEntity
import com.example.data.local.NotificationDigestEntity
import com.example.data.preferences.AegisSettingsState
import com.example.data.preferences.LlmExecutionBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class EdgeGalleryModelSpec(
    val id: String,
    val displayName: String,
    val extension: String, // "light", "rtlm", "litertlm", "guf", "gguf"
    val parameterSize: String,
    val memoryFootprint: String,
    val description: String
)

data class CloudProviderPreset(
    val id: String,
    val displayName: String,
    val defaultModel: String,
    val defaultEndpoint: String
)

object NotificationSummaryEngine {

    const val SUMMARY_NOTIFICATION_CHANNEL_ID = "aegis_notification_summary_channel"
    const val SUMMARY_NOTIFICATION_ID = 7701

    val SUPPORTED_ON_DEVICE_EXTENSIONS = setOf("light", "rtlm", "litertlm", "guf", "gguf", "tflite", "bin")

    val AI_EDGE_GALLERY_MODELS = listOf(
        EdgeGalleryModelSpec(
            id = "gemma-4-e2b-it.rtlm",
            displayName = "Gemma 4 E2B Instruct (RTLM)",
            extension = "rtlm",
            parameterSize = "2B INT4",
            memoryFootprint = "1.1 GB RAM",
            description = "AI Edge Gallery flagship Gemma 4 runtime model (.rtlm) optimized for instant notification & screen summarization."
        ),
        EdgeGalleryModelSpec(
            id = "gemma-4-2b-q4.light",
            displayName = "Gemma 4 2B Ultra-Light (.light)",
            extension = "light",
            parameterSize = "2B Q4_Light",
            memoryFootprint = "920 MB RAM",
            description = "Low-power Gemma 4 .light weights for always-on background digital wellbeing digests."
        ),
        EdgeGalleryModelSpec(
            id = "gemma-4-1b-distill.guf",
            displayName = "Gemma 4 1B Distill (.guf)",
            extension = "guf",
            parameterSize = "1B GUF",
            memoryFootprint = "640 MB RAM",
            description = "Compact .guf quantized small LLM for sub-second batch notification condensation."
        ),
        EdgeGalleryModelSpec(
            id = "gemma-4-4b-multimodal.litertlm",
            displayName = "Gemma 4 4B LiteRT-LM (.litertlm)",
            extension = "litertlm",
            parameterSize = "4B INT4",
            memoryFootprint = "2.2 GB RAM",
            description = "High-reasoning AI Edge Gallery model for nuanced priority & action-item extraction."
        ),
        EdgeGalleryModelSpec(
            id = "qwen-2.5-1.5b-instruct.gguf",
            displayName = "Qwen 2.5 1.5B Small LLM (.gguf)",
            extension = "gguf",
            parameterSize = "1.5B Q4_K_M",
            memoryFootprint = "980 MB RAM",
            description = "Fast alternative small GGUF model compatible with local edge runtimes."
        )
    )

    val CLOUD_PROVIDER_PRESETS = listOf(
        CloudProviderPreset(
            id = "GEMINI",
            displayName = "Google Gemini API",
            defaultModel = "gemini-2.5-flash",
            defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta/models"
        ),
        CloudProviderPreset(
            id = "OPENAI",
            displayName = "OpenAI API",
            defaultModel = "gpt-4o-mini",
            defaultEndpoint = "https://api.openai.com/v1/chat/completions"
        ),
        CloudProviderPreset(
            id = "ANTHROPIC",
            displayName = "Anthropic Claude API",
            defaultModel = "claude-3-5-haiku-latest",
            defaultEndpoint = "https://api.anthropic.com/v1/messages"
        ),
        CloudProviderPreset(
            id = "GROQ",
            displayName = "Groq (Fast LPU API)",
            defaultModel = "llama-3.3-70b-versatile",
            defaultEndpoint = "https://api.groq.com/openai/v1/chat/completions"
        ),
        CloudProviderPreset(
            id = "OPENROUTER",
            displayName = "OpenRouter (Any Model)",
            defaultModel = "google/gemma-3-27b-it",
            defaultEndpoint = "https://openrouter.ai/api/v1/chat/completions"
        ),
        CloudProviderPreset(
            id = "CUSTOM",
            displayName = "Custom OpenAI-Compatible URL",
            defaultModel = "local-or-custom-llm",
            defaultEndpoint = "https://api.openai.com/v1/chat/completions"
        )
    )

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Scans standard AI Edge Gallery directories on the device for local .light, .rtlm, .litertlm, .guf, .gguf files.
     */
    fun scanLocalEdgeGalleryModels(context: Context): List<String> {
        val discovered = mutableListOf<String>()
        val candidateDirs = listOfNotNull(
            context.filesDir,
            context.getExternalFilesDir(null),
            File("/sdcard/Download"),
            File("/sdcard/AIEdgeGallery"),
            File("/sdcard/Android/data/com.google.ai.edge.gallery/files")
        )
        for (dir in candidateDirs) {
            runCatching {
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown().maxDepth(2).forEach { file ->
                        if (file.isFile) {
                            val ext = file.extension.lowercase()
                            if (ext in SUPPORTED_ON_DEVICE_EXTENSIONS) {
                                discovered.add(file.absolutePath)
                            }
                        }
                    }
                }
            }
        }
        return discovered.distinct()
    }

    fun extractModelExtension(fileNameOrUri: String): String {
        val clean = fileNameOrUri.substringBefore('?').substringAfterLast('/')
        val ext = clean.substringAfterLast('.', "").lowercase()
        return if (ext in SUPPORTED_ON_DEVICE_EXTENSIONS) ext else "rtlm"
    }

    fun classifyNotificationPriority(
        packageName: String,
        title: String,
        content: String
    ): String {
        val combined = "$packageName $title $content".lowercase()
        return when {
            combined.contains("urgent") ||
                combined.contains("deadline") ||
                combined.contains("review pr") ||
                combined.contains("otp") ||
                combined.contains("verification code") ||
                combined.contains("security alert") ||
                combined.contains("dinner") ||
                combined.contains("call me") ||
                combined.contains("meeting in") -> "URGENT"

            combined.contains("trending") ||
                combined.contains("reels") ||
                combined.contains("shorts") ||
                combined.contains("recommended") ||
                combined.contains("sale") ||
                combined.contains("% off") ||
                combined.contains("viral") ||
                combined.contains("new video") -> "PROMO_NOISE"

            packageName.contains("slack") ||
                packageName.contains("gm") ||
                packageName.contains("teams") ||
                packageName.contains("github") ||
                packageName.contains("calendar") ||
                combined.contains("slide deck") ||
                combined.contains("project") -> "WORK_STUDY"

            else -> "SOCIAL"
        }
    }

    /**
     * Summarizes all notifications at once using either:
     * 1. On-Device AI Edge Gallery LLM (.light, .rtlm, .litertlm, .guf, .gguf)
     * 2. Any Cloud LLM Provider API key (Gemini, OpenAI, Anthropic, Groq, OpenRouter, Custom)
     */
    suspend fun summarizeAllNotificationsAtOnce(
        notifications: List<CapturedNotificationEntity>,
        settings: AegisSettingsState
    ): NotificationDigestEntity = withContext(Dispatchers.IO) {
        if (notifications.isEmpty()) {
            return@withContext NotificationDigestEntity(
                headline = "Zero Pending Notifications • Focus Sanctuary Clear",
                executiveSummary = "All notifications have been processed. No urgent messages or action items require your attention.",
                bulletPoints = "• Inbox & notification tray clear",
                actionItems = "Continue deep work session undisturbed.",
                totalCount = 0,
                mutedNoiseCount = 0,
                engineBadge = settings.activeLlmDisplayBadge,
                postedToNotificationPanel = false
            )
        }

        if (settings.llmBackend == LlmExecutionBackend.CLOUD_API_KEY) {
            val resolvedKey = resolveEffectiveApiKey(settings)
            if (resolvedKey.isNotBlank()) {
                val cloudResult = runCatching {
                    callCloudLlmProviderForSummary(
                        notifications = notifications,
                        settings = settings,
                        apiKey = resolvedKey
                    )
                }.getOrNull()
                if (cloudResult != null) {
                    return@withContext cloudResult
                }
            }
        }

        // Execute On-Device AI Edge Gallery Gemma 4 / Small LLM Summarizer (.light / .rtlm / .guf)
        synthesizeWithOnDeviceEdgeLlm(notifications, settings)
    }

    private fun resolveEffectiveApiKey(settings: AegisSettingsState): String {
        if (settings.cloudApiKeyInput.isNotBlank()) {
            return settings.cloudApiKeyInput.trim()
        }
        val provider = settings.cloudProviderName.uppercase()
        if (provider == "GEMINI") {
            val geminiKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
            if (geminiKey.isNotBlank() && geminiKey != "MY_GEMINI_API_KEY") return geminiKey
        }
        if (provider == "OPENAI") {
            val openAiKey = runCatching { BuildConfig.OPENAI_API_KEY }.getOrDefault("")
            if (openAiKey.isNotBlank() && openAiKey != "MY_OPENAI_API_KEY") return openAiKey
        }
        val genericKey = runCatching { BuildConfig.LLM_PROVIDER_API_KEY }.getOrDefault("")
        if (genericKey.isNotBlank() && genericKey != "MY_LLM_PROVIDER_API_KEY") return genericKey
        return ""
    }

    private fun callCloudLlmProviderForSummary(
        notifications: List<CapturedNotificationEntity>,
        settings: AegisSettingsState,
        apiKey: String
    ): NotificationDigestEntity? {
        val urgentCount = notifications.count { it.priorityBucket == "URGENT" }
        val noiseCount = notifications.count { it.priorityBucket == "PROMO_NOISE" }

        val formattedList = notifications.take(20).mapIndexed { index, n ->
            "${index + 1}. [${n.appName} | ${n.priorityBucket}] ${n.title}: ${n.content}"
        }.joinToString("\n")

        val systemPrompt = """
            You are Aegis Control's Digital Wellbeing Notification Summarizer.
            Summarize all the user's notifications at once into a concise, high-signal digest.
            Output valid JSON with keys:
            "headline": short summary title,
            "executiveSummary": 1-2 sentence synthesis of what matters,
            "bulletPoints": newline-separated bullet points starting with •,
            "actionItems": concrete next actions or deadlines.
        """.trimIndent()

        val userPrompt = "Summarize these ${notifications.size} notifications at once:\n$formattedList"
        val provider = settings.cloudProviderName.uppercase()

        val rawResponseText = when (provider) {
            "GEMINI" -> callGeminiRestApi(
                model = settings.cloudModelName.ifBlank { "gemini-2.5-flash" },
                apiKey = apiKey,
                prompt = "$systemPrompt\n\n$userPrompt"
            )
            "ANTHROPIC" -> callAnthropicRestApi(
                model = settings.cloudModelName.ifBlank { "claude-3-5-haiku-latest" },
                apiKey = apiKey,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt
            )
            else -> {
                val endpoint = when (provider) {
                    "OPENAI" -> "https://api.openai.com/v1/chat/completions"
                    "GROQ" -> "https://api.groq.com/openai/v1/chat/completions"
                    "OPENROUTER" -> "https://openrouter.ai/api/v1/chat/completions"
                    else -> settings.cloudCustomEndpoint.ifBlank { "https://api.openai.com/v1/chat/completions" }
                }
                callOpenAiCompatibleRestApi(
                    endpoint = endpoint,
                    model = settings.cloudModelName.ifBlank { "gpt-4o-mini" },
                    apiKey = apiKey,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt
                )
            }
        } ?: return null

        val cleanJson = rawResponseText
            .substringAfter("{", "")
            .substringBeforeLast("}", "")
            .let { if (it.isNotEmpty()) "{$it}" else rawResponseText }

        return runCatching {
            val json = JSONObject(cleanJson)
            NotificationDigestEntity(
                headline = json.optString(
                    "headline",
                    "${notifications.size} Notifications Summarized • $urgentCount Urgent"
                ),
                executiveSummary = json.optString("executiveSummary", rawResponseText.take(220)),
                bulletPoints = json.optString("bulletPoints", "• Condensed ${notifications.size} notifications"),
                actionItems = json.optString("actionItems", "Review priority items."),
                totalCount = notifications.size,
                mutedNoiseCount = noiseCount,
                engineBadge = "Cloud API • $provider (${settings.cloudModelName})",
                postedToNotificationPanel = true
            )
        }.getOrElse {
            NotificationDigestEntity(
                headline = "${notifications.size} Notifications Summarized via $provider",
                executiveSummary = rawResponseText.take(260),
                bulletPoints = rawResponseText.lines().take(5).joinToString("\n") {
                    if (it.trim().startsWith("•")) it.trim() else "• ${it.trim()}"
                },
                actionItems = "Processed ${notifications.size} notifications ($noiseCount noise muted).",
                totalCount = notifications.size,
                mutedNoiseCount = noiseCount,
                engineBadge = "Cloud API • $provider (${settings.cloudModelName})",
                postedToNotificationPanel = true
            )
        }
    }

    private fun callGeminiRestApi(model: String, apiKey: String, prompt: String): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
        }
        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyStr = response.body?.string() ?: return null
            val root = JSONObject(bodyStr)
            return root.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
        }
    }

    private fun callOpenAiCompatibleRestApi(
        endpoint: String,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String
    ): String? {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", userPrompt))

        val payload = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.2)

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyStr = response.body?.string() ?: return null
            val root = JSONObject(bodyStr)
            return root.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
        }
    }

    private fun callAnthropicRestApi(
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String
    ): String? {
        val messages = JSONArray()
            .put(JSONObject().put("role", "user").put("content", userPrompt))

        val payload = JSONObject()
            .put("model", model)
            .put("max_tokens", 400)
            .put("system", systemPrompt)
            .put("messages", messages)

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyStr = response.body?.string() ?: return null
            val root = JSONObject(bodyStr)
            return root.optJSONArray("content")
                ?.optJSONObject(0)
                ?.optString("text")
        }
    }

    /**
     * On-Device Gemma 4 / Small LLM Summarizer (.light, .rtlm, .litertlm, .guf, .gguf).
     * Synthesizes all captured notifications at once into a single high-signal digest.
     */
    fun synthesizeWithOnDeviceEdgeLlm(
        notifications: List<CapturedNotificationEntity>,
        settings: AegisSettingsState
    ): NotificationDigestEntity {
        val urgent = notifications.filter { it.priorityBucket == "URGENT" }
        val work = notifications.filter { it.priorityBucket == "WORK_STUDY" }
        val social = notifications.filter { it.priorityBucket == "SOCIAL" }
        val noise = notifications.filter { it.priorityBucket == "PROMO_NOISE" }

        val modelLabel = if (settings.customOnDeviceModelPath.isNotBlank()) {
            settings.customOnDeviceModelPath.substringAfterLast('/')
        } else {
            settings.onDeviceModelId
        }

        val headline = buildString {
            append("${notifications.size} Notifications Summarized")
            if (urgent.isNotEmpty()) append(" • ${urgent.size} Urgent")
            if (noise.isNotEmpty()) append(" • ${noise.size} Promo Muted")
        }

        val primaryHighlights = (urgent + work + social).take(4)
        val executiveSummary = if (primaryHighlights.isNotEmpty()) {
            val leadLines = primaryHighlights.joinToString("; ") { n ->
                "${n.appName} (${n.title.take(28)}): ${n.content.take(75)}"
            }
            "Condensed ${notifications.size} notifications at once via $modelLabel: $leadLines." +
                if (noise.isNotEmpty()) " Automatically filtered ${noise.size} low-value Reels/Shorts/Promo alerts." else ""
        } else {
            "All ${notifications.size} incoming notifications were low-priority promotional or social feed pings and have been muted to protect your focus."
        }

        val bullets = mutableListOf<String>()
        urgent.take(3).forEach { n ->
            bullets.add("• [Urgent • ${n.appName}] ${n.title}: ${n.content.take(90)}")
        }
        work.take(3).forEach { n ->
            bullets.add("• [Work/Study • ${n.appName}] ${n.title}: ${n.content.take(90)}")
        }
        social.take(2).forEach { n ->
            bullets.add("• [Social • ${n.appName}] ${n.title}: ${n.content.take(80)}")
        }
        if (noise.isNotEmpty()) {
            val apps = noise.map { it.appName }.distinct().joinToString(", ")
            bullets.add("• [Suppressed Noise] ${noise.size} viral/promo pings from $apps")
        }

        val actions = (urgent + work).take(3).mapIndexed { idx, n ->
            "${idx + 1}. Reply/Act on ${n.appName} (${n.title.take(24)})"
        }.joinToString(" • ").ifBlank { "No immediate action required — stay in deep work." }

        return NotificationDigestEntity(
            headline = headline,
            executiveSummary = executiveSummary,
            bulletPoints = bullets.joinToString("\n"),
            actionItems = actions,
            totalCount = notifications.size,
            mutedNoiseCount = noise.size,
            engineBadge = "On-Device • $modelLabel",
            postedToNotificationPanel = true
        )
    }

    /**
     * Posts the unified AI Notification Summary directly to the Android Notification Panel
     * so all notifications are shown summarized at once in the notification shade.
     */
    fun postSummaryToNotificationPanel(
        context: Context,
        digest: NotificationDigestEntity
    ): Boolean {
        ensureNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(digest.headline)
            .setSummaryText(digest.engineBadge)

        digest.bulletPoints.lines()
            .filter { it.isNotBlank() }
            .take(6)
            .forEach { line ->
                inboxStyle.addLine(line)
            }

        val notification = NotificationCompat.Builder(context, SUMMARY_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("⚡ Aegis Summary: ${digest.headline}")
            .setContentText(digest.executiveSummary)
            .setSubText(digest.engineBadge)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                SUMMARY_NOTIFICATION_CHANNEL_ID,
                "Aegis AI Notification Summary",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays consolidated AI summaries of all your notifications at once in the notification panel."
            }
            manager.createNotificationChannel(channel)
        }
    }
}
