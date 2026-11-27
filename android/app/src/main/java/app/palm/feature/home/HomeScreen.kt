package app.palm.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.FeatureCard
import app.palm.core.designsystem.devanagariSafe
import app.palm.core.navigation.Feature
import java.time.LocalTime

/** S06 Home: header, hero, feature grid; Back asks before leaving (D-11..D-13). */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onScan: () -> Unit,
    onFeature: (Feature) -> Unit,
    onGreetingLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var askExit by rememberSaveable { mutableStateOf(false) }
    // Innermost enabled handler, so it wins over NavDisplay's pop on the one-entry stack (D-13)
    BackHandler(enabled = !askExit) { askExit = true }
    if (askExit) ExitDialog(onStay = { askExit = false })

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val p = profile ?: return
    val nocturne = LocalNocturne.current
    val type = nocturne.type
    val entry = rememberEntryClock(still = nocturne.reduceMotion)
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, top = NocturneSpace.S8, bottom = NocturneSpace.S24),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .entryMotion(entry, index = 0, rise = false)
                .then(
                    // Debug-only hook (D-03): no affordance and no semantics action
                    if (onGreetingLongPress != null) {
                        Modifier.pointerInput(onGreetingLongPress) { detectTapGestures(onLongPress = { onGreetingLongPress() }) }
                    } else {
                        Modifier
                    },
                ),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(greetingRes(dayPart(LocalTime.now())), p.firstName),
                style = type.overline.devanagariSafe(),
                color = NocturneSemantic.MutedForeground,
            )
            Text(
                stringResource(R.string.app_name),
                style = type.displayL,
                color = NocturneSemantic.Foreground,
                modifier = Modifier.semantics { heading() },
            )
        }
        Spacer(Modifier.height(NocturneSpace.S16))
        HeroCard(onScan, Modifier.entryMotion(entry, index = 1))
        Spacer(Modifier.height(NocturneSpace.S24))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_sparkle), null, Modifier.size(24.dp), tint = NocturneColors.AccentGold)
            Spacer(Modifier.width(NocturneSpace.S8))
            Text(
                stringResource(R.string.home_explore),
                style = type.displayM,
                color = NocturneSemantic.Foreground,
                modifier = Modifier.semantics { heading() },
            )
        }
        Spacer(Modifier.height(NocturneSpace.S16))
        Column(verticalArrangement = Arrangement.spacedBy(NocturneSpace.S16)) {
            GridRows.forEach { row ->
                // Tiles in a row share the taller one's height
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S16)) {
                    row.forEach { feature ->
                        FeatureCard(
                            title = stringResource(feature.titleRes),
                            modifier = Modifier.weight(1f).fillMaxHeight().entryMotion(entry, index = 2 + feature.ordinal),
                            onClick = { if (feature == Feature.PALM) onScan() else onFeature(feature) },
                            illustration = painterResource(feature.artRes),
                            badge = if (feature == Feature.PALM) null else stringResource(R.string.home_soon_badge),
                        )
                    }
                }
            }
        }
    }
}

// Once per process: coming back to Home (or rotating) does not replay the entry motion
private var entryPlayed = false

private const val STAGGER_MS = 60f
private const val ENTRY_END_MS = NocturneDurations.Standard + STAGGER_MS * (2 + 7)

/** One clock in ms drives the whole stagger; item i starts at i * 60 ms and runs Standard. */
@Composable
private fun rememberEntryClock(still: Boolean): Animatable<Float, *> {
    val clock = remember { Animatable(if (entryPlayed || still) ENTRY_END_MS else 0f) }
    LaunchedEffect(Unit) {
        entryPlayed = true
        clock.animateTo(ENTRY_END_MS, tween(ENTRY_END_MS.toInt(), easing = LinearEasing))
    }
    return clock
}

/** Fade (and a 16 dp rise) read in the graphicsLayer lambda, so the stagger never recomposes Home. */
private fun Modifier.entryMotion(clock: Animatable<Float, *>, index: Int, rise: Boolean = true) = graphicsLayer {
    val f = NocturneEasings.EmphasizedDecelerate.transform(
        ((clock.value - index * STAGGER_MS) / NocturneDurations.Standard).coerceIn(0f, 1f),
    )
    alpha = f
    if (rise) translationY = (1f - f) * 16.dp.toPx()
}

/** D-12 order; the odd 7th tile (Ask [Guide]) spans both columns. */
private val GridRows = listOf(
    listOf(Feature.PALM, Feature.TAROT),
    listOf(Feature.HOROSCOPE, Feature.BIRTH_CHART),
    listOf(Feature.LOVE_TEST, Feature.DAILY_REPORT),
    listOf(Feature.ASK_GUIDE),
)
