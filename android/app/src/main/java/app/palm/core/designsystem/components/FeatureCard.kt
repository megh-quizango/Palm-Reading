package app.palm.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneGradientAngles
import app.palm.core.designsystem.NocturneGradients
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneShadows
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.nocturneBrush
import app.palm.core.designsystem.nocturneGlow
import app.palm.core.designsystem.rasterizedBehind

private val CardShape = RoundedCornerShape(NocturneRadius.Lg)
private val SlotHeight = 132.dp

/** S06 feature tile: optional 132 dp art slot with a badge, then the title row with a decorative arrow disc. */
@Composable
fun FeatureCard(
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    illustration: Painter? = null,
    badge: String? = null,
) {
    val fill = remember { nocturneBrush(NocturneGradients.Card, NocturneGradientAngles.Card) }
    val foil = remember { nocturneBrush(NocturneGradients.FoilBorder, NocturneGradientAngles.FoilBorder) }
    val slotFill = remember { Brush.verticalGradient(listOf(NocturneColors.SurfaceCardHi, NocturneColors.BgRaised)) }
    val discStroke = remember { Brush.linearGradient(listOf(NocturneColors.AccentGold, NocturneColors.BrandLink)) }
    val type = LocalNocturne.current.type
    val soonCd = badge?.let { stringResource(R.string.home_tile_soon_cd, title) }
    Column(
        modifier
            .nocturneGlow(NocturneShadows.GlowCard, CardShape)
            .clip(CardShape)
            .rasterizedBehind {
                drawRect(fill)
                if (illustration != null) {
                    // The opaque art slot covers the card fill, so it is painted over it here, not as a second layer
                    val slot = Size(size.width, SlotHeight.toPx())
                    drawRect(slotFill, size = slot)
                    drawRect(Brush.radialGradient(listOf(NocturneShadows.GlowOrb.color, Color.Transparent), slot.center, slot.width * 0.55f), size = slot)
                }
            }
            .border(NocturneBorderWidth, foil, CardShape)
            .then(if (soonCd != null) Modifier.semantics { contentDescription = soonCd } else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        if (illustration != null) {
            Box(
                Modifier.fillMaxWidth().height(SlotHeight),
                contentAlignment = Alignment.Center,
            ) {
                Image(illustration, null, Modifier.size(112.dp))
                if (badge != null) {
                    NocturneChip(
                        badge,
                        Modifier.align(Alignment.TopStart).padding(NocturneSpace.S8),
                        textColor = NocturneSemantic.MutedForeground,
                    )
                }
            }
        }
        Row(
            Modifier.padding(start = NocturneSpace.S16, top = NocturneSpace.S16, end = NocturneSpace.S8, bottom = NocturneSpace.S16),
            horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
                Text(title, style = type.titleM, color = NocturneSemantic.CardForeground)
                if (body != null) Text(body, style = type.bodyM, color = NocturneSemantic.MutedForeground)
            }
            Box(
                Modifier
                    .size(32.dp)
                    .background(NocturneColors.SurfaceCardHi, CircleShape)
                    .border(NocturneBorderWidth, discStroke, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_arrow), null, Modifier.size(16.dp), tint = NocturneColors.AccentGold)
            }
        }
    }
}
