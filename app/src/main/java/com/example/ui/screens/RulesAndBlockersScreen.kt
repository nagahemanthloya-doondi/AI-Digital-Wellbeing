package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.BlockRuleEntity
import com.example.data.preferences.AegisSettingsState
import com.example.domain.BlockingEngine
import com.example.domain.InstalledBrowserInfo
import com.example.domain.InterceptionDecision
import com.example.domain.ScreenContextSnapshot
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmeraldBright
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.StrictAmberBright

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesAndBlockersScreen(
    settings: AegisSettingsState,
    rules: List<BlockRuleEntity>,
    installedBrowsers: List<InstalledBrowserInfo>,
    onAddRule: (String, String, String) -> Unit,
    onToggleRule: (BlockRuleEntity, Boolean) -> Unit,
    onDeleteRule: (BlockRuleEntity) -> Unit,
    onAddPresetPack: (String) -> Unit,
    onToggleIncognito: (Boolean) -> Unit,
    onToggleYoutubeShorts: (Boolean) -> Unit,
    onToggleInstagramReels: (Boolean) -> Unit,
    onToggleTiktok: (Boolean) -> Unit,
    onToggleFacebookReels: (Boolean) -> Unit,
    onToggleFuzzyMatching: (Boolean) -> Unit,
    onTriggerBlockOverlay: (String, String, String, String) -> Unit
) {
    var inputPattern by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("WEBSITE") }
    var selectedCategory by remember { mutableStateOf("Social") }

    // Live Algorithm Tester state
    var testInputUrlOrText by remember { mutableStateOf("https://m.youtube.com/shorts/c.a.s.i.n.o_viral") }
    var testDecision by remember { mutableStateOf<InterceptionDecision?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("rules_and_blockers_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Websites, Keywords, Incognito & Reels",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Instant cross-browser URL/keyword blocking, Incognito lock, and Short-form feed interception",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Add Website or Keyword Rule Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AegisEmeraldBright.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Add Website or Keyword Rule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = selectedType == "WEBSITE",
                            onClick = { selectedType = "WEBSITE" },
                            label = { Text("Website Domain / URL") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AegisEmeraldBright,
                                selectedLabelColor = Color(0xFF022C22),
                                selectedLeadingIconColor = Color(0xFF022C22)
                            )
                        )
                        FilterChip(
                            selected = selectedType == "KEYWORD",
                            onClick = { selectedType = "KEYWORD" },
                            label = { Text("Screen Keyword") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AegisCyan,
                                selectedLabelColor = Color(0xFF022C22),
                                selectedLeadingIconColor = Color(0xFF022C22)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputPattern,
                        onValueChange = { inputPattern = it },
                        label = {
                            Text(
                                if (selectedType == "WEBSITE") {
                                    "Enter domain or URL (e.g. reddit.com, stake.com)"
                                } else {
                                    "Enter keyword or phrase (e.g. casino, gossip, nsfw)"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_rule_pattern_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val categories = listOf("Social", "Adult/NSFW", "Gambling", "News/Doomscroll", "Gaming", "Custom")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (inputPattern.isNotBlank()) {
                                onAddRule(inputPattern, selectedType, selectedCategory)
                                inputPattern = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_rule_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisEmeraldBright,
                            contentColor = Color(0xFF022C22)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Rule")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add ${if (selectedType == "WEBSITE") "Website" else "Keyword"} to Instant Blocker",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Anti-obfuscation toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Anti-Obfuscation & Leetspeak Algorithm",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Catches c.a.s.i.n.o, n\$fw, c4s1n0, and subdomain tricks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.aggressiveFuzzyMatching,
                            onCheckedChange = onToggleFuzzyMatching
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick-Add Category Packs:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Adult/NSFW", "Gambling", "Social", "Gaming").forEach { pack ->
                            OutlinedButton(
                                onClick = { onAddPresetPack(pack) },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("+ $pack Pack", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // 2. Incognito Mode & Browser Coverage Card
        item {
            IncognitoAndBrowserCoverageCard(
                incognitoEnabled = settings.incognitoBlockerEnabled,
                installedBrowsers = installedBrowsers,
                onToggleIncognito = onToggleIncognito
            )
        }

        // 3. Reels & YouTube Shorts Blocker Matrix Card
        item {
            ReelsAndShortsBlockerCard(
                settings = settings,
                onToggleYoutubeShorts = onToggleYoutubeShorts,
                onToggleInstagramReels = onToggleInstagramReels,
                onToggleTiktok = onToggleTiktok,
                onToggleFacebookReels = onToggleFacebookReels
            )
        }

        // 4. Interactive Algorithm Test Sandbox
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AegisCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Algorithm Sandbox",
                            tint = AegisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Blocker Algorithm Tester",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Test any URL, leetspeak keyword, or Shorts link against your active rules:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = testInputUrlOrText,
                        onValueChange = { testInputUrlOrText = it },
                        label = { Text("Sample URL or Screen Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val snapshot = ScreenContextSnapshot(
                                    packageName = "com.android.chrome",
                                    appName = "Google Chrome",
                                    detectedUrl = testInputUrlOrText,
                                    visibleTexts = listOf(testInputUrlOrText)
                                )
                                testDecision = BlockingEngine.evaluateScreenContext(
                                    snapshot = snapshot,
                                    settings = settings,
                                    enabledRules = rules.filter { it.isEnabled }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisCyan,
                                contentColor = Color(0xFF022C22)
                            )
                        ) {
                            Text("Evaluate Input", fontWeight = FontWeight.Bold)
                        }
                        if (testDecision?.shouldBlock == true) {
                            Button(
                                onClick = {
                                    val d = testDecision!!
                                    onTriggerBlockOverlay(
                                        d.triggerType,
                                        d.matchedRuleLabel,
                                        d.appName,
                                        d.snippet
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AlertCrimson,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Launch Block")
                            }
                        }
                    }

                    if (testDecision != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val d = testDecision!!
                        Surface(
                            color = if (d.shouldBlock) AlertCrimson.copy(alpha = 0.16f) else AegisEmeraldBright.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (d.shouldBlock) {
                                        "VERDICT: INSTANT BLOCK (${d.triggerType})"
                                    } else {
                                        "VERDICT: SAFE TO ACCESS"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (d.shouldBlock) AlertCrimson else AegisEmeraldBright,
                                    fontWeight = FontWeight.Bold
                                )
                                if (d.shouldBlock) {
                                    Text(
                                        text = d.matchedRuleLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Active Website & Keyword Rules Header
        item {
            Text(
                text = "Configured Website & Keyword Rules (${rules.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(rules, key = { it.id }) { rule ->
            BlockRuleItemCard(
                rule = rule,
                isStrictLocked = settings.isStrictCurrentlyLocked,
                onToggle = { enabled -> onToggleRule(rule, enabled) },
                onDelete = { onDeleteRule(rule) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IncognitoAndBrowserCoverageCard(
    incognitoEnabled: Boolean,
    installedBrowsers: List<InstalledBrowserInfo>,
    onToggleIncognito: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Incognito Mode Blocker",
                        tint = StrictAmberBright,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Universal Incognito Mode Blocker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Blocks Incognito, Private, Secret & InPrivate tabs across all browsers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = incognitoEnabled,
                    onCheckedChange = onToggleIncognito,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1E1300),
                        checkedTrackColor = StrictAmberBright
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Protected Browser Engines (URL Bar + Incognito Node Inspection):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                installedBrowsers.take(10).forEach { browser ->
                    Surface(
                        color = if (browser.isInstalledOnDevice) {
                            AegisEmeraldBright.copy(alpha = 0.16f)
                        } else {
                            ObsidianSurfaceElevated
                        },
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (browser.isInstalledOnDevice) AegisEmeraldBright else AegisCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = browser.appName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelsAndShortsBlockerCard(
    settings: AegisSettingsState,
    onToggleYoutubeShorts: (Boolean) -> Unit,
    onToggleInstagramReels: (Boolean) -> Unit,
    onToggleTiktok: (Boolean) -> Unit,
    onToggleFacebookReels: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = "Reels and Shorts Blocker",
                    tint = AlertCrimson,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reels & YouTube Shorts Blocker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Intercepts vertical short-form video players while allowing normal messaging or search",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            ShortPlatformToggleItem(
                name = "YouTube Shorts (App & Browser)",
                detail = "Detects reel_player_page_container & youtube.com/shorts",
                checked = settings.youtubeShortsBlockerEnabled,
                onCheckedChange = onToggleYoutubeShorts
            )
            ShortPlatformToggleItem(
                name = "Instagram Reels",
                detail = "Detects clips_viewer_view_pager & instagram.com/reels",
                checked = settings.instagramReelsBlockerEnabled,
                onCheckedChange = onToggleInstagramReels
            )
            ShortPlatformToggleItem(
                name = "TikTok Short-Form Feed",
                detail = "Blocks TikTok vertical feed app & web player",
                checked = settings.tiktokBlockerEnabled,
                onCheckedChange = onToggleTiktok
            )
            ShortPlatformToggleItem(
                name = "Facebook Reels",
                detail = "Detects fb_shorts & facebook.com/reel",
                checked = settings.facebookReelsBlockerEnabled,
                onCheckedChange = onToggleFacebookReels
            )
        }
    }
}

@Composable
private fun ShortPlatformToggleItem(
    name: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun BlockRuleItemCard(
    rule: BlockRuleEntity,
    isStrictLocked: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = if (rule.ruleType == "WEBSITE") AegisEmeraldBright else AegisCyan

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = typeColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = rule.ruleType,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rule.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${rule.hitCount} hits",
                        style = MaterialTheme.typography.labelSmall,
                        color = StrictAmberBright
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rule.pattern,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle
                )
                IconButton(
                    onClick = onDelete,
                    enabled = !isStrictLocked
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Rule",
                        tint = if (isStrictLocked) Color(0xFF475569) else AlertCrimson
                    )
                }
            }
        }
    }
}
