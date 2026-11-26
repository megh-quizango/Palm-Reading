package app.palm.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.NocturneSize
import app.palm.core.designsystem.components.FeatureCard
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.components.NocturneButtonStyle
import app.palm.core.designsystem.components.NocturneChip
import app.palm.core.designsystem.devanagariSafe

private const val PALM = "palm."

// D-06/D-07 proof: conjuncts and matras must shape and stay unclipped in every style
private const val HINDI_SAMPLE = "हृदय रेखा · ज्ञान · क्षत्रिय"

@Composable
fun ShowcaseScreen(statusViewModel: BackendStatusViewModel) {
    val s16 = NocturneSpace.S16
    val padding = WindowInsets.safeDrawing.add(WindowInsets(s16, s16, s16, s16)).asPaddingValues()
    val (palm, roles) = NocturneColors.ByPath.entries.partition { it.key.startsWith(PALM) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(NocturneSpace.S24)) {
            item { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall) }
            item { BackendStatusRow(statusViewModel) }
            item { Section("Components") { Components() } }
            item { Section("Colours") { SwatchGrid(roles.map { it.key to it.value }) } }
            item { Section("Palm lines") { SwatchGrid(palm.map { it.key to it.value }) } }
            item { Section("Type") { TypeSamples() } }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NocturneSpace.S12)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchGrid(swatches: List<Pair<String, Color>>) {
    FlowRow(
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
        verticalArrangement = Arrangement.spacedBy(NocturneSpace.S12),
    ) {
        swatches.forEach { (path, color) ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NocturneSpace.S4)) {
                Box(
                    Modifier.fillMaxWidth().height(NocturneSize.TouchMin)
                        .background(color, MaterialTheme.shapes.small)
                        .border(Dp.Hairline, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
                )
                Text(path, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Components() {
    Column(verticalArrangement = Arrangement.spacedBy(NocturneSpace.S16)) {
        NocturneButton("Primary", onClick = {}, Modifier.fillMaxWidth())
        NocturneButton("Secondary", onClick = {}, Modifier.fillMaxWidth(), style = NocturneButtonStyle.Secondary)
        NocturneButton("Ghost", onClick = {}, Modifier.fillMaxWidth(), style = NocturneButtonStyle.Ghost)
        NocturneButton("Scan again", onClick = {}, Modifier.fillMaxWidth(), adChip = true)
        NocturneChip("Chip")
        FeatureCard("Feature card", "Gradient fill, foil border and the card glow, all from tokens.", Modifier.fillMaxWidth())
    }
}

@Composable
private fun TypeSamples() {
    Column(verticalArrangement = Arrangement.spacedBy(NocturneSpace.S16)) {
        LocalNocturne.current.type.byName.forEach { (name, style) ->
            Column {
                Text(name, style = style)
                Text(HINDI_SAMPLE, style = style.devanagariSafe())
            }
        }
    }
}
