package app.palm.feature.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings
import app.palm.core.designsystem.NocturneGradientAngles
import app.palm.core.designsystem.NocturneGradients
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.nocturneBrush
import kotlinx.coroutines.launch

private const val PAGES = 3
private val PanelShape = RoundedCornerShape(NocturneRadius.Xl)
private val PanelMin = 160.dp
private val PanelMax = 340.dp

private data class IntroPage(@StringRes val title: Int, @StringRes val body: Int)

private val Pages = listOf(
    IntroPage(R.string.intro_1_title, R.string.intro_1_body),
    IntroPage(R.string.intro_2_title, R.string.intro_2_body),
    IntroPage(R.string.intro_3_title, R.string.intro_3_body),
)

/** S03: three swipeable intro pages; Next pages on, Get started finishes (D-08). */
@Composable
fun IntroScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalNocturne.current.reduceMotion
    val last = pager.currentPage == PAGES - 1
    Column(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        HorizontalPager(pager, Modifier.weight(1f)) { page -> IntroPageContent(page) }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, top = NocturneSpace.S16, bottom = NocturneSpace.S16),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PageIndicator(pager.currentPage, reduceMotion)
            NocturneButton(
                text = stringResource(if (last) R.string.intro_get_started else R.string.common_next),
                onClick = {
                    if (last) {
                        onFinish()
                    } else {
                        scope.launch {
                            val next = pager.currentPage + 1
                            if (reduceMotion) {
                                pager.scrollToPage(next)
                            } else {
                                pager.animateScrollToPage(next, animationSpec = tween(NocturneDurations.Emphasized, easing = NocturneEasings.Standard))
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun IntroPageContent(page: Int) {
    val type = LocalNocturne.current.type
    val cardBrush = remember { nocturneBrush(NocturneGradients.Card, NocturneGradientAngles.Card) }
    BoxWithConstraints(Modifier.fillMaxSize().padding(start = NocturneSpace.S16, end = NocturneSpace.S16, top = NocturneSpace.S24)) {
        // The text never takes the panel's 160 dp minimum; past that it scrolls (large font scale)
        val textMax = (maxHeight - PanelMin - NocturneSpace.S24).coerceAtLeast(0.dp)
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .heightIn(min = PanelMin, max = PanelMax)
                    .fillMaxHeight()
                    .clip(PanelShape)
                    .background(cardBrush, PanelShape),
                contentAlignment = Alignment.Center,
            ) {
                DashedRing()
                PageArt(page)
            }
            Spacer(Modifier.height(NocturneSpace.S24))
            Column(
                Modifier.fillMaxWidth().heightIn(max = textMax).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Pages[page].title),
                    style = type.displayL,
                    color = NocturneSemantic.Foreground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(NocturneSpace.S8))
                Text(
                    stringResource(Pages[page].body),
                    style = type.bodyL,
                    color = NocturneSemantic.MutedForeground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DashedRing() {
    Canvas(Modifier.size(300.dp)) {
        val stroke = 1.dp.toPx()
        drawCircle(
            color = NocturneColors.BrandLink.copy(alpha = 0.3f),
            radius = size.minDimension / 2 - stroke,
            style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx()))),
        )
    }
}

@Composable
private fun PageArt(page: Int) {
    when (page) {
        0 -> Box(Modifier.fillMaxSize()) {
            Art(R.drawable.illus_constellation_hand, Modifier.size(160.dp, 208.dp).align(Alignment.Center))
            Art(R.drawable.illus_guide_orb, Modifier.align(Alignment.TopEnd).padding(NocturneSpace.S24).size(48.dp))
        }
        1 -> Art(R.drawable.illus_zodiac_wheel, Modifier.size(176.dp))
        else -> Art(R.drawable.illus_guide_orb, Modifier.size(160.dp))
    }
}

@Composable
private fun Art(@DrawableRes id: Int, modifier: Modifier) = Image(painterResource(id), contentDescription = null, modifier = modifier)

@Composable
private fun PageIndicator(current: Int, reduceMotion: Boolean) {
    val description = stringResource(R.string.intro_page_cd, current + 1, PAGES)
    Row(
        Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PAGES) { i ->
            val active = i == current
            val width by animateDpAsState(
                if (active) 24.dp else 8.dp,
                if (reduceMotion) snap() else tween(NocturneDurations.Quick, easing = NocturneEasings.Standard),
                label = "dotWidth",
            )
            val color by animateColorAsState(
                if (active) NocturneColors.BrandLink else NocturneSemantic.MutedForeground.copy(alpha = 0.5f),
                if (reduceMotion) snap() else tween(NocturneDurations.Quick, easing = NocturneEasings.Standard),
                label = "dotColor",
            )
            Box(Modifier.width(width).height(8.dp).background(color, RoundedCornerShape(NocturneRadius.Pill)))
        }
    }
}
