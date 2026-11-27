package app.palm.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.components.NocturneButtonStyle
import app.palm.core.designsystem.devanagariSafe
import app.palm.core.designsystem.nocturneBrush
import app.palm.core.designsystem.nocturneGlow
import app.palm.core.designsystem.rasterizedBehind

private val HeroShape = RoundedCornerShape(NocturneRadius.Lg)

/** S06 hero: text on the left ~60% (white on the right end is 4.1:1), orb on the right; the CTA is the only target. */
@Composable
fun HeroCard(onScan: () -> Unit, modifier: Modifier = Modifier) {
    val nocturne = LocalNocturne.current
    val type = nocturne.type
    val aurora = remember { nocturneBrush(NocturneGradients.HeroAurora, NocturneGradientAngles.HeroAurora) }
    val edge = remember {
        nocturneBrush(arrayOf(0f to Color.White.copy(alpha = 0.55f), 1f to Color.White.copy(alpha = 0.05f)), 135f)
    }
    val showOrb = LocalDensity.current.fontScale < 1.5f
    Row(
        modifier
            .fillMaxWidth()
            .nocturneGlow(NocturneShadows.GlowPrimary, HeroShape)
            .clip(HeroShape)
            .rasterizedBehind {
                val r = size.maxDimension * 0.7f
                drawRect(aurora)
                drawRect(Brush.radialGradient(listOf(NocturneColors.PalmHeart.copy(alpha = 0.45f), Color.Transparent), Offset.Zero, r))
                drawRect(
                    Brush.radialGradient(
                        listOf(NocturneColors.PalmHead.copy(alpha = 0.25f), Color.Transparent), Offset(size.width, size.height), r,
                    ),
                )
            }
            .border(NocturneBorderWidth, edge, HeroShape)
            .padding(NocturneSpace.S24),
        horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
            Row(horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S4), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_sparkle), null, Modifier.size(16.dp), tint = NocturneColors.AccentGold)
                Text(stringResource(R.string.home_hero_overline), style = type.overline.devanagariSafe(), color = NocturneColors.AccentGold)
            }
            Text(stringResource(R.string.home_hero_title), style = type.displayM, color = NocturneSemantic.OnPrimary)
            Text(stringResource(R.string.home_hero_body), style = type.bodyL, color = NocturneSemantic.OnPrimary)
            NocturneButton(
                text = stringResource(R.string.home_hero_cta),
                onClick = onScan,
                style = NocturneButtonStyle.OnHero,
                modifier = Modifier.padding(top = NocturneSpace.S8),
            )
        }
        if (showOrb) HeroOrb(still = nocturne.reduceMotion)
    }
}

@Composable
private fun HeroOrb(still: Boolean) {
    var breathe: State<Float>? = null
    var spin: State<Float>? = null
    if (!still) {
        val loop = rememberInfiniteTransition(label = "hero")
        breathe = loop.animateFloat(
            1f, 1.04f,
            infiniteRepeatable(tween(NocturneDurations.Ambient, easing = NocturneEasings.GentleSine), RepeatMode.Reverse),
            label = "breathe",
        )
        spin = loop.animateFloat(0f, 360f, infiniteRepeatable(tween(NocturneDurations.AmbientSlow, easing = LinearEasing)), label = "spin")
    }
    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        // Values are read in the graphicsLayer lambdas (draw phase), so the loop never recomposes
        Image(
            painterResource(R.drawable.hero_orbit), null,
            Modifier.requiredSize(160.dp, 104.dp).graphicsLayer { rotationZ = spin?.value ?: 0f },
        )
        Image(
            painterResource(R.drawable.illus_guide_orb), null,
            Modifier.size(104.dp).graphicsLayer {
                val s = breathe?.value ?: 1f
                scaleX = s
                scaleY = s
            },
        )
    }
}
