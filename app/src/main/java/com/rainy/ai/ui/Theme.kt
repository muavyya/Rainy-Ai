package com.rainy.ai.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun RainyTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFFB39DDB), secondary = Color(0xFFF48FB1))
    } else {
        lightColorScheme(primary = Color(0xFF6A4C93), secondary = Color(0xFFD81B60))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
