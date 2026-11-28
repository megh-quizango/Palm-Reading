package app.palm.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSpace

/** Icon disc, title, body and one or two full-width actions (permission, fallback and "gone" states). */
@Composable
fun NocturneStateBlock(
    icon: Painter,
    tone: Color,
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>? = null,
    modifier: Modifier = Modifier,
) {
    val type = LocalNocturne.current.type
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(96.dp)
                .background(NocturneColors.SurfaceCard, CircleShape)
                .border(NocturneBorderWidth, tone.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(NocturneSpace.S24))
        Text(
            title,
            style = type.displayM,
            color = NocturneColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(NocturneSpace.S8))
        Text(body, style = type.bodyL, color = NocturneColors.TextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(NocturneSpace.S24))
        NocturneButton(primary.first, onClick = primary.second, modifier = Modifier.fillMaxWidth())
        if (secondary != null) {
            Spacer(Modifier.height(NocturneSpace.S8))
            NocturneButton(
                secondary.first,
                onClick = secondary.second,
                modifier = Modifier.fillMaxWidth(),
                style = NocturneButtonStyle.Ghost,
            )
        }
    }
}
