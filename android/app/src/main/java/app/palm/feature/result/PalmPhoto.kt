package app.palm.feature.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.contract.PalmLineDto
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSpace

sealed interface PhotoState {
    data object Loading : PhotoState
    data object Unavailable : PhotoState
    data class Ready(val image: ImageBitmap) : PhotoState
}

/** D-10 asks for about 30%; the UI-SPEC measured 0.55 for light-skin contrast. One constant, so 0.30 is a one-line change. */
private const val HALO_ALPHA = 0.55f
private const val STAGGER_MS = 120
private const val LINE_MS = NocturneDurations.Emphasized

/**
 * The photo box with the line overlay (D-09, D-10, D-12); fills the box the caller sizes. Shared by Result and the share card.
 * A non-null [revealKey] draws the lines in once per key (after the photo fade); null draws them complete and static.
 */
@Composable
fun PalmPhoto(
    state: PhotoState,
    lines: List<PalmLineDto>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(NocturneRadius.Lg),
    revealKey: String? = null,
) {
    val drawn = remember(lines) { drawnLines(lines) }
    val names = drawn.map { stringResource(lineNameRes(it.type)) }
    val description = if (names.isEmpty()) {
        stringResource(R.string.result_photo_cd_plain)
    } else {
        stringResource(R.string.result_photo_cd, names.joinToString(", "))
    }
    // Saved per entry: not replayed on tab switch, rotation, return from the share sheet or process recreation
    var played by rememberSaveable(revealKey) { mutableStateOf(false) }
    val animate = revealKey != null && !played && !LocalNocturne.current.reduceMotion
    Box(
        modifier
            .clip(shape)
            .background(NocturneColors.BgDeep)
            .border(NocturneBorderWidth, NocturneColors.BorderHairline.copy(alpha = 0.35f), shape)
            // Only Ready describes itself: the placeholders read their spinner or visible caption (UI-SPEC Accessibility)
            .then(if (state is PhotoState.Ready) Modifier.semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            PhotoState.Loading -> CircularProgressIndicator(Modifier.size(48.dp), color = NocturneColors.BrandLink)
            PhotoState.Unavailable -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_gallery), null, Modifier.size(40.dp), tint = NocturneColors.TextMuted)
                Spacer(Modifier.height(NocturneSpace.S8))
                Text(
                    stringResource(R.string.result_photo_unavailable),
                    style = LocalNocturne.current.type.caption,
                    color = NocturneColors.TextMuted,
                )
            }
            is PhotoState.Ready -> {
                val image = state.image
                val total = STAGGER_MS * (drawn.size - 1).coerceAtLeast(0) + LINE_MS
                val fade = remember { Animatable(if (animate) 0f else 1f) }
                val reveal = remember { Animatable(if (animate) 0f else 1f) }
                LaunchedEffect(animate) {
                    if (animate) {
                        fade.animateTo(1f, tween(NocturneDurations.Quick, easing = NocturneEasings.Standard))
                        reveal.animateTo(1f, tween(total, easing = LinearEasing))
                        played = true
                    } else {
                        fade.snapTo(1f)
                        reveal.snapTo(1f)
                    }
                }
                // Animated values are read only inside draw lambdas: the reveal never recomposes or relayouts (HeroCard lesson)
                Image(
                    image, contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().drawWithContent {
                        drawContent()
                        if (fade.value < 1f) drawRect(NocturneColors.BgDeep, alpha = 1f - fade.value)
                    },
                )
                Spacer(
                    Modifier.fillMaxSize().drawWithCache {
                        // From the decoded bitmap and the canvas, so letterboxing by the height cap still lines up (D-09)
                        val rect = fitRect(image.width, image.height, size.width, size.height)
                        val paths = drawn.map { linePath(it, rect) }
                        val measures = paths.map { p -> PathMeasure().apply { setPath(p, false) } }
                        val partial = Path()
                        val halo = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        onDrawBehind {
                            val t = reveal.value * total
                            drawn.forEachIndexed { i, line ->
                                val p = ((t - STAGGER_MS * i) / LINE_MS).coerceIn(0f, 1f)
                                if (p <= 0f) return@forEachIndexed
                                val path = if (p >= 1f) {
                                    paths[i]
                                } else {
                                    partial.reset()
                                    val m = measures[i]
                                    m.getSegment(0f, m.length * NocturneEasings.EmphasizedDecelerate.transform(p), partial, true)
                                    partial
                                }
                                drawPath(path, NocturneColors.BgDeep, alpha = HALO_ALPHA, style = halo)
                                drawPath(path, lineColor(line.type), style = stroke)
                            }
                        }
                    },
                )
            }
        }
    }
}

/** At most 64 points, clamped and mapped onto the fit rect, smoothed with Catmull-Rom (T-05-08, RESEARCH Pattern 2). */
private fun linePath(line: PalmLineDto, rect: FitRect): Path {
    val points = capPoints(line.trace).map { rect.map(it[0], it[1]) }
    val px = FloatArray(points.size) { points[it].first }
    val py = FloatArray(points.size) { points[it].second }
    return Path().apply {
        moveTo(px[0], py[0])
        catmullRom(px, py).forEach { c -> cubicTo(c.c1x, c.c1y, c.c2x, c.c2y, c.x, c.y) }
    }
}
