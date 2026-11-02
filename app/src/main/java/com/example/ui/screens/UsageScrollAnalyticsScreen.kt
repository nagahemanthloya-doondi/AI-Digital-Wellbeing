package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScrollTelemetryEntity
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
import java.util.Locale

@Composable
fun UsageScrollAnalyticsScreen(
    permissionStatus: SystemPermissionStatus,
    summary: UsageScrollSummary,
    telemetryList: List<ScrollTelemetryEntity>,
    onSyncDeviceUsage: () -> Unit,
    onToggleAvoidStatus: (ScrollTelemetryEntity) -> Unit,
    onLogSampleScroll: (String, String, Int, Float) -> Unit
) {
    val context = LocalContext.current
    val avoidApps = telemetryList.filter { it.isAvoidMarked }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("usage_scroll_analytics_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Usage, Scroll & Wasted Time",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "How much you scrolled, what to avoid, and time wasted today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (permissionStatus.isUsageAccessGranted) {
                            onSyncDeviceUsage()
                        } else {
                            UsageAnalyticsManager.openUsageAccessSettings(context)
                        }
                    },
                    modifier = Modifier.testTag("sync_usage_stats_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync UsageStatsManager",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (permissionStatus.isUsageAccessGranted) "Sync OS" else "Grant Usage")
                }
            }
        }

        // Infographic Donut & Scroll Distance Comparison Card
        item {
            ScrollAndWastedTimeInfographicCard(summary = summary)
        }

        // 4 KPI Telemetry Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    modifier = Modifier.weight(1f),
                    label = "TIME SCROLLED",
                    value = "${summary.totalScrollMinutes}m",
                    subtext = "${summary.totalScrollSwipes} vertical swipes",
                    accentColor = AegisCyan
                )
                MetricStatCard(
                    modifier = Modifier.weight(1f),
                    label = "TIME WASTED",
                    value = "${summary.wastedMinutes}m",
                    subtext = "On Avoid-marked apps",
                    accentColor = AlertCrimson
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    modifier = Modifier.weight(1f),
                    label = "SCROLL DISTANCE",
                    value = String.format(Locale.US, "%.1f m", summary.totalScrollMeters),
                    subtext = String.format(
                        Locale.US,
                        "%.1fx Statue of Liberty height",
                        summary.totalScrollMeters / 93f
                    ),
                    accentColor = StrictAmberBright
                )
                MetricStatCard(
                    modifier = Modifier.weight(1f),
                    label = "PRODUCTIVE FOCUS",
                    value = "${summary.productiveMinutes}m",
                    subtext = "Integrity Score ${summary.focusHealthScore}%",
                    accentColor = AegisEmeraldBright
                )
            }
        }

        // "What Should You Avoid?" Actionable Recommendations Card
        item {
            WhatYouShouldAvoidCard(
                avoidApps = avoidApps,
                summary = summary
            )
        }

        // Per-App Scroll & Usage Breakdown Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Scroll & Usage Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        onLogSampleScroll(
                            "com.google.android.youtube",
                            "YouTube (Shorts & Feed)",
                            5,
                            24.5f
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianSurfaceElevated,
                        contentColor = AegisCyan
                    ),
                    modifier = Modifier.testTag("simulate_scroll_burst_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Record Scroll Burst",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+5m Scroll Test", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        items(telemetryList, key = { it.packageName }) { item ->
            AppTelemetryBreakdownCard(
                item = item,
                totalScreenMinutes = summary.totalScreenMinutes.coerceAtLeast(1),
                onToggleAvoid = { onToggleAvoidStatus(item) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ScrollAndWastedTimeInfographicCard(summary: UsageScrollSummary) {
    val totalMins = (summary.wastedMinutes + summary.productiveMinutes).coerceAtLeast(1)
    val wastedFraction = (summary.wastedMinutes.toFloat() / totalMins.toFloat()).coerceIn(0.05f, 0.95f)
    val productiveFraction = 1f - wastedFraction

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Daily Attention & Scroll Infographic",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Custom Multi-Arc Donut Chart
                Box(
                    modifier = Modifier.size(145.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(135.dp)) {
                        val strokeWidth = 16.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        // Background track
                        drawArc(
                            color = Color(0xFF1E293B),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )

                        // Wasted Time Arc (Crimson)
                        val wastedSweep = wastedFraction * 360f
                        drawArc(
                            color = AlertCrimson,
                            startAngle = -90f,
                            sweepAngle = wastedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Productive Focus Arc (Emerald)
                        drawArc(
                            color = AegisEmeraldBright,
                            startAngle = -90f + wastedSweep + 6f,
                            sweepAngle = (productiveFraction * 360f - 12f).coerceAtLeast(10f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${summary.totalScreenMinutes}m",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "TOTAL TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LegendStatRow(
                        color = AlertCrimson,
                        title = "Wasted / Avoid Time",
                        value = "${summary.wastedMinutes} mins (${(wastedFraction * 100).toInt()}%)"
                    )
                    LegendStatRow(
                        color = AegisEmeraldBright,
                        title = "Productive / Safe Time",
                        value = "${summary.productiveMinutes} mins (${(productiveFraction * 100).toInt()}%)"
                    )
                    LegendStatRow(
                        color = AegisCyan,
                        title = "Active Thumb Scrolling",
                        value = "${summary.totalScrollMinutes} mins (${String.format(Locale.US, "%.0f", summary.totalScrollMeters)}m)"
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendStatRow(color: Color, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetricStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subtext: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WhatYouShouldAvoidCard(
    avoidApps: List<ScrollTelemetryEntity>,
    summary: UsageScrollSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F0D18)),
        border = androidx.compose.foundation.BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "What You Should Avoid",
                    tint = AlertCrimson,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "What You Should Avoid Right Now",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (avoidApps.isEmpty()) {
                Text(
                    text = "No high-drain apps marked to avoid. Mark distracting apps below to track wasted time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFCDD2)
                )
            } else {
                val topDrain = avoidApps.maxByOrNull { it.scrollDurationMillis + it.foregroundUsageMillis }
                if (topDrain != null) {
                    val mins = maxOf(topDrain.foregroundUsageMillis, topDrain.scrollDurationMillis) / 60_000L
                    Text(
                        text = "1. Primary Dopamine Drain: ${topDrain.appName} (${mins}m wasted, ${String.format(Locale.US, "%.1f", topDrain.estimatedScrollMeters)}m scrolled). Keep Reels/Shorts Blocker active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFE4E6)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "2. Velocity Alert: You have scrolled ${String.format(Locale.US, "%.1f", summary.totalScrollMeters)} meters (${summary.totalScrollSwipes} swipes) today. Avoid vertical feeds after 21:00.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFE4E6)
                )
            }
        }
    }
}

@Composable
private fun AppTelemetryBreakdownCard(
    item: ScrollTelemetryEntity,
    totalScreenMinutes: Int,
    onToggleAvoid: () -> Unit
) {
    val usageMins = (maxOf(item.foregroundUsageMillis, item.scrollDurationMillis) / 60_000L).toInt()
    val scrollMins = (item.scrollDurationMillis / 60_000L).toInt()
    val progress = (usageMins.toFloat() / totalScreenMinutes.toFloat()).coerceIn(0.04f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${usageMins}m screen • ${scrollMins}m scrolled • ${String.format(Locale.US, "%.1f", item.estimatedScrollMeters)}m (${item.scrollEventsCount} swipes)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilterChip(
                    selected = item.isAvoidMarked,
                    onClick = onToggleAvoid,
                    label = {
                        Text(
                            text = if (item.isAvoidMarked) "AVOID (Wasted)" else "SAFE / FOCUS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (item.isAvoidMarked) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AlertCrimson.copy(alpha = 0.22f),
                        selectedLabelColor = AlertCrimson,
                        selectedLeadingIconColor = AlertCrimson
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (item.isAvoidMarked) AlertCrimson else AegisEmeraldBright,
                trackColor = Color(0xFF1E293B)
            )
        }
    }
}
