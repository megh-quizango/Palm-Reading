package app.palm.core.designsystem

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Positions are in Figma units of the 360 x 800 "Illustration/Star field" frame; radii in dp at 360 dp width.

/** A twinkling dot: rest [alpha], animated between [lo] and [hi] with its own [phase] (0..1). */
internal data class SkyStar(
    val x: Float, val y: Float, val radiusDp: Float, val alpha: Float,
    val lo: Float, val hi: Float, val periodMs: Int, val phase: Float,
)

internal data class SkySparkle(val x: Float, val y: Float, val radiusDp: Float, val phase: Float)

internal data class SkyBlob(val x: Float, val y: Float, val rx: Float, val ry: Float, val color: Color)

internal data class SkyPoint(val x: Float, val y: Float)

internal data class Sky(
    val nebula: List<SkyBlob>,
    val far: List<SkyStar>,
    val mid: List<SkyStar>,
    val constellations: List<List<SkyPoint>>,
    val sparkles: List<SkySparkle>,
)

private const val SEED = 20261126 // fixed so the sky is the same on every launch
private const val TWINKLE_FLOOR = 0.25f
private const val MID_TWINKLE_MS = 3000 // Figma layer value, no token
private const val CONSTELLATION_MS = 6000 // Figma layer value, no token
internal const val SHOOTING_STAR_MS = 900 // Figma layer value, no token
private const val SHOOTING_FADE_OUT_MS = 400 // Figma layer value, no token
private const val SPARKLE_STAGGER_MS = 400 // Figma layer value, no token
internal const val SPARKLE_COUNT = 7
internal const val SPARKLE_CYCLE_MS = SPARKLE_COUNT * (NocturneDurations.AmbientFast + SPARKLE_STAGGER_MS).toLong()

internal const val REST_CONSTELLATION_ALPHA = 0.45f
internal const val REST_NEBULA_ALPHA = 0.85f
internal const val REST_SPARKLE_SCALE = 1f

private val SparkleGold = lerp(NocturneColors.AccentGold, NocturneColors.TextOnPrimary, 0.7f)

internal fun makeSky(): Sky {
    val r = Random(SEED)
    fun x() = r.nextFloat() * 360f
    fun y() = r.nextFloat() * 800f
    val far = List(55) { i ->
        val a = 0.25f + 0.3f * r.nextFloat()
        SkyStar(x(), y(), 0.8f, a, TWINKLE_FLOOR, a, NocturneDurations.Ambient, (i % 4) / 4f)
    }
    val mid = List(26) { SkyStar(x(), y(), 1.3f, 0.5f + 0.4f * r.nextFloat(), 0.5f, 0.9f, MID_TWINKLE_MS, r.nextFloat()) }
    val sparkles = List(SPARKLE_COUNT) { SkySparkle(x(), y(), 3.5f + 3f * r.nextFloat(), r.nextFloat()) }
    return Sky(
        nebula = listOf(
            SkyBlob(310f, 150f, 230f, 190f, NocturneColors.BrandPrimary.copy(alpha = 0.45f)),
            SkyBlob(30f, 520f, 210f, 180f, NocturneGradients.HeroAurora[2].second.copy(alpha = 0.30f)),
            SkyBlob(270f, 770f, 200f, 150f, NocturneColors.PalmHead.copy(alpha = 0.25f)),
        ),
        far = far,
        mid = mid,
        constellations = listOf(
            listOf(SkyPoint(228f, 228f), SkyPoint(262f, 210f), SkyPoint(298f, 226f), SkyPoint(312f, 262f), SkyPoint(286f, 286f)),
            listOf(SkyPoint(40f, 640f), SkyPoint(70f, 618f), SkyPoint(104f, 630f)),
        ),
        sparkles = sparkles,
    )
}

/** GentleSine ping-pong 0 -> 1 -> 0 over 2 x [periodMs], shifted by [phase] periods. */
private fun pingPong(tMs: Long, periodMs: Int, phase: Float = 0f): Float {
    val f = ((tMs % (2L * periodMs)).toFloat() / periodMs + 2f * phase) % 2f
    return NocturneEasings.GentleSine.transform(if (f < 1f) f else 2f - f)
}

internal fun twinkleAlpha(star: SkyStar, tMs: Long): Float =
    star.lo + (star.hi - star.lo) * pingPong(tMs, star.periodMs, star.phase)

/** Index of the one sparkle animating at [tMs], or -1 during the stagger gap. */
internal fun activeSparkle(tMs: Long): Int {
    val slot = NocturneDurations.AmbientFast + SPARKLE_STAGGER_MS
    val local = tMs % SPARKLE_CYCLE_MS
    return if (local % slot < NocturneDurations.AmbientFast) (local / slot).toInt() else -1
}

/** Progress 0..1 of the active sparkle at [tMs]. */
private fun sparkleProgress(tMs: Long): Float =
    (tMs % (NocturneDurations.AmbientFast + SPARKLE_STAGGER_MS)).toFloat() / NocturneDurations.AmbientFast

/** 1 -> 1.15 -> 1 -> 0.8 -> 1 over one turn, so a sparkle never pops at its start or end. */
internal fun sparkleScale(p: Float): Float {
    val s = sin(2f * PI.toFloat() * p)
    return REST_SPARKLE_SCALE + s * (if (s > 0f) 0.15f else 0.2f)
}

// Seeded gaps between shooting stars (8-12 s); the first value is the delay after the sky starts
private val ShotGaps = Random(SEED + 1).let { r -> LongArray(17) { 8_000L + r.nextLong(4_001L) } }
private val ShotCycle = ShotGaps.drop(1).sum()

/** Start time of the [k]th shooting star in ms of sky time. */
internal fun shootingStarStart(k: Int): Long {
    val n = ShotGaps.size - 1
    var t = ShotGaps[0] + (k / n) * ShotCycle
    for (i in 1..(k % n)) t += ShotGaps[i]
    return t
}

/** Progress 0..1 of the visible shooting star at [tMs], or -1 when none is on screen. */
internal fun shootingStar(tMs: Long): Float {
    if (tMs < ShotGaps[0]) return -1f
    var local = (tMs - ShotGaps[0]) % ShotCycle
    for (i in 1 until ShotGaps.size) {
        if (local < SHOOTING_STAR_MS) return local.toFloat() / SHOOTING_STAR_MS
        local -= ShotGaps[i]
        if (local < 0) return -1f
    }
    return -1f
}

private fun shootingAlpha(p: Float): Float {
    val ms = p * SHOOTING_STAR_MS
    return when {
        ms < NocturneDurations.Instant -> ms / NocturneDurations.Instant
        ms > SHOOTING_STAR_MS - SHOOTING_FADE_OUT_MS -> (SHOOTING_STAR_MS - ms) / SHOOTING_FADE_OUT_MS
        else -> 1f
    }
}

/** Draws one star layer; [t] < 0 is the rest state (own alpha, no drift). */
private fun DrawScope.drawStars(
    stars: List<SkyStar>, centers: List<Offset>, color: Color, driftPx: Float, dp: Float, t: Long, theta: Float,
) {
    val rest = t < 0L
    for (i in stars.indices) {
        val s = stars[i]
        val a = theta + s.phase * 2f * PI.toFloat()
        val d = if (rest) Offset.Zero else Offset(sin(a) * driftPx, (cos(a) - 1f) * driftPx)
        drawCircle(color, s.radiusDp * dp, centers[i] + d, if (rest) s.alpha else twinkleAlpha(s, t))
    }
}

/** Four-point sparkle of radius 1 around the origin; scaled at draw time. */
private fun sparklePath() = Path().apply {
    val w = 0.28f
    moveTo(0f, -1f)
    quadraticTo(w * 0.3f, -w * 0.3f, 1f, 0f)
    quadraticTo(w * 0.3f, w * 0.3f, 0f, 1f)
    quadraticTo(-w * 0.3f, w * 0.3f, -1f, 0f)
    quadraticTo(-w * 0.3f, -w * 0.3f, 0f, -1f)
    close()
}

/**
 * The app's one full-bleed sky: the sky gradient plus six layers (nebula, far stars, mid stars,
 * constellations, near sparkles, shooting star) per 03-UI-SPEC "Background (FND-03)".
 * One elapsed-time clock drives every layer and is read only in the draw phase, so a frame never recomposes.
 * Static rest state, with no clock composed, when [animate] is false or reduce motion / battery saver is on.
 * The root calls it once behind NavDisplay; camera and generating routes pass animate = false and paint
 * their own opaque bg.camera. Draws behind the system bars; content handles its own insets.
 */
@Composable
fun NocturneBackground(modifier: Modifier = Modifier, animate: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val sky = remember { makeSky() }
    val clock: MutableLongState? = if (animate && !LocalNocturne.current.reduceMotion) {
        remember { mutableLongStateOf(0L) }.also { c ->
            // Infinite-animation frames do not hold Compose test idling
            LaunchedEffect(c) {
                val t0 = withInfiniteAnimationFrameMillis { it } - c.longValue
                while (true) withInfiniteAnimationFrameMillis { c.longValue = it - t0 }
            }
        }
    } else null
    Box(
        modifier.fillMaxSize().drawWithCache {
            val u = size.width / 360f // px per Figma unit; radii and drift scale with width
            val v = size.height / 800f
            val dp = u // UI-SPEC: dp sizes scale by size.width / 360 dp, so one spec dp is u px
            fun at(x: Float, y: Float) = Offset(x * u, y * v)
            val skyBrush = nocturneBrush(NocturneGradients.Sky, NocturneGradientAngles.Sky)
            val nebula = sky.nebula.map { b ->
                Brush.radialGradient(listOf(b.color, b.color.copy(alpha = 0f)), at(b.x, b.y), b.rx * u)
            }
            val far = sky.far.map { at(it.x, it.y) }
            val mid = sky.mid.map { at(it.x, it.y) }
            val lines = sky.constellations.map { line -> line.map { at(it.x, it.y) } }
            val sparkleAt = sky.sparkles.map { at(it.x, it.y) }
            val sparkle = sparklePath()
            val glow = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), Offset.Zero, 2.2f)
            val shotTail = at(230f, 112f)
            val shotHead = at(322f, 70f)
            val shotLen = (shotHead - shotTail).getDistance()
            val shotDir = (shotHead - shotTail) / shotLen
            val shotBrush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.95f)), shotTail, shotHead,
            )
            onDrawBehind {
                val t = clock?.longValue ?: -1L // the only animated read, here in the draw phase; -1 is rest (unboxed)
                val rest = t < 0L
                drawRect(skyBrush)

                // L1 nebula: Lissajous drift (x = sin, y = sin 2 / 2) on the 40 s orbit
                val theta = if (rest) 0f else 2f * PI.toFloat() * (t % NocturneDurations.AmbientSlow) / NocturneDurations.AmbientSlow
                val nebulaAlpha = if (rest) REST_NEBULA_ALPHA else 0.7f + 0.3f * pingPong(t, NocturneDurations.AmbientSlow)
                val nDx = if (rest) 0f else sin(theta) * 24f * dp
                val nDy = if (rest) 0f else sin(2f * theta) / 2f * 24f * dp
                for (i in sky.nebula.indices) {
                    val b = sky.nebula[i]
                    val c = at(b.x, b.y)
                    translate(nDx, nDy) {
                        scale(1f, b.ry / b.rx, c) { drawCircle(nebula[i], b.rx * u, c, alpha = nebulaAlpha) }
                    }
                }

                // L2/L3 stars: shared 40 s orbit, amplitude 4 / 8 dp is the parallax
                drawStars(sky.far, far, NocturneColors.TextOnPrimary, 4f * dp, dp, t, theta)
                drawStars(sky.mid, mid, NocturneColors.TextPrimary, 8f * dp, dp, t, theta)

                // L4 constellations: lines and nodes pulse together
                val lineAlpha = if (rest) REST_CONSTELLATION_ALPHA else 0.3f + 0.3f * pingPong(t, CONSTELLATION_MS)
                for (l in lines.indices) {
                    val line = lines[l]
                    for (i in 1 until line.size) drawLine(NocturneColors.BrandLink, line[i - 1], line[i], dp, StrokeCap.Round, alpha = lineAlpha)
                    for (i in line.indices) {
                        drawCircle(NocturneColors.TextPrimary, (if (i == 0) 2f else 1.8f) * dp, line[i], alpha = lineAlpha / 0.6f)
                    }
                }

                // L5 near sparkles: one at a time, 16 dp drift
                val active = if (rest) -1 else activeSparkle(t)
                val p = if (rest) 0f else sparkleProgress(t)
                for (i in sky.sparkles.indices) {
                    val s = sky.sparkles[i]
                    val a = theta + s.phase * 2f * PI.toFloat()
                    val c = sparkleAt[i] + if (rest) Offset.Zero else Offset(sin(a) * 16f * dp, (cos(a) - 1f) * 16f * dp)
                    val k = if (i == active) sparkleScale(p) else REST_SPARKLE_SCALE
                    val deg = if (i == active) 15f * sin(PI.toFloat() * p) else 0f
                    translate(c.x, c.y) {
                        scale(s.radiusDp * dp * k, Offset.Zero) {
                            drawCircle(glow, 2.2f, Offset.Zero)
                            rotate(deg, Offset.Zero) { drawPath(sparkle, SparkleGold, alpha = 0.9f) }
                        }
                    }
                }

                // L6 shooting star: at most one, slides 120 dp along its own line
                val shot = if (rest) -1f else shootingStar(t)
                if (shot >= 0f) {
                    val d = shotDir * (shot * 120f * dp)
                    val alpha = shootingAlpha(shot)
                    translate(d.x, d.y) {
                        drawLine(shotBrush, shotTail, shotHead, 1.6f * dp, StrokeCap.Round, alpha = alpha)
                        drawCircle(Color.White, 2.2f * dp, shotHead, alpha = 0.95f * alpha)
                    }
                }
            }
        },
    ) {
        // Own layer: a star frame re-records only this Box, not the content (and its glows)
        Box(Modifier.fillMaxSize().graphicsLayer()) {
            CompositionLocalProvider(LocalContentColor provides NocturneSemantic.Foreground) { content() }
        }
    }
}
