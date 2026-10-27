// RAINY_STAGE1
package com.rainy.ai.ui.components

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.rainy.ai.ui.theme.Rainy
import com.rainy.ai.ui.theme.RainyMotion
import com.rainy.ai.ui.theme.RainyOrbSize
import com.rainy.ai.ui.theme.RainyPalette
import kotlin.math.PI
import kotlin.math.sin

enum class OrbState { Idle, Listening, Thinking, Speaking, Error }

/**
 * Rainy's presence indicator: a glowing core with rain-ripple rings.
 *
 * - Idle: slow breathing, rings drift outward.
 * - Listening: rings contract INWARD toward the core (receptive).
 * - Thinking: rotating arcs around the core.
 * - Speaking: rings expand faster and the core pulses.
 * - Error: dim core with a broken ring.
 *
 * Honesty note: the speaking pulse is a synthetic wave, NOT driven by real audio level.
 * Android's TextToSpeech does not expose output amplitude, so none is claimed.
 *
 * Battery/accessibility: frames only run while the screen is STARTED, and not at all when
 * [animate] is false or system animations are off (a calm static frame is drawn instead).
 */
@Composable
fun RainyOrb(
    state: OrbState,
    modifier: Modifier = Modifier,
    size: Dp = RainyOrbSize.hero,
    animate: Boolean = true,
    label: String? = null
) {
    val palette = Rainy.palette
    val context = LocalContext.current

    // "Remove animations" / animator scale 0 => do not run the frame loop.
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) == 0f
        }.getOrDefault(false)
    }
    val running = animate && !reducedMotion

    // Pause the frame loop whenever the host lifecycle is below STARTED.
    var visible by remember { mutableStateOf(true) }
    LifecycleStartEffect(Unit) {
        visible = true
        onStopOrDispose { visible = false }
    }

    val spec = remember(state, palette) { specFor(state, palette) }
    val accent = animateColorAsState(
        targetValue = spec.accent,
        animationSpec = tween(RainyMotion.orbStateMs, easing = FastOutSlowInEasing),
        label = "orbAccent"
    )
    val glow = orbFloat(spec.glow, "orbGlow")
    val rate = orbFloat(spec.rippleRate, "orbRate")
    val rippleAlpha = orbFloat(spec.rippleAlpha, "orbRippleAlpha")
    val inward = orbFloat(if (spec.inward) 1f else 0f, "orbInward")
    val arcs = orbFloat(spec.arcs, "orbArcs")
    val speak = orbFloat(spec.speak, "orbSpeak")
    val error = orbFloat(spec.error, "orbError")
    val breathAmp = orbFloat(spec.breathAmp, "orbBreath")

    // Phases are accumulated (not derived from absolute time) so changing speed on a state
    // change never makes rings jump. Initial values give a pleasant static frame.
    val clock = remember { mutableFloatStateOf(0.6f) }
    val ripple = remember { mutableFloatStateOf(0.35f) }
    val spin = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(running, visible) {
        if (!running || !visible) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
                last = now
                clock.floatValue += dt
                ripple.floatValue = (ripple.floatValue + dt * rate.value) % 1f
                spin.floatValue = (spin.floatValue + dt * 0.45f * arcs.value) % 1f
            }
        }
    }

    val description = label ?: defaultLabel(state)

    // State reads happen inside the draw lambda, so frames redraw without recomposing.
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = description }
    ) {
        drawOrb(
            accent = accent.value,
            glow = glow.value,
            rippleAlpha = rippleAlpha.value,
            inward = inward.value > 0.5f,
            arcs = arcs.value,
            speak = speak.value,
            error = error.value,
            breathAmp = breathAmp.value,
            t = clock.floatValue,
            ripplePhase = ripple.floatValue,
            spinPhase = spin.floatValue
        )
    }
}

private class OrbSpec(
    val accent: Color,
    val glow: Float,
    val rippleRate: Float,
    val rippleAlpha: Float,
    val inward: Boolean,
    val arcs: Float,
    val speak: Float,
    val error: Float,
    val breathAmp: Float
)

private fun specFor(state: OrbState, p: RainyPalette): OrbSpec = when (state) {
    OrbState.Idle -> OrbSpec(p.rain, 0.55f, 0.10f, 0.30f, false, 0f, 0f, 0f, 0.035f)
    OrbState.Listening -> OrbSpec(p.listening, 0.85f, 0.42f, 0.55f, true, 0f, 0f, 0f, 0.050f)
    OrbState.Thinking -> OrbSpec(p.thinking, 0.70f, 0.12f, 0.18f, false, 1f, 0f, 0f, 0.030f)
    OrbState.Speaking -> OrbSpec(p.speaking, 1.00f, 0.55f, 0.50f, false, 0f, 1f, 0f, 0.020f)
    OrbState.Error -> OrbSpec(p.error, 0.30f, 0.00f, 0.00f, false, 0f, 0f, 1f, 0.020f)
}

private fun defaultLabel(state: OrbState): String = when (state) {
    OrbState.Idle -> "Rainy is ready"
    OrbState.Listening -> "Rainy is listening"
    OrbState.Thinking -> "Rainy is thinking"
    OrbState.Speaking -> "Rainy is speaking"
    OrbState.Error -> "Rainy hit a problem"
}

@Composable
private fun orbFloat(target: Float, label: String): State<Float> = animateFloatAsState(
    targetValue = target,
    animationSpec = tween(RainyMotion.orbStateMs, easing = FastOutSlowInEasing),
    label = label
)

/** Synthetic speech-like pulse in [-1, 1]. Not audio-reactive. */
private fun speakWobble(t: Float): Float =
    sin(t * 7.3f) * 0.5f + sin(t * 11.7f + 1.3f) * 0.3f + sin(t * 4.1f + 0.4f) * 0.2f

private fun DrawScope.drawOrb(
    accent: Color,
    glow: Float,
    rippleAlpha: Float,
    inward: Boolean,
    arcs: Float,
    speak: Float,
    error: Float,
    breathAmp: Float,
    t: Float,
    ripplePhase: Float,
    spinPhase: Float
) {
    val c = center
    val maxR = this.size.minDimension / 2f
    val twoPi = (2.0 * PI).toFloat()
    val breath = sin(t * twoPi * 0.22f)
    val coreR = maxR * 0.30f * (1f + breathAmp * breath + 0.075f * speak * speakWobble(t))

    // 1. Ambient glow
    val glowK = glow * (0.9f + 0.1f * breath)
    drawCircle(
        brush = Brush.radialGradient(
            0.00f to accent.copy(alpha = (0.38f * glowK).coerceIn(0f, 1f)),
            0.45f to accent.copy(alpha = (0.14f * glowK).coerceIn(0f, 1f)),
            1.00f to Color.Transparent,
            center = c,
            radius = maxR
        ),
        radius = maxR,
        center = c
    )

    // 2. Rain ripples (3 staggered rings)
    if (rippleAlpha > 0.01f) {
        val stroke = Stroke(width = 1.5.dp.toPx())
        val inner = coreR * 1.15f
        val outer = maxR * 0.98f
        for (i in 0 until 3) {
            val p = (ripplePhase + i / 3f) % 1f
            val q = if (inward) 1f - p else p
            val env = sin(p * PI.toFloat())
            drawCircle(
                color = accent.copy(alpha = (rippleAlpha * env).coerceIn(0f, 1f)),
                radius = inner + (outer - inner) * q,
                center = c,
                style = stroke
            )
        }
    }

    // 3. Thinking arcs
    if (arcs > 0.01f) {
        drawArcRing(accent.copy(alpha = 0.85f * arcs), c, coreR * 1.6f, spinPhase * 360f, 62f, 3, 120f, 2.dp.toPx())
        drawArcRing(accent.copy(alpha = 0.45f * arcs), c, coreR * 1.95f, -spinPhase * 540f, 28f, 2, 180f, 1.dp.toPx())
    }

    // 4. Error ring (broken)
    if (error > 0.01f) {
        val pulse = 0.75f + 0.25f * sin(t * 1.6f)
        drawArcRing(accent.copy(alpha = (0.7f * error * pulse).coerceIn(0f, 1f)), c, coreR * 1.7f, 20f, 50f, 4, 90f, 2.dp.toPx())
    }

    // 5. Core
    val base = lerp(accent, Color(0xFF05080D), 0.35f * error) // error = dimmer core
    val hi = lerp(base, Color.White, 0.55f)
    val lo = lerp(base, Color(0xFF05080D), 0.60f)
    drawCircle(
        brush = Brush.radialGradient(
            0.00f to hi,
            0.55f to base,
            1.00f to lo,
            center = Offset(c.x - coreR * 0.30f, c.y - coreR * 0.35f),
            radius = coreR * 1.45f
        ),
        radius = coreR,
        center = c
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.10f + 0.10f * glow),
        radius = coreR,
        center = c,
        style = Stroke(width = 1.dp.toPx())
    )

    // 6. Specular highlight
    val hl = Offset(c.x - coreR * 0.34f, c.y - coreR * 0.40f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
            center = hl,
            radius = coreR * 0.45f
        ),
        radius = coreR * 0.45f,
        center = hl
    )
}

private fun DrawScope.drawArcRing(
    color: Color,
    c: Offset,
    radius: Float,
    startAngle: Float,
    sweep: Float,
    count: Int,
    stepAngle: Float,
    strokeWidth: Float
) {
    val topLeft = Offset(c.x - radius, c.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    for (i in 0 until count) {
        drawArc(
            color = color,
            startAngle = startAngle + i * stepAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = style
        )
    }
}
