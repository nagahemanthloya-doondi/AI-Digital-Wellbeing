package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AegisSettingsState
import com.example.data.preferences.BlockScreenVisualMode
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmeraldBright
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.StrictAmberBright

@Composable
fun BlockScreenStudioScreen(
    settings: AegisSettingsState,
    onSaveStudioConfig: (
        BlockScreenVisualMode,
        String,
        Boolean,
        String,
        String,
        String,
        String,
        String,
        Boolean
    ) -> Unit,
    onPreviewFullScreenBlock: () -> Unit
) {
    val context = LocalContext.current

    var selectedMode by remember(settings.blockScreenMode) { mutableStateOf(settings.blockScreenMode) }
    var challengeCode by remember(settings.defaultChallengeCode) { mutableStateOf(settings.defaultChallengeCode) }
    var useRandomCode by remember(settings.useRandomChallengeCode) { mutableStateOf(settings.useRandomChallengeCode) }
    var headline by remember(settings.blockHeadline) { mutableStateOf(settings.blockHeadline) }
    var message by remember(settings.blockMessage) { mutableStateOf(settings.blockMessage) }
    var customImageUri by remember(settings.customImageUri) { mutableStateOf(settings.customImageUri) }
    var customVideoUri by remember(settings.customVideoUri) { mutableStateOf(settings.customVideoUri) }
    var redirectUrl by remember(settings.redirectUrl) { mutableStateOf(settings.redirectUrl) }
    var autoRedirect by remember(settings.autoRedirectImmediately) { mutableStateOf(settings.autoRedirectImmediately) }

    // Zero-Permission Android Photo Picker for Custom Block Screen Picture
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            customImageUri = uri.toString()
            selectedMode = BlockScreenVisualMode.CUSTOM_IMAGE
        }
    }

    // Zero-Permission Android Video Picker for Custom Block Screen Video
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            customVideoUri = uri.toString()
            selectedMode = BlockScreenVisualMode.CUSTOM_VIDEO
        }
    }

    fun persistCurrentChanges() {
        onSaveStudioConfig(
            selectedMode,
            challengeCode,
            useRandomCode,
            headline,
            message,
            customImageUri,
            customVideoUri,
            redirectUrl,
            autoRedirect
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("block_screen_studio_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Editable Blocking Screen Studio",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Customize the un-bypassable overlay with your own Challenge Code, Picture, Playing Video, or URL Redirect",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Choose Editable Block Screen Mode
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "1. Select Block Screen Visual Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ModeSelectorOptionCard(
                        icon = Icons.Default.Password,
                        title = "Default Challenge Code Mode",
                        subtitle = "Displays your custom discipline code required to unlock 5-minute access",
                        isSelected = selectedMode == BlockScreenVisualMode.DEFAULT_CODE,
                        testTag = "mode_default_code_card",
                        onClick = { selectedMode = BlockScreenVisualMode.DEFAULT_CODE }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ModeSelectorOptionCard(
                        icon = Icons.Default.Image,
                        title = "Custom Picture Block Screen",
                        subtitle = "Displays your chosen gallery photo or motivational guardian artwork",
                        isSelected = selectedMode == BlockScreenVisualMode.CUSTOM_IMAGE,
                        testTag = "mode_custom_image_card",
                        onClick = { selectedMode = BlockScreenVisualMode.CUSTOM_IMAGE }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ModeSelectorOptionCard(
                        icon = Icons.Default.Videocam,
                        title = "Play Video on Block Screen",
                        subtitle = "Automatically plays your selected motivational video when blocked",
                        isSelected = selectedMode == BlockScreenVisualMode.CUSTOM_VIDEO,
                        testTag = "mode_custom_video_card",
                        onClick = { selectedMode = BlockScreenVisualMode.CUSTOM_VIDEO }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ModeSelectorOptionCard(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        title = "Redirect to Specific URL",
                        subtitle = "Redirects browser or offers 1-tap launch to a productive website",
                        isSelected = selectedMode == BlockScreenVisualMode.REDIRECT_URL,
                        testTag = "mode_redirect_url_card",
                        onClick = { selectedMode = BlockScreenVisualMode.REDIRECT_URL }
                    )
                }
            }
        }

        // 2. Mode-Specific Editable Options Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AegisEmeraldBright.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "2. Configure Editable Media & Code Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Always allow editing the Challenge Code (used by Default mode & 5-minute unlock)
                    OutlinedTextField(
                        value = challengeCode,
                        onValueChange = { challengeCode = it },
                        label = { Text("Custom Challenge Code (for Default Screen & 5-Min Unlock)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_challenge_code_input")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = useRandomCode,
                            onCheckedChange = { useRandomCode = it }
                        )
                        Text(
                            text = "Generate random alphanumeric code on each block",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Picture Picker Section
                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Custom Picture Option",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Current: ${if (customImageUri.startsWith("content://")) "Custom Gallery Photo Selected" else customImageUri}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Pick Picture",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick Picture")
                                }
                                FilterChip(
                                    selected = customImageUri == "PRESET_GUARDIAN",
                                    onClick = {
                                        customImageUri = "PRESET_GUARDIAN"
                                        selectedMode = BlockScreenVisualMode.CUSTOM_IMAGE
                                    },
                                    label = { Text("Hourglass Art") }
                                )
                                FilterChip(
                                    selected = customImageUri == "PRESET_HERO",
                                    onClick = {
                                        customImageUri = "PRESET_HERO"
                                        selectedMode = BlockScreenVisualMode.CUSTOM_IMAGE
                                    },
                                    label = { Text("Sanctuary Art") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Video Picker Section
                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Custom Video Player Option",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (customVideoUri.isNotBlank()) {
                                    "Selected Video URI: ${customVideoUri.take(42)}..."
                                } else {
                                    "Built-in Focus Breath Video Stream active (or pick any .mp4 from gallery)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        videoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Pick Video",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick Video from Device")
                                }
                                if (customVideoUri.isNotBlank()) {
                                    OutlinedButton(onClick = { customVideoUri = "" }) {
                                        Text("Use Built-in Stream")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Redirect URL Section
                    OutlinedTextField(
                        value = redirectUrl,
                        onValueChange = { redirectUrl = it },
                        label = { Text("Redirect Destination URL (e.g. https://leetcode.com)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_redirect_url_input")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = autoRedirect,
                            onCheckedChange = { autoRedirect = it }
                        )
                        Text(
                            text = "Automatically redirect browser immediately when blocked",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = headline,
                        onValueChange = { headline = it },
                        label = { Text("Block Screen Headline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Motivational Charter Subtext") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { persistCurrentChanges() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("save_block_studio_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisEmeraldBright,
                                contentColor = Color(0xFF022C22)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Design", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                persistCurrentChanges()
                                onPreviewFullScreenBlock()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("preview_block_studio_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StrictAmberBright,
                                contentColor = Color(0xFF1E1300)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = "Preview")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Block Screen", fontWeight = FontWeight.Bold)
                        }
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
private fun ModeSelectorOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) AegisEmeraldBright.copy(alpha = 0.15f) else ObsidianSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) AegisEmeraldBright else Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) AegisEmeraldBright else AegisCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected Mode",
                    tint = AegisEmeraldBright,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
