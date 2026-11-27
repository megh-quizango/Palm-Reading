package app.palm.feature.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneSelectField
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** S05: birth date (required) and birth time (optional) through M3 pickers (D-05, D-10). */
@Composable
fun BirthScreen(viewModel: BirthViewModel, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val type = LocalNocturne.current.type
    val locale = LocalConfiguration.current.locales[0]
    var datePickerOpen by rememberSaveable { mutableStateOf(false) }
    OnboardingStep(
        step = 2,
        title = stringResource(R.string.birth_title),
        saveFailed = state.saveFailed,
        ctaEnabled = state.date != null,
        onContinue = onContinue,
        modifier = modifier,
    ) {
        Spacer(Modifier.height(NocturneSpace.S8))
        Text(stringResource(R.string.birth_body), style = type.bodyL, color = NocturneSemantic.MutedForeground)
        Spacer(Modifier.height(NocturneSpace.S24))
        NocturneSelectField(
            label = stringResource(R.string.birth_date_label),
            value = state.date?.let { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(it) },
            placeholder = stringResource(R.string.birth_date_placeholder),
            onClick = { datePickerOpen = true },
        )
    }
    if (datePickerOpen) {
        BirthDatePicker(
            initial = state.date,
            onPick = { viewModel.onDate(it); datePickerOpen = false },
            onDismiss = { datePickerOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePicker(initial: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = remember { LocalDate.now() }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.let(::dateToUtcMillis),
        initialDisplayedMonthMillis = dateToUtcMillis(initial ?: LocalDate.of(2000, 1, 1)),
        yearRange = MIN_BIRTH_DATE.year..today.year,
        selectableDates = remember(today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = isValidBirthDate(utcMillisToDate(utcTimeMillis), today)
                override fun isSelectableYear(year: Int) = year in MIN_BIRTH_DATE.year..today.year
            }
        },
    )
    val colors = birthDatePickerColors()
    val picked = state.selectedDateMillis
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { picked?.let { onPick(utcMillisToDate(it)) } }, enabled = picked != null) {
                Text(stringResource(R.string.birth_date_confirm), color = NocturneColors.BrandLink.copy(alpha = if (picked != null) 1f else 0.38f))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.birth_date_dismiss), color = NocturneColors.BrandLink) }
        },
        colors = colors,
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun birthDatePickerColors(): DatePickerColors = DatePickerDefaults.colors(
    containerColor = NocturneColors.BgRaised,
    titleContentColor = NocturneSemantic.Foreground,
    headlineContentColor = NocturneSemantic.Foreground,
    weekdayContentColor = NocturneSemantic.MutedForeground,
    subheadContentColor = NocturneSemantic.MutedForeground,
    navigationContentColor = NocturneSemantic.Foreground,
    yearContentColor = NocturneSemantic.Foreground,
    currentYearContentColor = NocturneColors.BrandLink,
    selectedYearContentColor = NocturneSemantic.OnPrimary,
    selectedYearContainerColor = NocturneSemantic.Primary,
    dayContentColor = NocturneSemantic.Foreground,
    disabledDayContentColor = NocturneSemantic.Foreground.copy(alpha = 0.38f),
    selectedDayContentColor = NocturneSemantic.OnPrimary,
    selectedDayContainerColor = NocturneSemantic.Primary,
    todayContentColor = NocturneColors.BrandLink,
    todayDateBorderColor = NocturneColors.BrandLink,
)
