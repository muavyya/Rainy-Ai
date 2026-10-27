package com.rainy.ai.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RainyDark = darkColorScheme(
    primary = Color(0xFF9CC4E4),
    onPrimary = Color(0xFF0B1620),
    primaryContainer = Color(0xFF1E3347),
    onPrimaryContainer = Color(0xFFDCEBF8),
    secondary = Color(0xFFE59AB4),
    background = Color(0xFF0A0C0F),
    onBackground = Color(0xFFE3E7EB),
    surface = Color(0xFF12161B),
    onSurface = Color(0xFFE3E7EB),
    surfaceVariant = Color(0xFF1B2128),
    onSurfaceVariant = Color(0xFFC3CBD3),
    error = Color(0xFFFF8A80)
)

@Composable
fun RainyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RainyDark, content = content)
}
