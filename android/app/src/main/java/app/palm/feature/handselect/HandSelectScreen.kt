package app.palm.feature.handselect

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.contract.HandSide
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings
import app.palm.core.designsystem.NocturneGradientAngles
import app.palm.core.designsystem.NocturneGradients
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSelectedBorderWidth
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneShadows
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.components.NocturneTopBar
import app.palm.core.designsystem.nocturneBrush
import app.palm.core.designsystem.nocturneGlow

private val CardShape = RoundedCornerShape(NocturneRadius.Lg)

@StringRes
internal fun handLabel(hand: HandSide): Int = if (hand == HandSide.LEFT) R.string.hand_left else R.string.hand_right

/** S07: Left / Right (Right each time the screen opens, never persisted) and photo tips (D-15). */
@Composable
fun HandSelectScreen(onBack: () -> Unit, onContinue: (HandSide) -> Unit, modifier: Modifier = Modifier) {
    var hand by rememberSaveable { mutableStateOf(HandSide.RIGHT) }
    val type = LocalNocturne.current.type
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, bottom = NocturneSpace.S16),
    ) {
        NocturneTopBar(stringResource(R.string.hand_title), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = NocturneSpace.S16),
            verticalArrangement = Arrangement.spacedBy(NocturneSpace.S16),
        ) {
            Text(stringResource(R.string.hand_question), style = type.titleM, color = NocturneSemantic.Foreground)
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S16)) {
                for (side in listOf(HandSide.LEFT, HandSide.RIGHT)) {
                    HandOptionCard(side, selected = hand == side, onClick = { hand = side }, modifier = Modifier.weight(1f))
                }
            }
            Text(stringResource(R.string.hand_hint), style = type.caption, color = NocturneSemantic.MutedForeground)
            PhotoTips()
            Text(stringResource(R.string.hand_requirement), style = type.caption, color = NocturneSemantic.MutedForeground)
            Text(stringResource(R.string.hand_privacy), style = type.caption, color = NocturneSemantic.MutedForeground)
        }
        NocturneButton(stringResource(R.string.common_continue), onClick = { onContinue(hand) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun HandOptionCard(hand: HandSide, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val nocturne = LocalNocturne.current
    val hiFill = remember { Brush.verticalGradient(listOf(NocturneColors.SurfaceCardHi, NocturneSemantic.Card)) }
    val check = remember { nocturneBrush(NocturneGradients.Primary, NocturneGradientAngles.Primary) }
    val spec = if (nocturne.reduceMotion) snap<Float>() else tween(NocturneDurations.Quick, easing = NocturneEasings.Standard)
    val fill by animateFloatAsState(if (selected) 1f else 0f, spec, label = "handFill")
    val border by animateColorAsState(
        if (selected) NocturneColors.BrandLink else NocturneColors.BorderHairline.copy(alpha = 0.35f),
        if (nocturne.reduceMotion) snap() else tween(NocturneDurations.Quick, easing = NocturneEasings.Standard),
        label = "handBorder",
    )
    val stateText = stringResource(if (selected) R.string.common_selected else R.string.common_not_selected)
    Column(
        modifier
            .then(if (selected) Modifier.nocturneGlow(NocturneShadows.GlowCard, CardShape) else Modifier)
            .background(NocturneSemantic.Card, CardShape)
            .background(hiFill, CardShape, alpha = fill)
            .border(if (selected) NocturneSelectedBorderWidth else NocturneBorderWidth, border, CardShape)
            .clip(CardShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = stateText }
            .heightIn(min = 48.dp)
            .padding(NocturneSpace.S16),
        verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painterResource(R.drawable.illus_constellation_hand), null,
            Modifier.size(92.dp, 120.dp).graphicsLayer { if (hand == HandSide.LEFT) scaleX = -1f },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(handLabel(hand)),
                style = nocturne.type.labelL,
                color = NocturneSemantic.Foreground,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (selected) {
                Box(Modifier.size(24.dp).background(check, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp), tint = NocturneSemantic.OnPrimary)
                }
            }
        }
    }
}

private class BadTip(@DrawableRes val art: Int, @StringRes val label: Int)

private val BadTips = listOf(
    BadTip(R.drawable.tip_too_far, R.string.tips_bad_far),
    BadTip(R.drawable.tip_blurry_dark, R.string.tips_bad_blurry),
    BadTip(R.drawable.tip_fingers_closed, R.string.tips_bad_fingers),
    BadTip(R.drawable.tip_jewellery_bent, R.string.tips_bad_jewellery),
)

@Composable
private fun PhotoTips() {
    val type = LocalNocturne.current.type
    Column(
        Modifier
            .fillMaxWidth()
            .background(NocturneSemantic.Card, CardShape)
            .border(NocturneBorderWidth, NocturneColors.BorderHairline.copy(alpha = 0.3f), CardShape)
            .padding(NocturneSpace.S16),
        verticalArrangement = Arrangement.spacedBy(NocturneSpace.S16),
    ) {
        Row(
            Modifier.semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(56.dp)) {
                Image(
                    painterResource(R.drawable.tip_good), null,
                    Modifier.size(56.dp).clip(CircleShape).border(NocturneSelectedBorderWidth, NocturneColors.StatusSuccess, CircleShape),
                )
                StatusBadge(NocturneColors.StatusSuccess, R.drawable.ic_check, NocturneSemantic.OnAccent)
            }
            Column(verticalArrangement = Arrangement.spacedBy(NocturneSpace.S4)) {
                Text(stringResource(R.string.tips_good_title), style = type.titleM, color = NocturneSemantic.Foreground)
                Text(stringResource(R.string.tips_good_body), style = type.caption, color = NocturneSemantic.MutedForeground)
            }
        }
        Text(stringResource(R.string.tips_bad_title), style = type.labelM, color = NocturneSemantic.MutedForeground)
        Row(horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8), verticalAlignment = Alignment.Top) {
            for (tip in BadTips) {
                val label = stringResource(tip.label)
                val cd = stringResource(R.string.tips_bad_cd, label)
                Column(
                    Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = cd },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(56.dp)) {
                        Image(
                            painterResource(tip.art), null,
                            Modifier.size(56.dp).background(NocturneColors.SurfaceCardHi, CircleShape).clip(CircleShape),
                        )
                        StatusBadge(NocturneColors.StatusError, R.drawable.ic_close, NocturneSemantic.OnAccent)
                    }
                    Spacer(Modifier.height(NocturneSpace.S8))
                    Text(label, style = type.caption, color = NocturneSemantic.Foreground, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.StatusBadge(color: Color, @DrawableRes icon: Int, tint: Color) {
    Box(Modifier.align(Alignment.TopEnd).size(20.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, Modifier.size(12.dp), tint = tint)
    }
}
