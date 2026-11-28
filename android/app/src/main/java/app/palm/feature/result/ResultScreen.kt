package app.palm.feature.result

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.R
import app.palm.contract.PalmReadingDto
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneStateBlock
import app.palm.core.designsystem.components.NocturneTopBar

/** S10 Result (D-07): photo with lines, legend, disclaimer, the reading and the actions. */
@Composable
fun ResultScreen(
    readingId: String,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
    onAskGuide: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    LaunchedEffect(readingId) { viewModel.load(readingId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = NocturneSpace.S16),
    ) {
        NocturneTopBar(stringResource(R.string.result_title), onBack = onBack)
        when (val s = state) {
            ResultState.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(48.dp), color = NocturneColors.BrandLink)
            }
            ResultState.Missing -> Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = NocturneSpace.S24),
                verticalArrangement = Arrangement.Center,
            ) {
                NocturneStateBlock(
                    painterResource(R.drawable.ic_alert),
                    NocturneColors.BrandLink,
                    stringResource(R.string.result_missing_title),
                    stringResource(R.string.result_missing_body),
                    primary = stringResource(R.string.common_back_home) to onBack,
                )
            }
            is ResultState.Found -> Found(s.reading, s.photo, onScanAgain, onAskGuide, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Found(reading: PalmReadingDto, photo: PhotoState, onScanAgain: () -> Unit, onAskGuide: () -> Unit, modifier: Modifier) {
    val type = LocalNocturne.current.type
    val maxPhotoHeight = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height * 0.55f).toDp() }
    val image = (photo as? PhotoState.Ready)?.image
    val ratio = if (image != null) {
        image.width.toFloat() / image.height
    } else {
        reading.imageSize.w.toFloat() / reading.imageSize.h
    }.takeIf { it.isFinite() && it > 0f } ?: (3f / 4f)
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = NocturneSpace.S24)) {
        PalmPhoto(photo, reading.lines, Modifier.fillMaxWidth().heightIn(max = maxPhotoHeight).aspectRatio(ratio))
        if (image != null) { // D-12: no legend while the photo is loading or unavailable
            Spacer(Modifier.height(NocturneSpace.S8))
            LineLegend(reading.lines)
        }
        Spacer(Modifier.height(NocturneSpace.S16))
        Text(stringResource(R.string.result_disclaimer), style = type.caption, color = NocturneColors.TextMuted)
        Spacer(Modifier.height(NocturneSpace.S16))
        reading.palm?.let { Text(it.summary, style = type.bodyL, color = NocturneColors.TextPrimary) }
        reading.lines.sortedBy { it.type.ordinal }.forEach { line ->
            Spacer(Modifier.height(NocturneSpace.S16))
            Column(verticalArrangement = Arrangement.spacedBy(NocturneSpace.S4)) {
                Text(
                    line.label.ifBlank { stringResource(lineFullNameRes(line.type)) },
                    style = type.titleM,
                    color = NocturneColors.TextPrimary,
                )
                Text(line.interpretation, style = type.bodyL, color = NocturneColors.TextMuted)
            }
        }
        Spacer(Modifier.height(NocturneSpace.S24))
        ResultActions(reading, image, onScanAgain, onAskGuide)
        Spacer(Modifier.height(NocturneSpace.S32))
    }
}
