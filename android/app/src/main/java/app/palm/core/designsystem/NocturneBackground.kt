package app.palm.core.designsystem

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** One precomputed star: position as a fraction of the size, radius in dp, twinkle phase 0..1. */
private class Star(val x: Float, val y: Float, val radiusDp: Float, val color: Color, val alpha: Float, val phase: Float, val layer: Int)

/** Per layer (far, mid, near): star count, radius range in dp (D-04: 1-2.5 dp), colour, drift amplitude. */
private class StarLayer(val count: Int, val minDp: Float, val maxDp: Float, val color: Color, val driftDp: Float)

private val Layers = listOf(
    StarLayer(30, 1f, 1.4f, NocturneColors.TextPrimary, 4f),
    StarLayer(20, 1.2f, 1.8f, NocturneColors.BrandLink, 8f),
    StarLayer(10, 1.8f, 2.5f, NocturneColors.TextPrimary, 14f),
)

private const val SEED = 20261126 // fixed so the sky is the same on every launch
private const val MIN_TWINKLE = 0.35f

private fun makeStars(): List<Star> {
    val r = Random(SEED)
    return Layers.flatMapIndexed { i, l ->
        List(l.count) {
            Star(
                x = r.nextFloat(), y = r.nextFloat(),
                radiusDp = l.minDp + r.nextFloat() * (l.maxDp - l.minDp),
                color = l.color, alpha = 0.4f + 0.6f * r.nextFloat(), phase = r.nextFloat(), layer = i,
            )
        }
    }
}

private class StarClock(val twinkle: State<Float>, val twinkleFast: State<Float>, val drift: State<Float>)

@Composable
private fun InfiniteTransition.loop(durationMs: Int, easing: Easing, mode: RepeatMode): State<Float> =
    animateFloat(0f, 1f, infiniteRepeatable(tween(durationMs, easing = easing), mode), label = "stars")

/**
 * Full-bleed Nocturne sky: BgBase plus three star layers drawn in the draw phase (no recomposition per frame).
 * Draws behind the system bars; content handles its own insets. Static when system animations are off.
 * Phase 3 (FND-03) extends this composable; do not use it on the camera or generating screens.
 */
@Composable
fun NocturneBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val stars = remember { makeStars() }
    // reduceMotion is fixed for the process, so this branch never flips within a composition
    val clock = if (LocalNocturne.current.reduceMotion) null else rememberInfiniteTransition(label = "stars").let { t ->
        StarClock(
            twinkle = t.loop(NocturneDurations.Ambient, NocturneEasings.GentleSine, RepeatMode.Reverse),
            twinkleFast = t.loop(NocturneDurations.AmbientFast, NocturneEasings.GentleSine, RepeatMode.Reverse),
            drift = t.loop(NocturneDurations.AmbientSlow, LinearEasing, RepeatMode.Restart),
        )
    }
    Box(
        modifier.fillMaxSize().background(NocturneColors.BgBase).drawBehind {
            // Animated values are read only here, so a frame redraws without recomposing
            val tw = clock?.twinkle?.value
            val twFast = clock?.twinkleFast?.value
            val angle = 2f * PI.toFloat() * (clock?.drift?.value ?: 0f)
            stars.forEach { s ->
                val t = if (s.layer == 1) twFast else tw
                // Triangle wave offset by the star's phase; rest state (no animation) is full brightness
                val glow = if (t == null) 1f else MIN_TWINKLE + (1f - MIN_TWINKLE) * (1f - abs(2f * ((t + s.phase) % 1f) - 1f))
                val amp = Layers[s.layer].driftDp.dp.toPx()
                val a = angle + s.phase * 2f * PI.toFloat()
                val dx = if (clock == null) 0f else sin(a) * amp
                val dy = if (clock == null) 0f else (cos(a) - 1f) * amp
                drawCircle(
                    color = s.color,
                    radius = s.radiusDp.dp.toPx(),
                    center = Offset(s.x * size.width + dx, s.y * size.height + dy),
                    alpha = s.alpha * glow,
                )
            }
        },
    ) {
        // Own layer: a star frame re-records only this Box, not the content (and its glows)
        Box(Modifier.fillMaxSize().graphicsLayer()) {
            CompositionLocalProvider(LocalContentColor provides NocturneSemantic.Foreground) { content() }
        }
    }
}
