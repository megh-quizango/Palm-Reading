package app.palm.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.R
import app.palm.core.data.AppLanguage
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSelectedBorderWidth
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.devanagariSafe

private val RowShape = RoundedCornerShape(NocturneRadius.Md)

/** S02: pick English or Hindi; the UI switches at once, Continue moves on (D-06). */
@Composable
fun LanguageScreen(viewModel: LanguageViewModel, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()
    val type = LocalNocturne.current.type
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, top = NocturneSpace.S24, bottom = NocturneSpace.S16),
    ) {
        // Only the content scrolls at large font scale; the CTA stays above the navigation inset
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.language_title),
                style = type.displayL,
                color = NocturneSemantic.Foreground,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(NocturneSpace.S8))
            Text(stringResource(R.string.language_subtitle), style = type.bodyL, color = NocturneSemantic.MutedForeground)
            Spacer(Modifier.height(NocturneSpace.S24))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
                LanguageRow(R.string.language_name_en, selected == AppLanguage.EN) { viewModel.select(AppLanguage.EN) }
                LanguageRow(R.string.language_name_hi, selected == AppLanguage.HI) { viewModel.select(AppLanguage.HI) }
            }
            Spacer(Modifier.height(NocturneSpace.S24))
        }
        if (saveFailed) {
            Text(
                stringResource(R.string.common_save_failed),
                style = type.caption,
                color = NocturneColors.StatusError,
                modifier = Modifier.padding(bottom = NocturneSpace.S8),
            )
        }
        NocturneButton(
            text = stringResource(R.string.language_cta),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LanguageRow(@StringRes label: Int, selected: Boolean, onSelect: () -> Unit) {
    val look = if (selected) {
        Modifier
            .background(NocturneSemantic.Card, RowShape)
            .border(NocturneSelectedBorderWidth, NocturneColors.BrandLink, RowShape)
    } else {
        Modifier
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(look)
            .clip(RowShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = NocturneSpace.S16, vertical = NocturneSpace.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(label),
            style = LocalNocturne.current.type.bodyL.devanagariSafe(),
            color = NocturneSemantic.Foreground,
            modifier = Modifier.weight(1f),
        )
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = NocturneColors.BrandLink,
                unselectedColor = NocturneSemantic.MutedForeground,
            ),
        )
    }
}
