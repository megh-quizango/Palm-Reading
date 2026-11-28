package app.palm.feature.result.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.contract.PalmReadingDto
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBackground
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneChip
import app.palm.feature.result.PalmPhoto
import app.palm.feature.result.PhotoState
import app.palm.feature.result.drawnLines
import app.palm.feature.result.lineColor
import app.palm.feature.result.lineNameRes
import kotlinx.coroutines.flow.first

/**
 * The share image (UI-SPEC section 4): 432 x 540 dp at density 2.5 and font scale 1 = 1080 x 1350 px on every phone.
 * The only maxLines in Phase 5 live here (fixed-size image); the summary yields space first, the disclaimer is never cut.
 */
@Composable
fun ShareCard(reading: PalmReadingDto, photo: PhotoState, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalDensity provides Density(2.5f, fontScale = 1f)) {
        val type = LocalNocturne.current.type
        val drawn = drawnLines(reading.lines)
        val legend = photo !is PhotoState.Unavailable && drawn.isNotEmpty()
        Box(modifier.requiredSize(432.dp, 540.dp).clearAndSetSemantics {}) {
            NocturneBackground(animate = false) {
                Column(Modifier.fillMaxSize().padding(NocturneSpace.S24)) {
                    Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.illus_guide_orb), null, Modifier.size(40.dp))
                        Spacer(Modifier.width(NocturneSpace.S8))
                        Text(stringResource(R.string.app_name), style = type.displayM, color = NocturneColors.TextPrimary, maxLines = 1)
                    }
                    Spacer(Modifier.height(NocturneSpace.S16))
                    Row(Modifier.height(264.dp)) {
                        PalmPhoto(photo, reading.lines, Modifier.size(176.dp, 264.dp), shape = RoundedCornerShape(NocturneRadius.Md))
                        Spacer(Modifier.width(NocturneSpace.S16))
                        Column(Modifier.width(192.dp)) {
                            Text(
                                stringResource(R.string.share_card_title), style = type.displayM, color = NocturneColors.TextPrimary,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            if (legend) {
                                Spacer(Modifier.height(NocturneSpace.S8))
                                Column(verticalArrangement = Arrangement.spacedBy(NocturneSpace.S4)) {
                                    drawn.forEach { NocturneChip(stringResource(lineNameRes(it.type)), dotColor = lineColor(it.type)) }
                                }
                                Spacer(Modifier.height(NocturneSpace.S8))
                                Text(
                                    stringResource(R.string.result_legend_note), style = type.caption, color = NocturneColors.TextMuted,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(NocturneSpace.S16))
                    Text(
                        reading.palm?.summary.orEmpty(), Modifier.weight(1f), style = type.bodyL, color = NocturneColors.TextPrimary,
                        maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                    val traits = reading.palm?.traits.orEmpty().take(3)
                    if (traits.isNotEmpty()) {
                        Spacer(Modifier.height(NocturneSpace.S8))
                        Text(
                            traits.joinToString(" · "), style = type.caption, color = NocturneColors.AccentGold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(NocturneSpace.S16))
                    Text(
                        stringResource(R.string.result_disclaimer), style = type.caption, color = NocturneColors.TextMuted,
                        maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(NocturneSpace.S8))
                    Text(stringResource(R.string.share_card_footer), style = type.caption, color = NocturneColors.AccentGold, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Records [ShareCard] into a GraphicsLayer and hands back the image; nothing is drawn on screen (never drawLayer) and the
 * zero-size host takes no space. Captures after the layer was really recorded, not after a frame count (RESEARCH Pattern 6).
 */
@Composable
fun ShareCapture(reading: PalmReadingDto, photo: PhotoState, onCaptured: (ImageBitmap) -> Unit) {
    val layer = rememberGraphicsLayer()
    var recorded by remember { mutableStateOf(false) }
    val callback by rememberUpdatedState(onCaptured)
    Box(Modifier.size(0.dp).clearAndSetSemantics {}) {
        ShareCard(
            reading, photo,
            Modifier.drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                recorded = true
            },
        )
    }
    LaunchedEffect(Unit) {
        snapshotFlow { recorded }.first { it }
        withFrameNanos { }
        callback(layer.toImageBitmap())
    }
}
