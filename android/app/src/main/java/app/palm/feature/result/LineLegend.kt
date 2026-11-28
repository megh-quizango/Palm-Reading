package app.palm.feature.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.palm.contract.PalmLineDto
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneChip

/** One text chip per drawn line, so colour is never the only cue (D-11); nothing when no line is drawn. */
@Composable
fun LineLegend(lines: List<PalmLineDto>, modifier: Modifier = Modifier) {
    val drawn = drawnLines(lines)
    if (drawn.isEmpty()) return
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
        verticalArrangement = Arrangement.spacedBy(NocturneSpace.S4),
    ) {
        drawn.forEach { NocturneChip(stringResource(lineNameRes(it.type)), dotColor = lineColor(it.type)) }
    }
}
