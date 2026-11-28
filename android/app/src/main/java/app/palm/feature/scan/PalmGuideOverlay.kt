package app.palm.feature.scan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import app.palm.contract.HandSide
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings

/** Figma HAND outline, viewBox 0 0 200 260, thumb on the left (UI-SPEC S08 layer 3; our own art). */
private const val HAND =
    "M60 250C40 210 30 180 28 150L22 100C20 88 36 84 40 96L50 130L48 50C48 36 66 36 66 50L70 120L74 28C74 14 94 14 94 28" +
        "L94 118L102 36C102 22 122 22 122 36L118 124L132 66C134 52 152 56 150 70L138 150C134 190 124 222 116 250"

private val Halo = NocturneColors.SurfaceScrim.copy(alpha = 0.35f)

/**
 * S08 overlay: dashed palm outline (mirrored for the right hand), corner brackets, scan line and focus ring.
 * Every animated value is read in the draw lambdas only, so the loop neither recomposes nor fires
 * accessibility layout events (03-05 HeroOrb lesson). [freeTop] / [freeBottom] bound the guide region in px.
 */
@Composable
internal fun PalmGuideOverlay(
    hand: HandSide,
    detected: Boolean,
    busy: Boolean,
    focusPoint: Pair<Float, Float>?,
    freeTop: () -> Float,
    freeBottom: () -> Float,
    modifier: Modifier = Modifier,
) {
    val still = LocalNocturne.current.reduceMotion
    val fade = if (still) snap<Color>() else tween(NocturneDurations.Quick, easing = NocturneEasings.Standard)
    val outline = animateColorAsState(
        if (detected) NocturneColors.StatusSuccess.copy(alpha = 0.9f) else NocturneColors.TextPrimary.copy(alpha = 0.45f),
        fade,
        label = "outline",
    )
    val corners = animateColorAsState(if (detected) NocturneColors.StatusSuccess else NocturneColors.BrandLink, fade, label = "corners")

    // the screen's one loop: composed only while searching, idle and motion is allowed
    val sweep: State<Float>? = if (!detected && !busy && !still) {
        rememberInfiniteTransition(label = "scanLine").animateFloat(
            0f,
            1f,
            infiniteRepeatable(tween(NocturneDurations.AmbientFast, easing = NocturneEasings.GentleSine), RepeatMode.Reverse),
            label = "sweep",
        )
    } else {
        null
    }

    val ringScale = remember { Animatable(1f) }
    val ringAlpha = remember { Animatable(0f) }
    var ringAt by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            ringAt = Offset(focusPoint.first, focusPoint.second)
            ringAlpha.snapTo(1f)
            if (still) {
                ringScale.snapTo(1f)
            } else {
                ringScale.snapTo(1.2f)
                ringScale.animateTo(1f, tween(NocturneDurations.Quick, easing = NocturneEasings.EmphasizedDecelerate))
            }
        } else if (still) {
            ringAlpha.snapTo(0f)
        } else {
            ringAlpha.animateTo(0f, tween(NocturneDurations.Quick, easing = NocturneEasings.EmphasizedAccelerate))
        }
    }

    Spacer(
        modifier.drawWithCache {
            val top = freeTop()
            val freeH = freeBottom() - top
            val inset = 16.dp.toPx()
            val boxW = minOf(0.72f * size.width, (freeH - 2 * inset) / 1.3f)
            val box = if (boxW > 0f) {
                Rect(Offset((size.width - boxW) / 2f, top + (freeH - boxW * 1.3f) / 2f), Size(boxW, boxW * 1.3f))
            } else {
                null
            }
            val handPath = box?.let { handPath(it, mirror = hand == HandSide.RIGHT) }
            val cornerBox = box?.inflate(inset)
            val cornerPath = cornerBox?.let { cornerPath(it, 32.dp.toPx(), 8.dp.toPx()) }
            val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx()))
            val thin = 2.dp.toPx()
            val thick = 4.dp.toPx()
            val haloExtra = 4.dp.toPx()
            val lineBrush = cornerBox?.let {
                Brush.horizontalGradient(listOf(Color.Transparent, NocturneColors.BrandLink, Color.Transparent), it.left, it.right)
            }
            val ringRadius = 32.dp.toPx()
            onDrawBehind {
                if (handPath != null) {
                    strokePath(handPath, thin + haloExtra, Halo, dash)
                    strokePath(handPath, thin, outline.value, dash)
                }
                if (cornerPath != null) {
                    strokePath(cornerPath, thick + haloExtra, Halo, null)
                    strokePath(cornerPath, thick, corners.value, null)
                }
                val s = sweep
                if (s != null && cornerBox != null && lineBrush != null) {
                    val y = cornerBox.top + s.value * (cornerBox.height - thin)
                    drawRect(lineBrush, Offset(cornerBox.left, y), Size(cornerBox.width, thin))
                }
                val a = ringAlpha.value
                if (a > 0f) {
                    val r = ringRadius * ringScale.value
                    drawCircle(Halo.copy(alpha = Halo.alpha * a), r, ringAt, style = Stroke(thin + haloExtra))
                    drawCircle(NocturneColors.TextPrimary.copy(alpha = a), r, ringAt, style = Stroke(thin))
                }
            }
        },
    )
}

private fun DrawScope.strokePath(path: Path, width: Float, color: Color, effect: PathEffect?) =
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect))

private fun handPath(box: Rect, mirror: Boolean): Path {
    val s = box.width / 200f
    val m = android.graphics.Matrix().apply {
        setScale(if (mirror) -s else s, s)
        postTranslate(if (mirror) box.right else box.left, box.top) // scaleX = -1 about the box centre
    }
    return PathParser().parsePathString(HAND).toPath().also { it.asAndroidPath().transform(m) }
}

/** Four L-shaped brackets with rounded bends on [r]. */
private fun cornerPath(r: Rect, arm: Float, bend: Float) = Path().apply {
    for ((cx, cy) in listOf(r.left to r.top, r.right to r.top, r.right to r.bottom, r.left to r.bottom)) {
        val dx = if (cx == r.left) 1f else -1f
        val dy = if (cy == r.top) 1f else -1f
        moveTo(cx, cy + dy * arm)
        lineTo(cx, cy + dy * bend)
        quadraticTo(cx, cy, cx + dx * bend, cy)
        lineTo(cx + dx * arm, cy)
    }
}
