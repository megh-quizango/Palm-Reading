package app.palm.feature.scan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.R
import app.palm.contract.HandSide
import app.palm.contract.InvalidReason
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.components.NocturneButtonStyle
import app.palm.core.designsystem.components.NocturneStateBlock
import app.palm.core.designsystem.components.NocturneTopBar
import app.palm.core.photo.PhotoOrigin
import app.palm.feature.handselect.handLabel

private val PhotoShape = RoundedCornerShape(NocturneRadius.Lg)

/** UI-SPEC ScanReady "Photo gone": no broken image, one way back to the scanner. */
@Composable
private fun PhotoGone(onRetake: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, bottom = NocturneSpace.S16),
    ) {
        NocturneTopBar(stringResource(R.string.scan_ready_title), onBack = onRetake)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = NocturneSpace.S24),
            verticalArrangement = Arrangement.Center,
        ) {
            NocturneStateBlock(
                painterResource(R.drawable.ic_camera),
                NocturneColors.BrandLink,
                stringResource(R.string.scan_ready_gone_title),
                stringResource(R.string.scan_ready_gone_body),
                primary = stringResource(R.string.scan_retake) to onRetake,
            )
        }
    }
}

/** ScanReady (D-01, D-03): the processed photo with its 21 landmarks and "Read my palm"; Back = Retake (D-12). */
@Composable
fun ScanReadyScreen(
    onRetake: () -> Unit,
    onSaved: (String) -> Unit,
    onInvalid: (InvalidReason, HandSide) -> Unit,
    viewModel: ScanReadyViewModel = hiltViewModel(),
) {
    val capture = viewModel.capture
    val readState by viewModel.readState.collectAsStateWithLifecycle()
    val busy = readState == ReadState.Reading || readState is ReadState.Saved || readState is ReadState.Invalid
    val retake = {
        if (!busy) {
            viewModel.retake()
            onRetake()
        }
    }
    // Always enabled, so Back while reading is swallowed instead of popping the screen (RESEARCH Pitfall 2)
    BackHandler(onBack = retake)
    LaunchedEffect(readState) {
        val saved = readState as? ReadState.Saved ?: return@LaunchedEffect
        onSaved(saved.readingId)
        viewModel.consumeSaved()
    }
    LaunchedEffect(readState) {
        val invalid = readState as? ReadState.Invalid ?: return@LaunchedEffect
        onInvalid(invalid.reason, invalid.hand)
        viewModel.consumeInvalid()
    }
    val gone by viewModel.gone.collectAsStateWithLifecycle()
    if (capture == null || gone) {
        PhotoGone(retake)
        return
    }

    val type = LocalNocturne.current.type
    val bitmap by viewModel.bitmap.collectAsStateWithLifecycle()
    val maxPhotoHeight = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height * 0.6f).toDp() }
    val photoCd = stringResource(R.string.scan_ready_photo_cd, capture.landmarks.size)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, bottom = NocturneSpace.S16),
    ) {
        NocturneTopBar(stringResource(R.string.scan_ready_title), onBack = retake)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = NocturneSpace.S24),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val image = bitmap
            Box(
                Modifier
                    .heightIn(max = maxPhotoHeight)
                    .aspectRatio(if (image == null) 4f / 3f else capture.width.toFloat() / capture.height)
                    .clip(PhotoShape)
                    .border(NocturneBorderWidth, NocturneColors.BorderHairline.copy(alpha = 0.35f), PhotoShape),
                contentAlignment = Alignment.Center,
            ) {
                if (image == null) {
                    CircularProgressIndicator(Modifier.size(48.dp), color = NocturneColors.BrandLink)
                } else {
                    Image(
                        image,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().semantics { contentDescription = photoCd },
                    )
                    // The box has the photo's own aspect ratio, so Fit fills it and points map straight onto it
                    Canvas(Modifier.fillMaxSize()) {
                        val r = 4.dp.toPx()
                        for (p in capture.landmarks) {
                            val c = Offset(p.x * size.width, p.y * size.height)
                            drawCircle(NocturneColors.BrandLink, r, c)
                            drawCircle(NocturneColors.BgBase, r, c, style = Stroke(1.dp.toPx()))
                        }
                    }
                }
            }
            Spacer(Modifier.height(NocturneSpace.S16))
            Text(
                stringResource(
                    R.string.scan_ready_source,
                    stringResource(handLabel(capture.hand)),
                    stringResource(if (capture.origin == PhotoOrigin.CAMERA) R.string.scan_source_camera else R.string.scan_source_gallery),
                ),
                style = type.titleM,
                color = NocturneColors.TextPrimary,
            )
            Spacer(Modifier.height(NocturneSpace.S8))
            Text(
                stringResource(R.string.scan_ready_meta, capture.landmarks.size, capture.width, capture.height),
                style = type.caption,
                color = NocturneColors.TextMuted,
            )
        }
        Spacer(Modifier.height(NocturneSpace.S16))
        if (readState == ReadState.Failed) {
            Text(
                stringResource(R.string.scan_ready_error),
                style = type.caption,
                color = NocturneColors.StatusError,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(NocturneSpace.S8))
        }
        NocturneButton(
            stringResource(
                when (readState) {
                    ReadState.Failed -> R.string.scan_ready_retry
                    ReadState.Idle -> R.string.scan_ready_read
                    else -> R.string.scan_ready_reading
                },
            ),
            onClick = viewModel::read,
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        )
        Spacer(Modifier.height(NocturneSpace.S8))
        NocturneButton(
            stringResource(R.string.scan_retake),
            onClick = retake,
            modifier = Modifier.fillMaxWidth(),
            style = NocturneButtonStyle.Ghost,
            enabled = !busy,
        )
    }
}
