package app.palm.feature.scan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings
import app.palm.core.designsystem.NocturneSpace

/** Full-screen scrim while a photo is processed (UI-SPEC Processing state); swallows every touch. */
@Composable
fun ProcessingOverlay(modifier: Modifier = Modifier) {
    val nocturne = LocalNocturne.current
    Box(
        modifier
            .fillMaxSize()
            .background(NocturneColors.SurfaceScrim.copy(alpha = 0.72f))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(NocturneSpace.S16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NocturneSpace.S16),
        ) {
            CircularProgressIndicator(
                Modifier.size(48.dp),
                color = NocturneColors.BrandLink,
                strokeWidth = 4.dp,
                trackColor = NocturneColors.TextPrimary.copy(alpha = 0.12f),
            )
            Text(
                stringResource(R.string.scan_processing),
                style = nocturne.type.titleM,
                color = NocturneColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** Fades [ProcessingOverlay] in and out over Quick / Standard; instant under reduced motion (UI-SPEC Motion). */
@Composable
fun ProcessingOverlayHost(visible: Boolean) {
    val reduce = LocalNocturne.current.reduceMotion
    val spec = tween<Float>(NocturneDurations.Quick, easing = NocturneEasings.Standard)
    AnimatedVisibility(
        visible,
        enter = if (reduce) EnterTransition.None else fadeIn(spec),
        exit = if (reduce) ExitTransition.None else fadeOut(spec),
    ) { ProcessingOverlay() }
}
