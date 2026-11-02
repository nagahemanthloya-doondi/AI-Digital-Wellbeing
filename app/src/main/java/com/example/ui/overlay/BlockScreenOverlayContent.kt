package com.example.ui.overlay

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.preferences.AegisSettingsState
import com.example.data.preferences.BlockScreenVisualMode
import com.example.domain.InterceptionDecision
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmeraldBright
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.StrictAmberBright
import kotlin.math.sin

@Composable
fun BlockScreenOverlayContent(
    decision: InterceptionDecision,
    settings: AegisSettingsState,
    onGoBackToSafety: () -> Unit,
    onGrantFiveMinAccess: () -> Unit,
    onTriggerRedirectUrl: (String) -> Unit,
    isPreviewMode: Boolean = false
) {
    // CRITICAL: Consume hardware Back button so there is NO loophole to dismiss the block screen
    BackHandler(enabled = true) {
        // Intentionally consumed: user must choose "Go Back to Safety" or complete the 5-minute challenge
    }

    val requiredCode = decision.challengeCodeRequired.ifBlank { settings.defaultChallengeCode }
    var showFiveMinUnlockBox by remember {
        mutableStateOf(settings.blockScreenMode == BlockScreenVisualMode.DEFAULT_CODE)
    }
    var typedCode by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulsePhase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050912),
                        ObsidianBg,
                        Color(0xFF0F172A)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("block_screen_overlay_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AlertCrimson.copy(alpha = 0.16f))
                        .border(1.dp, AlertCrimson.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Aegis Block Shield",
                        tint = AlertCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPreviewMode) {
                            "LIVE BLOCK SCREEN PREVIEW • NO-BYPASS LOCK"
                        } else {
                            "INTERCEPTED: ${decision.triggerType}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFCDD2),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (settings.isStrictCurrentlyLocked) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(StrictAmberBright.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Strict Mode Locked",
                            tint = StrictAmberBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STRICT MODE ACTIVE • ZERO LOOPHOLE ENFORCEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = StrictAmberBright
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = settings.blockHeadline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = settings.blockMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Editable Centerpiece Area (Picture / Video / Redirect URL / Default Challenge Code)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFF1E293B)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (settings.blockScreenMode) {
                        BlockScreenVisualMode.CUSTOM_IMAGE -> {
                            EditableImageBlockCenterpiece(customImageUri = settings.customImageUri)
                        }

                        BlockScreenVisualMode.CUSTOM_VIDEO -> {
                            EditableVideoBlockCenterpiece(
                                customVideoUri = settings.customVideoUri,
                                pulsePhase = pulsePhase
                            )
                        }

                        BlockScreenVisualMode.REDIRECT_URL -> {
                            EditableRedirectBlockCenterpiece(
                                redirectUrl = settings.redirectUrl,
                                autoRedirect = settings.autoRedirectImmediately,
                                onTriggerRedirect = { onTriggerRedirectUrl(settings.redirectUrl) }
                            )
                        }

                        BlockScreenVisualMode.DEFAULT_CODE -> {
                            DefaultCodeBlockCenterpiece(requiredCode = requiredCode)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Trigger Diagnosis Strip
                    Surface(
                        color = Color(0xFF090F1E),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = decision.matchedRuleLabel.ifBlank { "Restricted Rule Triggered" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AegisEmeraldBright,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = decision.appName.ifBlank { "System Guard" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            if (decision.snippet.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Context: ${decision.snippet}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 2
                                )
                            }
                            if (decision.aiReasoning.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = decision.aiReasoning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AegisCyan
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5-Minute Access Challenge Input Drawer
            AnimatedVisibility(visible = showFiveMinUnlockBox) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131E36)),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        StrictAmberBright.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (settings.isStrictCurrentlyLocked && settings.strictModeDisableFiveMinAccess) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Snooze Locked",
                                    tint = AlertCrimson,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "5-Minute Access Disabled by Strict Mode Lock",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AlertCrimson
                                )
                            }
                        } else {
                            Text(
                                text = "ENTER CHALLENGE CODE FOR 5-MINUTE ACCESS",
                                style = MaterialTheme.typography.labelMedium,
                                color = StrictAmberBright
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color(0xFF070C17),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = requiredCode,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AegisEmeraldBright,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = typedCode,
                                onValueChange = {
                                    typedCode = it
                                    codeError = null
                                },
                                label = { Text("Type exact code above") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("block_screen_code_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AegisEmeraldBright,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            if (codeError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = codeError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AlertCrimson
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (typedCode.trim().equals(requiredCode.trim(), ignoreCase = true)) {
                                        onGrantFiveMinAccess()
                                    } else {
                                        codeError = "Code mismatch. Type the exact characters to unlock 5 minutes."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("confirm_five_min_access_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StrictAmberBright,
                                    contentColor = Color(0xFF1E1300)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Unlock 5 minutes",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verify Code & Unlock for 5 Minutes",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Primary Exit Actions (Go Back OR Access for 5 Minutes + Optional Redirect)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onGoBackToSafety,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("block_screen_go_back_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisEmeraldBright,
                        contentColor = Color(0xFF022C22)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go Back to Safety"
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Go Back to Safety",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (settings.redirectUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onTriggerRedirectUrl(settings.redirectUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("block_screen_redirect_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AegisCyan)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Redirect to Productive URL",
                            tint = AegisCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Redirect to ${settings.redirectUrl.removePrefix("https://").take(28)}",
                            color = AegisCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                val snoozeAllowed = !(settings.isStrictCurrentlyLocked && settings.strictModeDisableFiveMinAccess)
                if (snoozeAllowed) {
                    OutlinedButton(
                        onClick = { showFiveMinUnlockBox = !showFiveMinUnlockBox },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("block_screen_access_5m_toggle_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFF475569)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Access for 5 Minutes",
                            tint = StrictAmberBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showFiveMinUnlockBox) "Hide 5-Minute Unlock" else "Access for 5 Minutes (Requires Code)",
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultCodeBlockCenterpiece(requiredCode: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_focus_guardian_1793632266911),
            contentDescription = "Sanctuary Guardian Hourglass",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "DEFAULT CHALLENGE CODE SHIELD",
            style = MaterialTheme.typography.labelMedium,
            color = AegisCyan
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = requiredCode,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF070C17))
                .border(1.dp, AegisEmeraldBright.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun EditableImageBlockCenterpiece(customImageUri: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        val isPresetHero = customImageUri == "PRESET_HERO"
        val isCustomContentUri = customImageUri.startsWith("content://") || customImageUri.startsWith("file://")

        if (isCustomContentUri) {
            AsyncImage(
                model = Uri.parse(customImageUri),
                contentDescription = "Custom Motivation Block Image",
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.img_focus_guardian_1793632266911),
                placeholder = painterResource(id = R.drawable.img_focus_guardian_1793632266911),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
        } else {
            Image(
                painter = painterResource(
                    id = if (isPresetHero) {
                        R.drawable.img_aegis_hero_1793632267022
                    } else {
                        R.drawable.img_focus_guardian_1793632266911
                    }
                ),
                contentDescription = "Guardian Visual Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "CUSTOM PICTURE BLOCK SHIELD",
            style = MaterialTheme.typography.labelSmall,
            color = AegisEmeraldBright
        )
    }
}

@Composable
private fun EditableVideoBlockCenterpiece(
    customVideoUri: String,
    pulsePhase: Float
) {
    var isPlaying by remember { mutableStateOf(true) }
    val hasCustomVideo = customVideoUri.startsWith("content://") || customVideoUri.startsWith("file://") || customVideoUri.startsWith("http")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF050A14))
                .border(1.dp, AegisCyan.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasCustomVideo) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(customVideoUri))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                if (isPlaying) start()
                            }
                        }
                    },
                    update = { videoView ->
                        if (isPlaying && !videoView.isPlaying) {
                            runCatching { videoView.start() }
                        } else if (!isPlaying && videoView.isPlaying) {
                            runCatching { videoView.pause() }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Built-in procedural Focus Synthesizer Video Canvas when no gallery .mp4 is selected yet
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)
                    val baseRadius = minOf(w, h) * 0.26f

                    for (ring in 1..4) {
                        val animWave = if (isPlaying) sin(pulsePhase + ring * 0.8f) * 14f else 0f
                        drawCircle(
                            color = AegisEmeraldBright.copy(alpha = 0.18f / ring),
                            radius = baseRadius * (0.7f + ring * 0.32f) + animWave,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Dynamic focus horizon wave
                    val wavePoints = 40
                    val stepX = w / wavePoints
                    for (i in 0 until wavePoints) {
                        val x1 = i * stepX
                        val x2 = (i + 1) * stepX
                        val y1 = h * 0.5f + sin(pulsePhase + i * 0.3f) * (h * 0.16f)
                        val y2 = h * 0.5f + sin(pulsePhase + (i + 1) * 0.3f) * (h * 0.16f)
                        drawLine(
                            color = AegisCyan.copy(alpha = 0.65f),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Guardian Player",
                        tint = AegisEmeraldBright,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPlaying) "PLAYING FOCUS GUARDIAN STREAM" else "STREAM PAUSED",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Breathe deeply for 10 seconds before deciding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // Video Playback Status Pill
            Surface(
                onClick = { isPlaying = !isPlaying },
                color = Color(0xCC0F172A),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Video Playback",
                        tint = AegisEmeraldBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "Playing" else "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EditableRedirectBlockCenterpiece(
    redirectUrl: String,
    autoRedirect: Boolean,
    onTriggerRedirect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF091326))
            .border(1.dp, AegisCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "URL Redirect Mode",
            tint = AegisCyan,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (autoRedirect) "AUTO-REDIRECTING TO PRODUCTIVE DESTINATION" else "PRODUCTIVE URL REDIRECT READY",
            style = MaterialTheme.typography.labelMedium,
            color = AegisCyan,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = redirectUrl,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 13.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onTriggerRedirect,
            colors = ButtonDefaults.buttonColors(
                containerColor = AegisCyan,
                contentColor = Color(0xFF042F2E)
            )
        ) {
            Text(text = "Launch Destination Now", fontWeight = FontWeight.Bold)
        }
    }
}
