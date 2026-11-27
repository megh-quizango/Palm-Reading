package app.palm.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSize
import app.palm.core.designsystem.NocturneSpace

/** 64 dp bar: circular back button + wrapping title (S07, Scan placeholder). */
@Composable
fun NocturneTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp),
        horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(NocturneSize.TouchMin)
                .background(NocturneSemantic.Card, CircleShape)
                .border(NocturneBorderWidth, NocturneColors.BorderHairline.copy(alpha = 0.35f), CircleShape),
        ) {
            Icon(
                painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.common_back),
                tint = NocturneSemantic.Foreground,
                modifier = Modifier.size(NocturneSize.Icon),
            )
        }
        Text(
            title,
            style = LocalNocturne.current.type.displayM,
            color = NocturneSemantic.Foreground,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
    }
}
