package com.rainy.ai.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private class Drop(val x: Float, val phase: Float, val speed: Int, val len: Float)

/** Lightweight rain: about 40 lines, one animated float, drawn in the draw phase only. */
@Composable
fun RainBackground(modifier: Modifier = Modifier) {
    val drops = remember {
        val r = Random(7)
        List(40) {
            Drop(
                x = r.nextFloat(),
                phase = r.nextFloat(),
                speed = 1 + r.nextInt(3),
                len = 14f + r.nextFloat() * 22f
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "rain")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "rainT"
    )
    Canvas(modifier = modifier) {
        val color = Color(0x30B0C4DE)
        for (d in drops) {
            val progress = (t * d.speed + d.phase) % 1f
            val x = d.x * size.width + progress * 24f
            val y = progress * (size.height + 60f) - 30f
            drawLine(
                color = color,
                start = Offset(x, y),
                end = Offset(x + 4f, y + d.len),
                strokeWidth = 1.5f
            )
        }
    }
}
