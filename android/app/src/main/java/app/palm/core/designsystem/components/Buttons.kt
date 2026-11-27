package app.palm.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings
import app.palm.core.designsystem.NocturneGradientAngles
import app.palm.core.designsystem.NocturneGradients
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneShadows
import app.palm.core.designsystem.NocturneSize
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.nocturneBrush
import app.palm.core.designsystem.nocturneGlow

enum class NocturneButtonStyle { Primary, Secondary, Ghost, OnHero }

private const val DISABLED_ALPHA = 0.38f // lower emphasis with the same colours
private const val PRESSED_SCALE = 0.97f

@Composable
fun NocturneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NocturneButtonStyle = NocturneButtonStyle.Primary,
    enabled: Boolean = true,
    adChip: Boolean = false,
) {
    val primaryBrush = remember { nocturneBrush(NocturneGradients.Primary, NocturneGradientAngles.Primary) }
    val look = when (style) {
        NocturneButtonStyle.Primary ->
            Modifier.nocturneGlow(NocturneShadows.GlowPrimary, PillShape).background(primaryBrush, PillShape)
        NocturneButtonStyle.Secondary -> Modifier.background(NocturneSemantic.Muted, PillShape)
        NocturneButtonStyle.Ghost -> Modifier.border(NocturneBorderWidth, NocturneSemantic.Border, PillShape)
        NocturneButtonStyle.OnHero -> Modifier.background(NocturneColors.TextOnPrimary, PillShape)
    }
    val textColor = when (style) {
        NocturneButtonStyle.Primary -> NocturneSemantic.OnPrimary
        NocturneButtonStyle.Secondary -> NocturneSemantic.Foreground
        NocturneButtonStyle.Ghost -> NocturneColors.BrandLink
        NocturneButtonStyle.OnHero -> NocturneColors.BrandPrimaryDeep
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val still = LocalNocturne.current.reduceMotion
    val scale = animateFloatAsState(
        if (pressed && !still) PRESSED_SCALE else 1f,
        tween(NocturneDurations.Press, easing = NocturneEasings.Standard),
        label = "press",
    )
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(look)
            .clip(PillShape)
            .clickable(interaction, LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = NocturneSize.TouchMin, minHeight = NocturneSize.Button)
            .padding(horizontal = NocturneSpace.S24),
        horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = LocalNocturne.current.type.labelL, color = textColor)
        if (adChip) AdChip()
    }
}
