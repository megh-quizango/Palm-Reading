package app.palm.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.R
import app.palm.core.data.Gender
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.components.NocturneChoiceTile
import app.palm.core.designsystem.components.NocturneTextField
import app.palm.core.designsystem.devanagariSafe

/** S04: first name and optional gender (D-09). */
@Composable
fun NameScreen(viewModel: NameViewModel, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val type = LocalNocturne.current.type
    // safeDrawing includes the IME inset, so the CTA rides above the keyboard (adjustResize)
    OnboardingStep(
        step = 1,
        title = stringResource(R.string.name_title),
        saveFailed = state.saveFailed,
        ctaEnabled = state.check == NameCheck.OK,
        onContinue = onContinue,
        modifier = modifier,
    ) {
        Spacer(Modifier.height(NocturneSpace.S24))
        NocturneTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = stringResource(R.string.name_label),
            placeholder = stringResource(R.string.name_placeholder),
            helper = stringResource(R.string.name_helper),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            textStyle = type.bodyL.devanagariSafe(),
        )
        Spacer(Modifier.height(NocturneSpace.S24))
        Text(stringResource(R.string.gender_title), style = type.labelM, color = NocturneSemantic.MutedForeground)
        Spacer(Modifier.height(NocturneSpace.S8))
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
            for ((g, label) in listOf(Gender.MALE to R.string.gender_male, Gender.FEMALE to R.string.gender_female, Gender.OTHER to R.string.gender_other)) {
                NocturneChoiceTile(stringResource(label), state.gender == g, { viewModel.onGenderTap(g) }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(NocturneSpace.S8))
        Text(stringResource(R.string.gender_hint), style = type.caption, color = NocturneSemantic.MutedForeground)
    }
}

/** Shared S04/S05 chrome: step overline, title, scrolling content, save error and the bottom CTA. */
@Composable
internal fun OnboardingStep(
    step: Int,
    title: String,
    saveFailed: Boolean,
    ctaEnabled: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val type = LocalNocturne.current.type
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, top = NocturneSpace.S24, bottom = NocturneSpace.S16),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.onboarding_step, step, 2), style = type.overline, color = NocturneSemantic.Accent)
            Spacer(Modifier.height(NocturneSpace.S8))
            Text(title, style = type.displayL, color = NocturneSemantic.Foreground, modifier = Modifier.semantics { heading() })
            content()
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
            text = stringResource(R.string.common_continue),
            onClick = onContinue,
            enabled = ctaEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
