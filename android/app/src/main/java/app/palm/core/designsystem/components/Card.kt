package app.palm.core.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneGradientAngles
import app.palm.core.designsystem.NocturneGradients
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.nocturneBrush
import app.palm.core.designsystem.rasterizedBehind

private val CardShape = RoundedCornerShape(NocturneRadius.Lg)

/** Info / line / Home card (UI-SPEC "New"): card gradient, 1 dp foil border, 16 dp padding. No glow, no sheen. */
@Composable
fun NocturneCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val fill = remember { nocturneBrush(NocturneGradients.Card, NocturneGradientAngles.Card) }
    val foil = remember { nocturneBrush(NocturneGradients.FoilBorder, NocturneGradientAngles.FoilBorder) }
    Column(
        modifier
            .clip(CardShape)
            .rasterizedBehind { drawRect(fill) }
            .border(NocturneBorderWidth, foil, CardShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(NocturneSpace.S16),
        verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
        content = content,
    )
}
