package app.palm.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneGradientAngles
import app.palm.core.designsystem.NocturneGradients
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneShadows
import app.palm.core.designsystem.NocturneSize
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.nocturneBrush
import app.palm.core.designsystem.nocturneGlow

enum class NocturneButtonStyle { Primary, Secondary, Ghost }

private const val DISABLED_ALPHA = 0.38f // lower emphasis with the same colours

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
    }
    val textColor = when (style) {
        NocturneButtonStyle.Primary -> NocturneSemantic.OnPrimary
        NocturneButtonStyle.Secondary -> NocturneSemantic.Foreground
        NocturneButtonStyle.Ghost -> NocturneColors.BrandLink
    }
    Row(
        modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(look)
            .clip(PillShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = NocturneSize.TouchMin, minHeight = NocturneSize.Button)
            .padding(horizontal = NocturneSpace.S24),
        horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = LocalNocturne.current.type.labelL, color = textColor)
        if (adChip) AdChip()
    }
}
