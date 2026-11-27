package app.palm.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings
import app.palm.core.designsystem.NocturneGradientAngles
import app.palm.core.designsystem.NocturneGradients
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneShadows
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.devanagariSafe
import app.palm.core.designsystem.nocturneBrush
import app.palm.core.designsystem.nocturneGlow

private val TileShape = RoundedCornerShape(NocturneRadius.Md)

/** Single-select tile (S04 gender); put several inside a selectableGroup(). */
@Composable
fun NocturneChoiceTile(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val nocturne = LocalNocturne.current
    val primaryBrush = remember { nocturneBrush(NocturneGradients.Primary, NocturneGradientAngles.Primary) }
    val fill by animateFloatAsState(
        if (selected) 1f else 0f,
        if (nocturne.reduceMotion) snap() else tween(NocturneDurations.Quick, easing = NocturneEasings.Standard),
        label = "tileFill",
    )
    val border by animateColorAsState(
        if (selected) NocturneColors.BrandLink else NocturneColors.BorderHairline.copy(alpha = 0.4f),
        if (nocturne.reduceMotion) snap() else tween(NocturneDurations.Quick, easing = NocturneEasings.Standard),
        label = "tileBorder",
    )
    val stateText = stringResource(if (selected) R.string.common_selected else R.string.common_not_selected)
    Box(
        modifier
            .then(if (selected) Modifier.nocturneGlow(NocturneShadows.GlowPrimary, TileShape) else Modifier)
            .background(NocturneSemantic.Card, TileShape)
            .background(primaryBrush, TileShape, alpha = fill)
            .border(NocturneBorderWidth, border, TileShape)
            .clip(TileShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = stateText }
            .heightIn(min = 56.dp)
            .padding(NocturneSpace.S8),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = nocturne.type.titleM.devanagariSafe(),
            // Foreground and OnPrimary are both near white; no animation needed
            color = if (selected) NocturneSemantic.OnPrimary else NocturneSemantic.Foreground,
            textAlign = TextAlign.Center,
        )
    }
}
