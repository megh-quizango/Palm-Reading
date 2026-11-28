package app.palm.feature.scan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings
import app.palm.core.designsystem.NocturneGradientAngles
import app.palm.core.designsystem.NocturneGradients
import app.palm.core.designsystem.NocturneShadows
import app.palm.core.designsystem.nocturneBrush
import app.palm.core.designsystem.nocturneGlow

internal val ControlSize = 52.dp
private val ShutterSize = 76.dp
private val ShutterDisc = 60.dp
private const val DISABLED_ALPHA = 0.38f

@Composable
private fun ControlDisc(fill: Color, enabled: Boolean, modifier: Modifier, content: @Composable () -> Unit) = Box(
    modifier
        .size(ControlSize)
        .alpha(if (enabled) 1f else DISABLED_ALPHA)
        .clip(CircleShape)
        .background(fill, CircleShape)
        .border(NocturneBorderWidth, NocturneColors.BorderHairline.copy(alpha = 0.35f), CircleShape),
    contentAlignment = Alignment.Center,
) { content() }

/** 52 dp camera button: scrim fill when [floating] (Back), else the bar fill that brightens while pressed. */
@Composable
internal fun CameraIconButton(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    floating: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill = when {
        floating -> NocturneColors.SurfaceScrim.copy(alpha = 0.72f)
        pressed -> NocturneColors.TextPrimary.copy(alpha = 0.2f)
        else -> NocturneColors.TextPrimary.copy(alpha = 0.12f)
    }
    ControlDisc(
        fill,
        enabled,
        modifier
            .clickable(interaction, LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = NocturneColors.TextPrimary, modifier = Modifier.size(24.dp))
    }
}

/** Torch switch (D-05): gold disc when on. Shown only when the camera has a flash unit. */
@Composable
internal fun TorchButton(on: Boolean, enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val still = LocalNocturne.current.reduceMotion
    val disc by animateColorAsState(
        if (on) NocturneColors.AccentGold else NocturneColors.TextPrimary.copy(alpha = 0.12f),
        if (still) snap() else tween(NocturneDurations.Instant, easing = NocturneEasings.Standard),
        label = "torch",
    )
    val description = stringResource(R.string.scan_torch_cd)
    val state = stringResource(if (on) R.string.common_on else R.string.common_off)
    ControlDisc(
        disc,
        enabled,
        modifier
            .toggleable(value = on, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .semantics {
                contentDescription = description
                stateDescription = state
            },
    ) {
        Icon(
            painterResource(if (on) R.drawable.ic_flash else R.drawable.ic_flash_off),
            contentDescription = null,
            tint = if (on) NocturneColors.TextOnAccent else NocturneColors.TextPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** S08 shutter (SCAN-04): disabled until a palm is held 500 ms, busy from the tap until the photo is taken. */
@Composable
internal fun ShutterButton(enabled: Boolean, busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val still = LocalNocturne.current.reduceMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val live = enabled || busy // busy keeps the enabled look
    val quick = if (still) snap<Float>() else tween(NocturneDurations.Quick, easing = NocturneEasings.Standard)
    val mix = animateFloatAsState(if (live) 1f else 0f, quick, label = "shutterMix")
    val scale = animateFloatAsState(
        if (pressed && !still) 0.97f else 1f,
        tween(NocturneDurations.Press, easing = NocturneEasings.Standard),
        label = "shutterPress",
    )
    val brush = remember { nocturneBrush(NocturneGradients.Primary, NocturneGradientAngles.Primary) }
    val ringOff = NocturneColors.TextPrimary.copy(alpha = 0.4f)
    val discOff = NocturneColors.TextPrimary.copy(alpha = 0.18f)
    val description = stringResource(R.string.scan_capture_cd)
    val state = when {
        busy -> stringResource(R.string.scan_capture_busy)
        !enabled -> stringResource(R.string.scan_capture_waiting)
        else -> null
    }
    Box(
        modifier
            .size(ShutterSize)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = 0.6f + 0.4f * mix.value
            }
            .drawBehind {
                val stroke = 4.dp.toPx()
                drawCircle(lerp(ringOff, NocturneColors.StatusSuccess, mix.value), (size.minDimension - stroke) / 2f, style = Stroke(stroke))
            }
            // unbounded round ripple instead of a clip, so the disc glow is not cut off
            .clickable(interaction, ripple(bounded = false, radius = ShutterSize / 2), enabled = enabled && !busy, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                if (state != null) stateDescription = state
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(ShutterDisc)
                .graphicsLayer { alpha = mix.value }
                .nocturneGlow(NocturneShadows.GlowPrimary, CircleShape),
        )
        Box(
            Modifier
                .size(ShutterDisc)
                .drawBehind {
                    val m = mix.value
                    drawCircle(discOff, alpha = 1f - m)
                    drawCircle(brush, alpha = m)
                },
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(24.dp), color = NocturneColors.TextOnPrimary, strokeWidth = 2.dp)
            } else {
                Icon(painterResource(R.drawable.ic_camera), contentDescription = null, tint = NocturneColors.TextOnPrimary, modifier = Modifier.size(24.dp))
            }
        }
    }
}
