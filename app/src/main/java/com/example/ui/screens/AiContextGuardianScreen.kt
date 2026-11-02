package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CapturedNotificationEntity
import com.example.data.local.NotificationDigestEntity
import com.example.data.preferences.AegisSettingsState
import com.example.data.preferences.LlmExecutionBackend
import com.example.domain.AiContextVerdict
import com.example.domain.BlockingEngine
import com.example.domain.EdgeGalleryModelSpec
import com.example.domain.NotificationSummaryEngine
import com.example.domain.ScreenContextSnapshot
import com.example.domain.SystemPermissionStatus
import com.example.domain.UsageAnalyticsManager
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmeraldBright
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.StrictAmberBright
import com.example.ui.theme.VioletAi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiContextGuardianScreen(
    permissionStatus: SystemPermissionStatus,
    settings: AegisSettingsState,
    capturedNotifications: List<CapturedNotificationEntity>,
    notificationDigests: List<NotificationDigestEntity>,
    discoveredEdgeModels: List<String>,
    isSummarizing: Boolean,
    latestLiveSnapshot: ScreenContextSnapshot,
    onSaveNotificationSummaryConfig: (
        Boolean,
        Boolean,
        Boolean,
        LlmExecutionBackend,
        String,
        String,
        String,
        String,
        String,
        String
    ) -> Unit,
    onSummarizeAllAndPostNow: () -> Unit,
    onSimulateIncomingBatch: () -> Unit,
    onClearCapturedNotifications: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onSaveAiSettings: (Boolean, String, Int, Boolean) -> Unit,
    onEvaluateSample: (String, String) -> AiContextVerdict,
    onTriggerAiBlockOverlay: (AiContextVerdict, String) -> Unit
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Notification Summary, 1 = Screen Context AI

    // Notification Summary & LLM state
    var notifSummaryEnabled by remember(settings.notificationSummaryEnabled) {
        mutableStateOf(settings.notificationSummaryEnabled)
    }
    var autoPostPanel by remember(settings.autoPostSummaryToNotificationPanel) {
        mutableStateOf(settings.autoPostSummaryToNotificationPanel)
    }
    var suppressPromos by remember(settings.suppressPromoNotificationsWhenSummarized) {
        mutableStateOf(settings.suppressPromoNotificationsWhenSummarized)
    }
    var selectedBackend by remember(settings.llmBackend) {
        mutableStateOf(settings.llmBackend)
    }
    var selectedOnDeviceModelId by remember(settings.onDeviceModelId) {
        mutableStateOf(settings.onDeviceModelId)
    }
    var customModelPath by remember(settings.customOnDeviceModelPath) {
        mutableStateOf(settings.customOnDeviceModelPath)
    }
    var cloudProvider by remember(settings.cloudProviderName) {
        mutableStateOf(settings.cloudProviderName)
    }
    var cloudModel by remember(settings.cloudModelName) {
        mutableStateOf(settings.cloudModelName)
    }
    var cloudEndpoint by remember(settings.cloudCustomEndpoint) {
        mutableStateOf(settings.cloudCustomEndpoint)
    }
    var cloudApiKey by remember(settings.cloudApiKeyInput) {
        mutableStateOf(settings.cloudApiKeyInput)
    }

    // Local Model File Picker (.light, .rtlm, .litertlm, .guf, .gguf)
    val modelFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val rawPath = uri.lastPathSegment ?: uri.toString()
            customModelPath = rawPath
            selectedBackend = LlmExecutionBackend.ON_DEVICE_EDGE
            onSaveNotificationSummaryConfig(
                notifSummaryEnabled,
                autoPostPanel,
                suppressPromos,
                LlmExecutionBackend.ON_DEVICE_EDGE,
                selectedOnDeviceModelId,
                rawPath,
                cloudProvider,
                cloudModel,
                cloudEndpoint,
                cloudApiKey
            )
        }
    }

    // Runtime permission launcher for POST_NOTIFICATIONS on Android 13+
    val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onRefreshPermissions()
        onSummarizeAllAndPostNow()
    }

    // Screen Context AI state
    var aiEnabled by remember(settings.aiGuardianEnabled) { mutableStateOf(settings.aiGuardianEnabled) }
    var instructions by remember(settings.aiUserInstructions) { mutableStateOf(settings.aiUserInstructions) }
    var threshold by remember(settings.aiSensitivityThreshold) {
        mutableFloatStateOf(settings.aiSensitivityThreshold.toFloat())
    }
    var autoBlock by remember(settings.aiAutoBlockOnVerdict) { mutableStateOf(settings.aiAutoBlockOnVerdict) }

    var sampleUrl by remember { mutableStateOf("https://www.youtube.com/watch?v=viral_drama") }
    var sampleScreenText by remember {
        mutableStateOf("SHOCKING Celebrity Gossip & Viral Prank Reaction Compilation #shorts #fyp Watch till the end!")
    }
    var currentVerdict by remember(instructions, threshold) {
        mutableStateOf(
            BlockingEngine.evaluateAiScreenContext(
                snapshot = ScreenContextSnapshot(
                    packageName = "com.android.chrome",
                    appName = "Chrome",
                    detectedUrl = sampleUrl,
                    visibleTexts = listOf(sampleScreenText)
                ),
                userInstructions = instructions,
                threshold = threshold.toInt()
            )
        )
    }

    val liveSnapshotVerdict = remember(latestLiveSnapshot, instructions, threshold) {
        BlockingEngine.evaluateAiScreenContext(
            snapshot = latestLiveSnapshot,
            userInstructions = instructions,
            threshold = threshold.toInt()
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("ai_context_guardian_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Wellbeing & Notification Summary",
                    tint = VioletAi,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Notification Summary & AI Guard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "On-Device Gemma 4 (.light / .rtlm / .guf) or Cloud LLM API Key",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Top Sub-Navigation Segmented Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurface)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { activeSubTab = 0 },
                    color = if (activeSubTab == 0) AegisCyan else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtab_notification_summary")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (activeSubTab == 0) Color(0xFF022C22) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Notification Summary",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (activeSubTab == 0) Color(0xFF022C22) else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    onClick = { activeSubTab = 1 },
                    color = if (activeSubTab == 1) VioletAi else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtab_screen_context_ai")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Screen Context AI",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (activeSubTab == 0) {
            // --- NOTIFICATION SUMMARY SERVICE TAB ---

            // 1. Notification Access & Master Switch Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AegisCyan.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Digital Wellbeing Notification Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Summarizes all incoming notifications at once and shows the unified digest in your Notification Panel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notifSummaryEnabled,
                                onCheckedChange = {
                                    notifSummaryEnabled = it
                                    onSaveNotificationSummaryConfig(
                                        it,
                                        autoPostPanel,
                                        suppressPromos,
                                        selectedBackend,
                                        selectedOnDeviceModelId,
                                        customModelPath,
                                        cloudProvider,
                                        cloudModel,
                                        cloudEndpoint,
                                        cloudApiKey
                                    )
                                },
                                modifier = Modifier.testTag("notif_summary_master_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF022C22),
                                    checkedTrackColor = AegisCyan
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Notification Listener Access Status Banner
                        Surface(
                            color = ObsidianSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    UsageAnalyticsManager.openNotificationListenerSettings(context)
                                }
                                .testTag("grant_notification_access_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (permissionStatus.isNotificationAccessGranted) {
                                            Icons.Default.CheckCircle
                                        } else {
                                            Icons.Default.Warning
                                        },
                                        contentDescription = "Notification Access",
                                        tint = if (permissionStatus.isNotificationAccessGranted) {
                                            AegisEmeraldBright
                                        } else {
                                            StrictAmberBright
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (permissionStatus.isNotificationAccessGranted) {
                                                "Notification Access Granted • Listening Live"
                                            } else {
                                                "Grant Notification Access Permission"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Required to read incoming notifications and condense them into one Notification Panel card",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open Settings",
                                    tint = AegisCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = autoPostPanel,
                                onCheckedChange = { autoPostPanel = it }
                            )
                            Text(
                                text = "Automatically post consolidated digest to the Android Notification Panel",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = suppressPromos,
                                onCheckedChange = { suppressPromos = it }
                            )
                            Text(
                                text = "Mute low-priority promotional / Reels / Shorts notifications once summarized",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // 2. Choose LLM Backend: On-Device AI Edge Gallery (Gemma 4 .light / .rtlm / .guf) OR Cloud Provider API Key
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Select Summarizer LLM Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Run 100% locally with AI Edge Gallery Gemma 4 / Small LLMs (.light, .rtlm, .guf) or connect any Cloud LLM API key",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = selectedBackend == LlmExecutionBackend.ON_DEVICE_EDGE,
                                onClick = { selectedBackend = LlmExecutionBackend.ON_DEVICE_EDGE },
                                label = { Text("On-Device LLM (Gemma 4)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AegisEmeraldBright,
                                    selectedLabelColor = Color(0xFF022C22),
                                    selectedLeadingIconColor = Color(0xFF022C22)
                                ),
                                modifier = Modifier.testTag("chip_on_device_llm")
                            )

                            FilterChip(
                                selected = selectedBackend == LlmExecutionBackend.CLOUD_API_KEY,
                                onClick = { selectedBackend = LlmExecutionBackend.CLOUD_API_KEY },
                                label = { Text("Any LLM Provider API Key") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AegisCyan,
                                    selectedLabelColor = Color(0xFF022C22),
                                    selectedLeadingIconColor = Color(0xFF022C22)
                                ),
                                modifier = Modifier.testTag("chip_cloud_api_llm")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (selectedBackend == LlmExecutionBackend.ON_DEVICE_EDGE) {
                            Text(
                                text = "AI Edge Gallery Models (.rtlm, .light, .guf, .litertlm, .gguf):",
                                style = MaterialTheme.typography.labelMedium,
                                color = AegisEmeraldBright
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            NotificationSummaryEngine.AI_EDGE_GALLERY_MODELS.forEach { spec ->
                                EdgeModelSpecRow(
                                    spec = spec,
                                    isSelected = selectedOnDeviceModelId == spec.id && customModelPath.isBlank(),
                                    onSelect = {
                                        selectedOnDeviceModelId = spec.id
                                        customModelPath = ""
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Custom Model File Import (.light, .rtlm, .guf, .litertlm, .gguf)
                            Surface(
                                color = ObsidianSurfaceElevated,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Load Custom AI Edge Gallery Model File",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (customModelPath.isNotBlank()) {
                                            "Active File: $customModelPath (.${NotificationSummaryEngine.extractModelExtension(customModelPath).uppercase()})"
                                        } else {
                                            "Supports .light, .rtlm, .litertlm, .guf, and .gguf model files from device storage"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (discoveredEdgeModels.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Discovered on device: ${discoveredEdgeModels.joinToString { it.substringAfterLast('/') }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AegisCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            modelFilePicker.launch(arrayOf("*/*"))
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pick_edge_model_file_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = "Select Model File",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Browse Device for .light / .rtlm / .guf Model")
                                    }
                                }
                            }
                        } else {
                            // Cloud LLM Provider API Key Configuration
                            Text(
                                text = "Choose LLM Provider:",
                                style = MaterialTheme.typography.labelMedium,
                                color = AegisCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NotificationSummaryEngine.CLOUD_PROVIDER_PRESETS.forEach { preset ->
                                    FilterChip(
                                        selected = cloudProvider == preset.id,
                                        onClick = {
                                            cloudProvider = preset.id
                                            cloudModel = preset.defaultModel
                                            cloudEndpoint = preset.defaultEndpoint
                                        },
                                        label = { Text(preset.displayName, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = cloudModel,
                                onValueChange = { cloudModel = it },
                                label = { Text("Model Name (e.g. gemini-2.5-flash, gpt-4o-mini, claude-3-5-haiku-latest)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (cloudProvider == "CUSTOM" || cloudProvider == "OPENROUTER") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = cloudEndpoint,
                                    onValueChange = { cloudEndpoint = it },
                                    label = { Text("OpenAI-Compatible Chat Completions Endpoint URL") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = cloudApiKey,
                                onValueChange = { cloudApiKey = it },
                                label = { Text("$cloudProvider API Key (or set in AI Studio Secrets panel)") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cloud_llm_api_key_input")
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tip: You can also configure LLM_PROVIDER_API_KEY, GEMINI_API_KEY, or OPENAI_API_KEY securely in the AI Studio Secrets panel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onSaveNotificationSummaryConfig(
                                    notifSummaryEnabled,
                                    autoPostPanel,
                                    suppressPromos,
                                    selectedBackend,
                                    selectedOnDeviceModelId,
                                    customModelPath,
                                    cloudProvider,
                                    cloudModel,
                                    cloudEndpoint,
                                    cloudApiKey
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_notif_llm_config_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisEmeraldBright,
                                contentColor = Color(0xFF022C22)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save LLM Config")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Notification Summary LLM Configuration", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Action Bar: Summarize All Notifications at Once & Show in Notification Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AegisEmeraldBright.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Batch Summarize & Publish to Notification Panel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Active Engine: ${settings.activeLlmDisplayBadge} • ${capturedNotifications.size} notifications in queue",
                            style = MaterialTheme.typography.bodySmall,
                            color = AegisCyan
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !permissionStatus.isPostNotificationsGranted
                                ) {
                                    postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSummarizeAllAndPostNow()
                                }
                            },
                            enabled = !isSummarizing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("summarize_all_notifications_now_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisCyan,
                                contentColor = Color(0xFF022C22)
                            )
                        ) {
                            if (isSummarizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF022C22)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Summarizing All Notifications...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Summarize All"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Summarize All (${capturedNotifications.size}) & Show in Notification Panel",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onSimulateIncomingBatch,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("simulate_incoming_notifications_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Simulate Incoming Batch")
                            }
                            if (capturedNotifications.isNotEmpty()) {
                                OutlinedButton(onClick = onClearCapturedNotifications) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Clear Queue",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Latest Published Notification Panel Digest Preview
            val latestDigest = notificationDigests.firstOrNull()
            if (latestDigest != null) {
                item {
                    PublishedDigestPreviewCard(digest = latestDigest)
                }
            }

            // 5. Captured Notifications Queue
            item {
                Text(
                    text = "Captured Notifications Queue (${capturedNotifications.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(capturedNotifications.take(12), key = { it.id }) { notif ->
                CapturedNotificationItemCard(notif = notif)
            }
        } else {
            // --- SCREEN CONTEXT AI GUARDIAN TAB ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VioletAi.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Optional AI Context Analysis",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Evaluates visible window text, headings & URLs against your goal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = aiEnabled,
                                onCheckedChange = {
                                    aiEnabled = it
                                    onSaveAiSettings(it, instructions, threshold.toInt(), autoBlock)
                                },
                                modifier = Modifier.testTag("ai_guardian_master_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VioletAi
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            label = { Text("Your AI Self-Control Charter (What to Allow vs. Block)") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_instructions_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Block Risk Threshold",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${threshold.toInt()}% Risk Score",
                                style = MaterialTheme.typography.labelMedium,
                                color = VioletAi,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = threshold,
                            onValueChange = { threshold = it },
                            valueRange = 25f..90f,
                            colors = SliderDefaults.colors(
                                thumbColor = VioletAi,
                                activeTrackColor = VioletAi
                            )
                        )

                        Button(
                            onClick = {
                                onSaveAiSettings(aiEnabled, instructions, threshold.toInt(), autoBlock)
                                currentVerdict = onEvaluateSample(sampleUrl, sampleScreenText)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_ai_charter_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VioletAi,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save AI Rules")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save AI Screen Context Rules", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Live Screen Context",
                                tint = AegisCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Latest Captured Accessibility Context",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = ObsidianSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "App: ${latestLiveSnapshot.appName} (${latestLiveSnapshot.packageName})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AegisCyan
                                )
                                if (latestLiveSnapshot.detectedUrl.isNotBlank()) {
                                    Text(
                                        text = "URL: ${latestLiveSnapshot.detectedUrl}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Visible Nodes: ${latestLiveSnapshot.visibleTexts.joinToString(" • ").take(160)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Live Verdict: ${if (liveSnapshotVerdict.shouldBlock) "BLOCK (${liveSnapshotVerdict.distractionScore}%)" else "CONTINUE WATCHING (${liveSnapshotVerdict.distractionScore}%)"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (liveSnapshotVerdict.shouldBlock) AlertCrimson else AegisEmeraldBright,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Interactive AI Context Decision Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select a realistic screen scenario or type any screen text to see how AI decides to block or continue watching:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val scenarios = listOf(
                            "Celebrity Drama & Shorts" to ("https://youtube.com/shorts/drama" to "SHOCKING Celebrity Gossip & Viral Prank Reaction Compilation #shorts #fyp"),
                            "Kotlin Android Lecture" to ("https://developer.android.com/kotlin" to "Advanced Kotlin Coroutines, Jetpack Compose Architecture & Compiler Tutorial Lecture"),
                            "Online Casino & Slots" to ("https://stake.com/casino/slots" to "Live Roulette Spin Jackpot Casino Betting Bonus 100x Free Spins"),
                            "Algorithm Research Paper" to ("https://en.wikipedia.org/wiki/Aho-Corasick_algorithm" to "Computer Science Algorithm Documentation: String-searching finite state machine")
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            scenarios.forEach { (label, pair) ->
                                FilterChip(
                                    selected = sampleUrl == pair.first,
                                    onClick = {
                                        sampleUrl = pair.first
                                        sampleScreenText = pair.second
                                        currentVerdict = BlockingEngine.evaluateAiScreenContext(
                                            snapshot = ScreenContextSnapshot(
                                                packageName = "com.android.chrome",
                                                appName = "Chrome",
                                                detectedUrl = pair.first,
                                                visibleTexts = listOf(pair.second)
                                            ),
                                            userInstructions = instructions,
                                            threshold = threshold.toInt()
                                        )
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = sampleScreenText,
                            onValueChange = {
                                sampleScreenText = it
                                currentVerdict = BlockingEngine.evaluateAiScreenContext(
                                    snapshot = ScreenContextSnapshot(
                                        packageName = "com.android.chrome",
                                        appName = "Chrome",
                                        detectedUrl = sampleUrl,
                                        visibleTexts = listOf(it)
                                    ),
                                    userInstructions = instructions,
                                    threshold = threshold.toInt()
                                )
                            },
                            label = { Text("Simulated Screen Text Extracted by Accessibility") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AiVerdictResultCard(
                            verdict = currentVerdict,
                            onEnforceBlock = {
                                onTriggerAiBlockOverlay(currentVerdict, sampleScreenText)
                            }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EdgeModelSpecRow(
    spec: EdgeGalleryModelSpec,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        color = if (isSelected) AegisEmeraldBright.copy(alpha = 0.14f) else ObsidianSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) AegisEmeraldBright else Color(0xFF1E293B),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = AegisCyan.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ".${spec.extension.uppercase()}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AegisCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = spec.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${spec.parameterSize} • ${spec.memoryFootprint} • ${spec.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AegisEmeraldBright
                )
                Text(
                    text = spec.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected Model",
                    tint = AegisEmeraldBright,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun PublishedDigestPreviewCard(digest: NotificationDigestEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, AegisCyan)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notification Panel Summary",
                        tint = AegisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SHOWN IN NOTIFICATION PANEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = AegisCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = AegisEmeraldBright.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = digest.engineBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = AegisEmeraldBright,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "⚡ ${digest.headline}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = digest.executiveSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color(0xFF061020),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "CONDENSED NOTIFICATION PANEL BULLETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = AegisCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = digest.bulletPoints,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            if (digest.actionItems.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Action Items: ${digest.actionItems}",
                    style = MaterialTheme.typography.labelMedium,
                    color = StrictAmberBright
                )
            }
        }
    }
}

@Composable
private fun CapturedNotificationItemCard(notif: CapturedNotificationEntity) {
    val badgeColor = when (notif.priorityBucket) {
        "URGENT" -> AlertCrimson
        "WORK_STUDY" -> AegisEmeraldBright
        "PROMO_NOISE" -> StrictAmberBright
        else -> AegisCyan
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = notif.priorityBucket,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = notif.appName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notif.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = notif.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AiVerdictResultCard(
    verdict: AiContextVerdict,
    onEnforceBlock: () -> Unit
) {
    val accent = if (verdict.shouldBlock) AlertCrimson else AegisEmeraldBright

    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (verdict.shouldBlock) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (verdict.shouldBlock) {
                            "AI DECISION: BLOCK SCREEN"
                        } else {
                            "AI DECISION: CONTINUE TO WATCH"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Risk: ${verdict.distractionScore}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { verdict.distractionScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = accent,
                trackColor = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = verdict.reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )

            if (verdict.shouldBlock) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onEnforceBlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enforce_ai_block_overlay_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertCrimson,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger AI Block Screen Overlay Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
