package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AegisEmeraldBright,
    onPrimary = Color(0xFF022C22),
    primaryContainer = AegisEmeraldContainer,
    onPrimaryContainer = OnAegisEmeraldContainer,
    secondary = AegisCyan,
    onSecondary = Color(0xFF083344),
    secondaryContainer = AegisCyanContainer,
    onSecondaryContainer = OnAegisCyanContainer,
    tertiary = StrictAmberBright,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = StrictAmberContainer,
    onTertiaryContainer = OnStrictAmberContainer,
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = OnAlertCrimsonContainer,
    background = ObsidianBg,
    onBackground = SlateTextPrimary,
    surface = ObsidianSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateOutline,
    surfaceContainerLow = Color(0xFF0B1324),
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianSurfaceElevated
)

private val LightColorScheme = lightColorScheme(
    primary = DeepEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = DeepCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = DeepAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),
    background = AlabasterBg,
    onBackground = InkTextPrimary,
    surface = AlabasterSurface,
    onSurface = InkTextPrimary,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = InkTextSecondary,
    outline = Color(0xFFCBD5E1)
)

val AegisShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AegisShapes,
        content = content
    )
}
