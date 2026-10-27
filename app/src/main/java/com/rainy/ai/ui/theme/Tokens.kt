// RAINY_STAGE1
package com.rainy.ai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 4-dp based spacing scale. Use these instead of raw dp values in new UI. */
object RainySpace {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

object RainyRadius {
    val sm = 10.dp
    val md = 14.dp
    val lg = 20.dp
    val xl = 28.dp
    val pill = 999.dp
}

/** Minimum touch target (Material / WCAG guidance). */
object RainyTouch {
    val min = 48.dp
}

object RainyOrbSize {
    val hero = 168.dp
    val medium = 96.dp
    val compact = 40.dp
}

object RainyMotion {
    const val fastMs = 150
    const val mediumMs = 300
    const val slowMs = 600
    const val orbStateMs = 520
}

/**
 * Cinematic midnight-rain palette.
 *
 * The neutral values intentionally match the existing Material color scheme in Theme.kt
 * (and the window background in themes.xml), so adding this palette changes nothing on
 * screen by itself. Orb/state colors are new and are not used by any current screen.
 *
 * State colors differ in hue AND the orb also differs in motion per state, so state is
 * never communicated by color alone.
 */
@Immutable
class RainyPalette(
    // neutrals
    val midnight: Color = Color(0xFF0B0D10),
    val abyss: Color = Color(0xFF080A0D),
    val glass: Color = Color(0xFF111418),
    val glassHigh: Color = Color(0xFF181C21),
    val hairline: Color = Color(0xFF2B3238),
    val textPrimary: Color = Color(0xFFE4E7EA),
    val textSecondary: Color = Color(0xFFA9B1B9),
    val textTertiary: Color = Color(0xFF7B8794),
    // brand + orb states
    val rain: Color = Color(0xFF8BB4D4),
    val listening: Color = Color(0xFF4FD6C4),
    val thinking: Color = Color(0xFFA08CFF),
    val speaking: Color = Color(0xFF5EC8FF),
    val error: Color = Color(0xFFFF8A80)
)

val LocalRainyPalette = staticCompositionLocalOf { RainyPalette() }

/** Access point: `Rainy.palette.rain` inside any composable under RainyTheme. */
object Rainy {
    val palette: RainyPalette
        @Composable @ReadOnlyComposable get() = LocalRainyPalette.current
}
