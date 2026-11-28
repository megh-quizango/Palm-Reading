package app.palm.feature.result

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.BuildConfig
import app.palm.R
import app.palm.contract.PalmReadingDto
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings
import app.palm.core.designsystem.NocturneGradientAngles
import app.palm.core.designsystem.NocturneGradients
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSize
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneStateBlock
import app.palm.core.designsystem.components.NocturneTopBar
import app.palm.core.designsystem.nocturneBrush
import java.util.Locale

/** S10 Result (D-07): photo with lines, legend and notes, disclaimer, Palm / Love / Career tabs, the actions. */
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
            is ResultState.Found -> Found(readingId, s.reading, s.photo, onScanAgain, onAskGuide, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Found(
    readingId: String,
    reading: PalmReadingDto,
    photo: PhotoState,
    onScanAgain: () -> Unit,
    onAskGuide: () -> Unit,
    modifier: Modifier,
) {
    val type = LocalNocturne.current.type
    val maxPhotoHeight = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height * 0.55f).toDp() }
    val image = (photo as? PhotoState.Ready)?.image
    val ratio = if (image != null) {
        image.width.toFloat() / image.height
    } else {
        reading.imageSize.w.toFloat() / reading.imageSize.h
    }.takeIf { it.isFinite() && it > 0f } ?: (3f / 4f)
    if (BuildConfig.DEBUG && image != null) {
        LaunchedEffect(image) {
            val (w, h) = reading.imageSize
            if (aspectMismatch(w, h, image.width, image.height)) {
                // Two numbers only: no id, no reading text, no traces (D-18)
                Log.w("Result", String.format(Locale.ROOT, "image_size aspect %.3f vs photo aspect %.3f", w.toFloat() / h, image.width.toFloat() / image.height))
            }
        }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = NocturneSpace.S24)) {
        // wrapContentWidth drops fillMaxWidth's min width, so a photo taller than the cap narrows (centred) instead of overflowing
        PalmPhoto(photo, reading.lines, Modifier.fillMaxWidth().wrapContentWidth().heightIn(max = maxPhotoHeight).aspectRatio(ratio), revealKey = readingId)
        if (image != null) { // D-12: legend and notes only once the photo is drawn
            val anyDrawn = reading.lines.any { it.isDrawn() }
            if (anyDrawn) {
                Spacer(Modifier.height(NocturneSpace.S8))
                LineLegend(reading.lines)
            }
            Spacer(Modifier.height(NocturneSpace.S8))
            Text(
                stringResource(if (anyDrawn) R.string.result_legend_note else R.string.result_no_lines_drawn),
                style = type.caption,
                color = NocturneColors.TextMuted,
            )
        }
        Spacer(Modifier.height(NocturneSpace.S16))
        // Above the tabs, so it is on every tab (RES-04, D-13)
        Text(stringResource(R.string.result_disclaimer), style = type.caption, color = NocturneColors.TextMuted)
        Spacer(Modifier.height(NocturneSpace.S16))
        ResultTabs(tab) { tab = it }
        Spacer(Modifier.height(NocturneSpace.S16))
        Column(verticalArrangement = Arrangement.spacedBy(NocturneSpace.S16)) {
            when (tab) {
                0 -> PalmTab(reading)
                1 -> LoveTab(reading)
                else -> CareerTab(reading)
            }
        }
        Spacer(Modifier.height(NocturneSpace.S24))
        ResultActions(reading, photo, onScanAgain, onAskGuide)
        Spacer(Modifier.height(NocturneSpace.S32))
    }
}

private val PillShape = RoundedCornerShape(NocturneRadius.Pill)
private val TabLabels = listOf(R.string.result_tab_palm, R.string.result_tab_love, R.string.result_tab_career)

/** Figma segmented control on M3 tabs (RESEARCH Pattern 4): Tab role and selected state for TalkBack, the pill drawn inside each tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultTabs(selected: Int, onSelect: (Int) -> Unit) {
    val nocturne = LocalNocturne.current
    val primary = remember { nocturneBrush(NocturneGradients.Primary, NocturneGradientAngles.Primary) }
    PrimaryTabRow(
        selectedTabIndex = selected,
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(NocturneColors.SurfaceCard.copy(alpha = 0.8f), PillShape)
            .border(NocturneBorderWidth, NocturneColors.BorderHairline.copy(alpha = 0.3f), PillShape),
        containerColor = Color.Transparent,
        indicator = {},
        divider = {},
    ) {
        TabLabels.forEachIndexed { i, label ->
            val isSelected = i == selected
            val pill by animateFloatAsState(
                if (isSelected) 1f else 0f,
                if (nocturne.reduceMotion) snap() else tween(NocturneDurations.Quick, easing = NocturneEasings.Standard),
                label = "tab",
            )
            val color = if (isSelected) NocturneColors.TextOnPrimary else NocturneColors.TextMuted
            Tab(
                selected = isSelected,
                onClick = { onSelect(i) },
                modifier = Modifier.height(NocturneSize.TouchMin),
                selectedContentColor = NocturneColors.TextOnPrimary,
                unselectedContentColor = NocturneColors.TextMuted,
            ) {
                Box(
                    Modifier.fillMaxSize().padding(NocturneSpace.S4).background(primary, PillShape, alpha = pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(label), style = nocturne.type.labelM, color = color)
                }
            }
        }
    }
}

@Composable
private fun PalmTab(reading: PalmReadingDto) {
    reading.palm?.let { InfoCard(stringResource(R.string.result_overview), it.summary, it.traits) }
    if (reading.lines.isNotEmpty()) {
        Text(
            stringResource(R.string.result_your_lines),
            style = LocalNocturne.current.type.displayM,
            color = NocturneColors.TextPrimary,
            modifier = Modifier.semantics { heading() },
        )
    }
    orderedLines(reading.lines).forEach { LineCard(it) }
}

@Composable
private fun LoveTab(reading: PalmReadingDto) {
    val love = reading.love ?: return
    InfoCard(stringResource(R.string.result_love_how), love.howYouLove)
    InfoCard(stringResource(R.string.result_love_partner), love.idealPartner)
    InfoCard(stringResource(R.string.result_love_challenges), love.challenges)
}

@Composable
private fun CareerTab(reading: PalmReadingDto) {
    val career = reading.career ?: return
    InfoCard(stringResource(R.string.result_career_strengths), career.strengths)
    InfoCard(stringResource(R.string.result_career_challenges), career.challenges)
    if (career.idealIndustries.isNotEmpty()) InfoCard(stringResource(R.string.result_career_industries), null, career.idealIndustries)
}
