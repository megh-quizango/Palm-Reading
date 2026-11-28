package app.palm.feature.scan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.contract.HandSide
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.vision.GuidanceState

private data class ChipContent(val icon: Int, val message: Int, val hint: Int?, val tone: Color?)

private fun chipContent(ui: ScannerUi): ChipContent {
    val hint = when (ui.wrongHand) {
        HandSide.LEFT -> R.string.scan_hint_wrong_hand_left
        HandSide.RIGHT -> R.string.scan_hint_wrong_hand_right
        null -> null
    }
    // tone null = Neutral (text.primary icon, hairline border)
    return when {
        ui.guideUnavailable -> ChipContent(R.drawable.ic_alert, R.string.scan_guide_unavailable, null, NocturneColors.StatusError)
        ui.captureFailed -> ChipContent(R.drawable.ic_alert, R.string.scan_guide_capture_failed, hint, NocturneColors.StatusError)
        else -> when (ui.shown) {
            GuidanceState.NoHand -> ChipContent(R.drawable.ic_hand, R.string.scan_guide_no_hand, hint, null)
            GuidanceState.BackOfHand -> ChipContent(R.drawable.ic_hand, R.string.scan_guide_back_of_hand, hint, NocturneColors.StatusWarning)
            GuidanceState.ClosedHand -> ChipContent(R.drawable.ic_hand, R.string.scan_guide_closed_hand, hint, NocturneColors.StatusWarning)
            GuidanceState.TooFar -> ChipContent(R.drawable.ic_search, R.string.scan_guide_too_far, hint, NocturneColors.StatusWarning)
            GuidanceState.Moving -> ChipContent(R.drawable.ic_hand, R.string.scan_guide_moving, hint, null)
            GuidanceState.PalmDetected -> ChipContent(R.drawable.ic_check, R.string.scan_guide_palm_detected, hint, NocturneColors.StatusSuccess)
        }
    }
}

/** S08 guidance chip (D-02, D-07): one merged polite live region that changes only with the shown state or hint. */
@Composable
internal fun GuidanceChip(ui: ScannerUi, modifier: Modifier = Modifier) {
    val still = LocalNocturne.current.reduceMotion
    val content = chipContent(ui)
    val border = animateColorAsState(
        content.tone?.copy(alpha = 0.9f) ?: NocturneColors.BorderHairline.copy(alpha = 0.5f),
        if (still) snap() else tween(NocturneDurations.Quick, easing = NocturneEasings.Standard),
        label = "chipBorder",
    )
    val pulse = remember { Animatable(1f) }
    val detected = ui.shown == GuidanceState.PalmDetected && !ui.captureFailed && !ui.guideUnavailable
    LaunchedEffect(detected) {
        if (detected && !still) {
            val half = tween<Float>(NocturneDurations.Standard / 2, easing = NocturneEasings.Standard)
            pulse.animateTo(1.04f, half)
            pulse.animateTo(1f, half)
        }
    }
    val shape = RoundedCornerShape(NocturneRadius.Lg)
    AnimatedContent(
        targetState = content,
        transitionSpec = {
            if (still) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val spec = tween<Float>(NocturneDurations.Quick, easing = NocturneEasings.Standard)
                fadeIn(spec) togetherWith fadeOut(spec)
            }
        },
        contentAlignment = Alignment.Center,
        label = "chip",
        modifier = modifier
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .background(NocturneColors.SurfaceScrim.copy(alpha = 0.72f), shape)
            .border(NocturneBorderWidth, border.value, shape)
            .padding(horizontal = NocturneSpace.S16, vertical = NocturneSpace.S8),
    ) { c ->
        // the outgoing content must not be read with the incoming one during the cross-fade
        val leaving = if (transition.targetState == EnterExitState.PostExit) Modifier.clearAndSetSemantics { } else Modifier
        Row(leaving, horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(c.icon), contentDescription = null, tint = c.tone ?: NocturneColors.TextPrimary, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(NocturneSpace.S4)) {
                Text(stringResource(c.message), style = LocalNocturne.current.type.titleM, color = NocturneColors.TextPrimary)
                if (c.hint != null) {
                    Text(stringResource(c.hint), style = LocalNocturne.current.type.caption, color = NocturneColors.TextPrimary)
                }
            }
        }
    }
}
