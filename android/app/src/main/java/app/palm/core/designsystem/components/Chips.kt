package app.palm.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace

internal val PillShape = RoundedCornerShape(50) // radius.pill (999 px) on any height

@Composable
fun NocturneChip(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    textColor: Color = NocturneSemantic.Foreground,
) {
    Row(
        modifier.background(NocturneSemantic.Muted, PillShape)
            .padding(horizontal = NocturneSpace.S8, vertical = NocturneSpace.S4),
        horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) Box(Modifier.size(NocturneSpace.S8).background(dotColor, CircleShape))
        Text(text, style = LocalNocturne.current.type.labelM, color = textColor)
    }
}

/** The "AD" marker on actions that show an ad first (ADS-03, e.g. "Scan again"). */
@Composable
fun AdChip(modifier: Modifier = Modifier) {
    Text(
        "AD",
        modifier.border(NocturneBorderWidth, NocturneColors.AdOutline, PillShape)
            .padding(horizontal = NocturneSpace.S8, vertical = NocturneSpace.S2),
        style = LocalNocturne.current.type.overline,
        color = NocturneColors.AdOutline,
    )
}
