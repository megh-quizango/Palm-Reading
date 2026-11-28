package app.palm.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace

/** The ExitDialog recipe as a shared component: title, body, Primary confirm and optional Ghost dismiss. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NocturneDialog(
    title: String,
    body: String,
    confirm: Pair<String, () -> Unit>,
    dismiss: Pair<String, () -> Unit>? = null,
    onDismissRequest: () -> Unit,
) {
    val nocturne = LocalNocturne.current
    val type = nocturne.type
    val shape = MaterialTheme.shapes.extraLarge
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    val fade = fadeIn(tween(NocturneDurations.Standard))
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        AnimatedVisibility(
            shown,
            enter = if (nocturne.reduceMotion) fade else fade + scaleIn(tween(NocturneDurations.Standard), initialScale = 0.92f),
        ) {
            Column(
                Modifier
                    .widthIn(max = 312.dp)
                    .background(NocturneColors.BgRaised, shape)
                    .border(NocturneBorderWidth, NocturneColors.BorderHairline.copy(alpha = 0.35f), shape)
                    .padding(NocturneSpace.S24),
                verticalArrangement = Arrangement.spacedBy(NocturneSpace.S16),
            ) {
                Text(title, style = type.displayM, color = NocturneSemantic.Foreground, modifier = Modifier.semantics { heading() })
                Text(body, style = type.bodyL, color = NocturneSemantic.MutedForeground)
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
                ) {
                    NocturneButton(confirm.first, onClick = confirm.second)
                    if (dismiss != null) NocturneButton(dismiss.first, onClick = dismiss.second, style = NocturneButtonStyle.Ghost)
                }
            }
        }
    }
}
