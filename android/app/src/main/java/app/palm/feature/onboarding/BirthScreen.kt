package app.palm.feature.onboarding

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneSelectField
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** S05: birth date (required) and birth time (optional) through M3 pickers (D-05, D-10). */
@Composable
fun BirthScreen(viewModel: BirthViewModel, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val type = LocalNocturne.current.type
    val locale = LocalConfiguration.current.locales[0]
    var datePickerOpen by rememberSaveable { mutableStateOf(false) }
    var timePickerOpen by rememberSaveable { mutableStateOf(false) }
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
        Spacer(Modifier.height(NocturneSpace.S16))
        NocturneSelectField(
            label = stringResource(R.string.birth_time_label),
            value = state.time?.let { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(it) },
            placeholder = stringResource(R.string.birth_time_placeholder),
            onClick = { timePickerOpen = true },
            enabled = !state.timeUnknown,
        )
        Spacer(Modifier.height(NocturneSpace.S8))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = state.timeUnknown, role = Role.Checkbox, onValueChange = viewModel::onTimeUnknown),
            horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.timeUnknown,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = NocturneSemantic.Primary,
                    checkmarkColor = NocturneSemantic.OnPrimary,
                    uncheckedColor = NocturneSemantic.MutedForeground,
                ),
            )
            Text(stringResource(R.string.birth_time_unknown), style = type.bodyL, color = NocturneSemantic.Foreground)
        }
        if (state.timeUnknown) {
            Text(stringResource(R.string.birth_time_unknown_note), style = type.caption, color = NocturneSemantic.MutedForeground)
        }
    }
    if (timePickerOpen) {
        BirthTimePicker(
            initial = state.time,
            onPick = { viewModel.onTime(it); timePickerOpen = false },
            onDismiss = { timePickerOpen = false },
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

/** M3 TimePicker in a BasicAlertDialog: no dependency on the newer TimePickerDialog wrapper (UI-SPEC Unresolved row). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthTimePicker(initial: LocalTime?, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: 12,
        initialMinute = initial?.minute ?: 0,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = NocturneColors.BgRaised) {
            Column(Modifier.padding(NocturneSpace.S24), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.birth_time_dialog_title),
                    style = LocalNocturne.current.type.titleM,
                    color = NocturneSemantic.Foreground,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(NocturneSpace.S16))
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = NocturneSemantic.Card,
                        selectorColor = NocturneSemantic.Primary,
                        timeSelectorSelectedContainerColor = NocturneSemantic.Primary,
                        timeSelectorSelectedContentColor = NocturneSemantic.OnPrimary,
                        timeSelectorUnselectedContainerColor = NocturneColors.SurfaceCardHi,
                        timeSelectorUnselectedContentColor = NocturneSemantic.Foreground,
                        clockDialSelectedContentColor = NocturneSemantic.OnPrimary,
                        clockDialUnselectedContentColor = NocturneSemantic.Foreground,
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.birth_time_dismiss), color = NocturneColors.BrandLink) }
                    TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                        Text(stringResource(R.string.birth_time_confirm), color = NocturneColors.BrandLink)
                    }
                }
            }
        }
    }
}
