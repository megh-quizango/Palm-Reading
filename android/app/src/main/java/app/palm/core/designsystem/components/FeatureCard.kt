package app.palm.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneGradientAngles
import app.palm.core.designsystem.NocturneGradients
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneShadows
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.nocturneBrush
import app.palm.core.designsystem.nocturneGlow

private val CardShape = RoundedCornerShape(NocturneRadius.Lg)

@Composable
fun FeatureCard(title: String, body: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val fill = remember { nocturneBrush(NocturneGradients.Card, NocturneGradientAngles.Card) }
    val foil = remember { nocturneBrush(NocturneGradients.FoilBorder, NocturneGradientAngles.FoilBorder) }
    val type = LocalNocturne.current.type
    Column(
        modifier
            .nocturneGlow(NocturneShadows.GlowCard, CardShape)
            .background(fill, CardShape)
            .border(NocturneBorderWidth, foil, CardShape)
            .clip(CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(NocturneSpace.S20),
        verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
    ) {
        Text(title, style = type.titleM, color = NocturneSemantic.CardForeground)
        Text(body, style = type.bodyM, color = NocturneSemantic.MutedForeground)
    }
}
