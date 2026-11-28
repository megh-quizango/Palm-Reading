package app.palm.feature.result

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
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
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSpace

sealed interface PhotoState {
    data object Loading : PhotoState
    data object Unavailable : PhotoState
    data class Ready(val image: ImageBitmap) : PhotoState
}

/** The photo box with the line overlay (D-09, D-10, D-12); fills the box the caller sizes. Shared by Result and the share card. */
@Composable
fun PalmPhoto(
    state: PhotoState,
    lines: List<PalmLineDto>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(NocturneRadius.Lg),
) {
    val drawn = remember(lines) { drawnLines(lines) }
    val names = drawn.map { stringResource(lineNameRes(it.type)) }
    val description = if (names.isEmpty()) {
        stringResource(R.string.result_photo_cd_plain)
    } else {
        stringResource(R.string.result_photo_cd, names.joinToString(", "))
    }
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
                Image(image, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                Canvas(Modifier.fillMaxSize()) {
                    // From the decoded bitmap and the canvas, so letterboxing by the height cap still lines up (D-09)
                    val rect = fitRect(image.width, image.height, size.width, size.height)
                    val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    for (line in drawn) {
                        val path = Path()
                        line.trace.forEachIndexed { i, p ->
                            val (x, y) = rect.map(p[0], p[1])
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, lineColor(line.type), style = stroke)
                    }
                }
            }
        }
    }
}
