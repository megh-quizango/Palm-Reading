package app.palm.feature.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.palm.contract.PalmLineDto
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneCard
import app.palm.core.designsystem.components.NocturneChip
import app.palm.core.designsystem.devanagariSafe

/** A Result section card (D-08): gold overline, optional body, optional chip row. Reading text renders as received (D-23). */
@Composable
fun InfoCard(overline: String, body: String?, chips: List<String> = emptyList(), modifier: Modifier = Modifier) {
    val type = LocalNocturne.current.type
    NocturneCard(modifier.fillMaxWidth()) {
        Text(overline, style = type.overline.devanagariSafe(), color = NocturneColors.AccentGold)
        if (!body.isNullOrBlank()) Text(body, style = type.bodyL, color = NocturneColors.TextPrimary)
        if (chips.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8), verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
                chips.forEach { NocturneChip(it, textColor = NocturneColors.TextMuted) }
            }
        }
    }
}

/** One card per line (RES-03): colour dot, title, exactly one badge, interpretation; one TalkBack node. */
@Composable
fun LineCard(line: PalmLineDto, modifier: Modifier = Modifier) {
    val type = LocalNocturne.current.type
    val badge = line.badge()
    NocturneCard(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
            verticalArrangement = Arrangement.spacedBy(NocturneSpace.S4),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
                // Keeps its colour when not drawn: it names the line, the badge says it is not drawn
                Box(Modifier.size(12.dp).background(lineColor(line.type), CircleShape))
                Text(
                    line.label.ifBlank { stringResource(lineFullNameRes(line.type)) },
                    style = type.titleM,
                    color = NocturneColors.TextPrimary,
                )
            }
            NocturneChip(
                stringResource(badgeRes(badge)),
                textColor = if (badge == Badge.APPROXIMATE) NocturneColors.BrandLink else NocturneColors.TextMuted,
            )
        }
        if (line.interpretation.isNotBlank()) Text(line.interpretation, style = type.bodyL, color = NocturneColors.TextMuted)
    }
}
