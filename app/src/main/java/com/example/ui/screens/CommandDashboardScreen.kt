package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.InterceptionLogEntity
import com.example.data.preferences.AegisSettingsState
import com.example.domain.SystemPermissionStatus
import com.example.domain.UsageAnalyticsManager
import com.example.ui.UsageScrollSummary
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmeraldBright
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.StrictAmberBright
import com.example.ui.theme.VioletAi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommandDashboardScreen(
    permissionStatus: SystemPermissionStatus,
    settings: AegisSettingsState,
    summary: UsageScrollSummary,
    totalInterceptions: Int,
    recentLogs: List<InterceptionLogEntity>,
    activeSnoozes: Map<String, Int>,
    clockTick: Long,
    onRefreshPermissions: () -> Unit,
    onToggleWebsiteBlocker: (Boolean) -> Unit,
    onToggleIncognitoBlocker: (Boolean) -> Unit,
    onToggleYoutubeShorts: (Boolean) -> Unit,
    onToggleInstagramReels: (Boolean) -> Unit,
    onToggleAiGuardian: (Boolean) -> Unit,
    onToggleNotificationSummary: (Boolean) -> Unit,
    onTriggerNotificationSummaryNow: () -> Unit,
    onStartStrictMode: (Int, Boolean, Boolean) -> Unit,
    onEmergencyUnlockStrict: (String) -> Boolean,
    onSimulateLiveBlock: (String, String, String, String) -> Unit,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    var selectedStrictMinutes by remember { mutableIntStateOf(60) }
    var preventSettingsTamper by remember { mutableStateOf(settings.strictModePreventSettingsTamper) }
    var disableFiveMinSnooze by remember { mutableStateOf(settings.strictModeDisableFiveMinAccess) }
    var showEmergencyUnlockDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("command_dashboard_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            HeroSanctuaryBanner(
                focusScore = summary.focusHealthScore,
                totalInterceptions = totalInterceptions,
                isStrictLocked = settings.isStrictCurrentlyLocked,
                onTestOverlayNow = {
                    onSimulateLiveBlock(
                        "WEBSITE",
                        "Blocked Website: tiktok.com",
                        "Google Chrome",
                        "https://www.tiktok.com/@trending/video/9812412"
                    )
                }
            )
        }

        // System Permissions Control Center (4 OS Engines)
        item {
            SystemPermissionsCard(
                status = permissionStatus,
                onOpenAccessibility = { UsageAnalyticsManager.openAccessibilitySettings(context) },
                onOpenUsageAccess = { UsageAnalyticsManager.openUsageAccessSettings(context) },
                onOpenOverlay = { UsageAnalyticsManager.openOverlayPermissionSettings(context) },
                onOpenNotificationAccess = { UsageAnalyticsManager.openNotificationListenerSettings(context) },
                onRefresh = onRefreshPermissions
            )
        }

        if (activeSnoozes.isNotEmpty()) {
            item {
                ActiveSnoozePassCard(activeSnoozes = activeSnoozes)
            }
        }

        // Strict Mode Commitment Vault ("How much time did you stay?")
        item {
            StrictModeCommitmentCard(
                settings = settings,
                clockTick = clockTick,
                selectedMinutes = selectedStrictMinutes,
                onSelectMinutes = { selectedStrictMinutes = it },
                preventTamper = preventSettingsTamper,
                onTogglePreventTamper = { preventSettingsTamper = it },
                disableSnooze = disableFiveMinSnooze,
                onToggleDisableSnooze = { disableFiveMinSnooze = it },
                onActivateStrict = {
                    onStartStrictMode(selectedStrictMinutes, preventSettingsTamper, disableFiveMinSnooze)
                },
                onRequestEmergencyUnlock = { showEmergencyUnlockDialog = true }
            )
        }

        // Core Protection Shields Matrix
        item {
            CoreProtectionMatrixCard(
                settings = settings,
                onToggleWebsiteBlocker = onToggleWebsiteBlocker,
                onToggleIncognitoBlocker = onToggleIncognitoBlocker,
                onToggleYoutubeShorts = onToggleYoutubeShorts,
                onToggleInstagramReels = onToggleInstagramReels,
                onToggleAiGuardian = onToggleAiGuardian,
                onToggleNotificationSummary = onToggleNotificationSummary,
                onQuickTestIncognito = {
                    onSimulateLiveBlock(
                        "INCOGNITO",
                        "Incognito / Private Browsing Blocked",
                        "Google Chrome",
                        "Detected 'New incognito tab' & private toolbar surface"
                    )
                },
                onQuickTestReels = {
                    onSimulateLiveBlock(
                        "REELS_SHORTS",
                        "YouTube Shorts & Instagram Reels Blocker",
                        "YouTube",
                        "Intercepted vertical Shorts player container (reel_player_page_container)"
                    )
                },
                onQuickTestNotificationSummary = onTriggerNotificationSummaryNow
            )
        }

        // Recent Interceptions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Interception Log",
                        tint = AegisEmeraldBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Interception Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (recentLogs.isNotEmpty()) {
                    TextButton(
                        onClick = onClearLogs,
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Logs",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }
        }

        if (recentLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.GppGood,
                            contentDescription = "Clean Log",
                            tint = AegisEmeraldBright,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No distractions intercepted yet.",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Tap 'Simulate Live Block' above to test the un-bypassable shield.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentLogs.take(8), key = { it.id }) { log ->
                InterceptionLogItemCard(log = log)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showEmergencyUnlockDialog) {
        EmergencyStrictUnlockDialog(
            requiredCode = settings.defaultChallengeCode,
            onDismiss = { showEmergencyUnlockDialog = false },
            onUnlockAttempt = { code ->
                val ok = onEmergencyUnlockStrict(code)
                if (ok) showEmergencyUnlockDialog = false
                ok
            }
        )
    }
}

@Composable
private fun HeroSanctuaryBanner(
    focusScore: Int,
    totalInterceptions: Int,
    isStrictLocked: Boolean,
    onTestOverlayNow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_aegis_hero_1793632267022),
                contentDescription = "Aegis Sanctuary Shield Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x66070C17),
                                Color(0xD9070C17),
                                Color(0xF5070C17)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isStrictLocked) StrictAmberBright.copy(alpha = 0.2f) else AegisEmeraldBright.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isStrictLocked) StrictAmberBright else AegisEmeraldBright
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = if (isStrictLocked) Icons.Default.Lock else Icons.Default.Shield,
                                contentDescription = "Sovereign Status",
                                tint = if (isStrictLocked) StrictAmberBright else AegisEmeraldBright,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isStrictLocked) "STRICT FORTRESS ACTIVE" else "AEGIS GUARDIAN ARMED",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "$totalInterceptions Blocks",
                        style = MaterialTheme.typography.labelMedium,
                        color = AegisCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Self-Governing Digital Sovereignty",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Focus Integrity: $focusScore% • Browser, Incognito, Reels, AI Context & Notification Summary",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onTestOverlayNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("simulate_live_block_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisEmeraldBright,
                        contentColor = Color(0xFF022C22)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Test Block Screen",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simulate Live Block Screen Overlay",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemPermissionsCard(
    status: SystemPermissionStatus,
    onOpenAccessibility: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "System Enforcement Engines (${status.activeCount}/4)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Grant device permissions for real-time OS interception & notification digests",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_permissions_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Permission Status",
                        tint = AegisCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PermissionRowItem(
                title = "1. Accessibility Service Engine",
                subtitle = "Inspects browser URLs, keywords, Incognito tabs, Reels/Shorts & AI context",
                isGranted = status.isAccessibilityEnabled,
                actionLabel = if (status.isAccessibilityEnabled) "Active" else "Enable",
                testTag = "perm_accessibility_button",
                onClick = onOpenAccessibility
            )

            Spacer(modifier = Modifier.height(10.dp))

            PermissionRowItem(
                title = "2. Display Over Other Apps (Overlay)",
                subtitle = "Draws the un-bypassable full-screen lock shield over restricted apps",
                isGranted = status.isOverlayPermissionGranted,
                actionLabel = if (status.isOverlayPermissionGranted) "Active" else "Grant",
                testTag = "perm_overlay_button",
                onClick = onOpenOverlay
            )

            Spacer(modifier = Modifier.height(10.dp))

            PermissionRowItem(
                title = "3. Device Usage Access",
                subtitle = "Measures screen time, scroll duration, and wasted vs. focused time",
                isGranted = status.isUsageAccessGranted,
                actionLabel = if (status.isUsageAccessGranted) "Active" else "Grant",
                testTag = "perm_usage_button",
                onClick = onOpenUsageAccess
            )

            Spacer(modifier = Modifier.height(10.dp))

            PermissionRowItem(
                title = "4. Notification Access (AI Summary)",
                subtitle = "Captures notifications and condenses them at once into the Notification Panel",
                isGranted = status.isNotificationAccessGranted,
                actionLabel = if (status.isNotificationAccessGranted) "Active" else "Grant",
                testTag = "perm_notification_listener_button",
                onClick = onOpenNotificationAccess
            )
        }
    }
}

@Composable
private fun PermissionRowItem(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = ObsidianSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
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
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = title,
                    tint = if (isGranted) AegisEmeraldBright else StrictAmberBright,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = if (isGranted) AegisEmeraldBright.copy(alpha = 0.16f) else StrictAmberBright.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isGranted) AegisEmeraldBright else StrictAmberBright,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isGranted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Settings",
                            tint = StrictAmberBright,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSnoozePassCard(activeSnoozes: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1608)),
        border = androidx.compose.foundation.BorderStroke(1.dp, StrictAmberBright.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Active 5-Minute Access",
                    tint = StrictAmberBright,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Active 5-Minute Temporary Access Passes",
                    style = MaterialTheme.typography.titleSmall,
                    color = StrictAmberBright
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            activeSnoozes.forEach { (key, seconds) ->
                val mins = seconds / 60
                val secs = seconds % 60
                Text(
                    text = "• $key — Re-locks automatically in ${mins}m ${secs}s",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StrictModeCommitmentCard(
    settings: AegisSettingsState,
    clockTick: Long,
    selectedMinutes: Int,
    onSelectMinutes: (Int) -> Unit,
    preventTamper: Boolean,
    onTogglePreventTamper: (Boolean) -> Unit,
    disableSnooze: Boolean,
    onToggleDisableSnooze: (Boolean) -> Unit,
    onActivateStrict: () -> Unit,
    onRequestEmergencyUnlock: () -> Unit
) {
    val now = maxOf(clockTick, System.currentTimeMillis())
    val isLocked = settings.strictModeActive && now < settings.strictModeEndTimestamp
    val remainingSec = if (isLocked) ((settings.strictModeEndTimestamp - now) / 1000L).coerceAtLeast(0L) else 0L
    val currentStayedMins = if (isLocked && settings.strictModeStartTimestamp > 0L) {
        ((now - settings.strictModeStartTimestamp).coerceAtLeast(0L)) / 60_000L
    } else 0L
    val totalLifetimeStayedMins = settings.strictModeTotalMinutesStayed + currentStayedMins

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) Color(0xFF1C1408) else ObsidianSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = if (isLocked) StrictAmberBright else Color(0xFF1E293B)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Security,
                        contentDescription = "Strict Mode",
                        tint = StrictAmberBright,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Strict Mode Commitment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Time Stayed in Strict Mode: ${totalLifetimeStayedMins / 60}h ${totalLifetimeStayedMins % 60}m total",
                            style = MaterialTheme.typography.labelSmall,
                            color = StrictAmberBright
                        )
                    }
                }

                Surface(
                    color = StrictAmberBright.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isLocked) "LOCKED" else "READY",
                        style = MaterialTheme.typography.labelSmall,
                        color = StrictAmberBright,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLocked) {
                val hrs = remainingSec / 3600
                val mins = (remainingSec % 3600) / 60
                val secs = remainingSec % 60

                Surface(
                    color = Color(0xFF0E0A04),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "REMAINING STRICT LOCK WINDOW",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = StrictAmberBright
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Current session stayed: ${currentStayedMins}m • Rules & blockers cannot be turned off",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onRequestEmergencyUnlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_strict_unlock_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StrictAmberBright.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Emergency Unlock",
                        tint = StrictAmberBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verify Master Challenge Code to Unlock Early",
                        color = StrictAmberBright
                    )
                }
            } else {
                Text(
                    text = "Choose how long you commit to stay in Strict Mode:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val durations = listOf(
                    30 to "30m",
                    60 to "1h",
                    240 to "4h",
                    480 to "8h",
                    1440 to "24h"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    durations.forEach { (mins, label) ->
                        FilterChip(
                            selected = selectedMinutes == mins,
                            onClick = { onSelectMinutes(mins) },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StrictAmberBright,
                                selectedLabelColor = Color(0xFF1E1300)
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = preventTamper,
                        onCheckedChange = onTogglePreventTamper
                    )
                    Text(
                        text = "Block Android Settings / Force Stop tampering",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = disableSnooze,
                        onCheckedChange = onToggleDisableSnooze
                    )
                    Text(
                        text = "Remove 5-Minute Access snooze option completely",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onActivateStrict,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("activate_strict_mode_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StrictAmberBright,
                        contentColor = Color(0xFF1E1300)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Engage Strict Mode",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Engage Strict Mode Lock (${selectedMinutes}m)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CoreProtectionMatrixCard(
    settings: AegisSettingsState,
    onToggleWebsiteBlocker: (Boolean) -> Unit,
    onToggleIncognitoBlocker: (Boolean) -> Unit,
    onToggleYoutubeShorts: (Boolean) -> Unit,
    onToggleInstagramReels: (Boolean) -> Unit,
    onToggleAiGuardian: (Boolean) -> Unit,
    onToggleNotificationSummary: (Boolean) -> Unit,
    onQuickTestIncognito: () -> Unit,
    onQuickTestReels: () -> Unit,
    onQuickTestNotificationSummary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Active Self-Control & Wellbeing Shields",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            ShieldToggleRow(
                icon = Icons.Default.PublicOff,
                accentColor = AegisEmeraldBright,
                title = "Website & Keyword Blocker",
                subtitle = "Blocks restricted domains & obfuscated keywords across all browsers",
                checked = settings.websiteKeywordBlockerEnabled,
                testTag = "toggle_website_keyword_blocker",
                onCheckedChange = onToggleWebsiteBlocker
            )

            Spacer(modifier = Modifier.height(10.dp))

            ShieldToggleRow(
                icon = Icons.Default.VisibilityOff,
                accentColor = StrictAmberBright,
                title = "Universal Incognito Blocker",
                subtitle = "Prevents opening Incognito / Private tabs in Chrome, Brave, Firefox, Edge",
                checked = settings.incognitoBlockerEnabled,
                testTag = "toggle_incognito_blocker",
                onCheckedChange = onToggleIncognitoBlocker,
                actionPillLabel = "Test Incognito Block",
                onActionPillClick = onQuickTestIncognito
            )

            Spacer(modifier = Modifier.height(10.dp))

            ShieldToggleRow(
                icon = Icons.Default.PlayCircleFilled,
                accentColor = AlertCrimson,
                title = "Reels & YouTube Shorts Blocker",
                subtitle = "Intercepts YouTube Shorts, Instagram Reels, TikTok & Facebook Reels",
                checked = settings.youtubeShortsBlockerEnabled || settings.instagramReelsBlockerEnabled,
                testTag = "toggle_reels_shorts_blocker",
                onCheckedChange = { enabled ->
                    onToggleYoutubeShorts(enabled)
                    onToggleInstagramReels(enabled)
                },
                actionPillLabel = "Test Shorts Block",
                onActionPillClick = onQuickTestReels
            )

            Spacer(modifier = Modifier.height(10.dp))

            ShieldToggleRow(
                icon = Icons.Default.NotificationsActive,
                accentColor = AegisCyan,
                title = "AI Notification Summary",
                subtitle = "Condenses all notifications at once into the Notification Panel (${settings.activeLlmDisplayBadge})",
                checked = settings.notificationSummaryEnabled,
                testTag = "toggle_notification_summary",
                onCheckedChange = onToggleNotificationSummary,
                actionPillLabel = "Summarize & Show in Panel Now",
                onActionPillClick = onQuickTestNotificationSummary
            )

            Spacer(modifier = Modifier.height(10.dp))

            ShieldToggleRow(
                icon = Icons.Default.AutoAwesome,
                accentColor = VioletAi,
                title = "AI Screen Context Guardian (Optional)",
                subtitle = "Reads screen hierarchy via Accessibility & decides to block or continue",
                checked = settings.aiGuardianEnabled,
                testTag = "toggle_ai_guardian",
                onCheckedChange = onToggleAiGuardian
            )
        }
    }
}

@Composable
private fun ShieldToggleRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
    actionPillLabel: String? = null,
    onActionPillClick: (() -> Unit)? = null
) {
    Surface(
        color = ObsidianSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.testTag(testTag),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF022C22),
                        checkedTrackColor = accentColor
                    )
                )
            }
            if (actionPillLabel != null && onActionPillClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    onClick = onActionPillClick,
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = actionPillLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InterceptionLogItemCard(log: InterceptionLogEntity) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = remember(log.timestamp) { formatter.format(Date(log.timestamp)) }

    val badgeColor = when (log.triggerType) {
        "INCOGNITO" -> StrictAmberBright
        "REELS_SHORTS" -> AlertCrimson
        "AI_CONTEXT" -> VioletAi
        else -> AegisEmeraldBright
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = log.triggerType,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.appName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = log.matchedRule,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (log.screenSnippet.isNotBlank()) {
                Text(
                    text = log.screenSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (log.aiReasoning.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.aiReasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = AegisCyan
                )
            }
        }
    }
}

@Composable
private fun EmergencyStrictUnlockDialog(
    requiredCode: String,
    onDismiss: () -> Unit,
    onUnlockAttempt: (String) -> Boolean
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Strict Mode Master Challenge") },
        text = {
            Column {
                Text(
                    text = "Type your exact Master Challenge Code to unlock Strict Mode early:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = requiredCode,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = StrictAmberBright
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    label = { Text("Challenge Code") },
                    singleLine = true
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = AlertCrimson
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!onUnlockAttempt(input)) {
                        error = "Incorrect Challenge Code."
                    }
                }
            ) {
                Text("Verify & Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Stay Locked")
            }
        }
    )
}
