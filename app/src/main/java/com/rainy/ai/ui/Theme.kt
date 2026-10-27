package com.rainy.ai.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rainy.ai.ui.theme.LocalRainyPalette
import com.rainy.ai.ui.theme.RainyPalette

// RAINY_STAGE1
private val RainyPaletteDefault = RainyPalette()

private val RainyDark = darkColorScheme(
    primary = Color(0xFF8BB4D4),
    onPrimary = Color(0xFF08131C),
    primaryContainer = Color(0xFF1A2A38),
    onPrimaryContainer = Color(0xFFDCE9F5),
    secondary = Color(0xFF8BB4D4),
    onSecondary = Color(0xFF08131C),
    secondaryContainer = Color(0xFF1A2A38),
    onSecondaryContainer = Color(0xFFDCE9F5),
    background = Color(0xFF0B0D10),
    onBackground = Color(0xFFE4E7EA),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE4E7EA),
    surfaceVariant = Color(0xFF181C21),
    onSurfaceVariant = Color(0xFFA9B1B9),
    surfaceContainerLowest = Color(0xFF0B0D10),
    surfaceContainerLow = Color(0xFF0F1215),
    surfaceContainer = Color(0xFF111418),
    surfaceContainerHigh = Color(0xFF161A1F),
    surfaceContainerHighest = Color(0xFF1B2026),
    outline = Color(0xFF4A535C),
    outlineVariant = Color(0xFF2B3238),
    error = Color(0xFFFF8A80)
)

private val RainyTypography = Typography(
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, letterSpacing = 0.1.sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun RainyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRainyPalette provides RainyPaletteDefault) {
        MaterialTheme(colorScheme = RainyDark, typography = RainyTypography, content = content)
    }
}
