package app.palm.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.core.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BirthUiState(
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val timeUnknown: Boolean = false,
    val saveFailed: Boolean = false,
)

@HiltViewModel
class BirthViewModel @Inject constructor(private val repo: ProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(BirthUiState())
    val state: StateFlow<BirthUiState> = _state.asStateFlow()

    private var touched = false

    init {
        // Resume an interrupted onboarding with the saved values (D-04)
        viewModelScope.launch {
            val saved = try { repo.profile.first() } catch (_: IOException) { null } ?: return@launch
            if (!touched) {
                _state.update { it.copy(date = saved.birthDate, time = saved.birthTime, timeUnknown = saved.birthTimeUnknown) }
            }
        }
    }

    fun onDate(date: LocalDate) {
        if (!isValidBirthDate(date, LocalDate.now())) return
        touched = true
        _state.update { it.copy(date = date) }
    }

    fun onTime(time: LocalTime) {
        touched = true
        _state.update { it.copy(time = time) }
    }

    /** "I don't know my birth time": clears the time; unchecking leaves the time empty (no default, D-10). */
    fun onTimeUnknown(checked: Boolean) {
        touched = true
        _state.update { it.copy(timeUnknown = checked, time = if (checked) null else it.time) }
    }

    /** The last onboarding step: saves the birth data, then marks onboarding complete. */
    fun finish(next: () -> Unit) {
        val s = _state.value
        val date = s.date ?: return
        viewModelScope.launch {
            try {
                repo.saveBirth(date, s.time, s.timeUnknown)
                repo.completeOnboarding()
                _state.update { it.copy(saveFailed = false) }
                next()
            } catch (_: IOException) {
                _state.update { it.copy(saveFailed = true) }
            }
        }
    }
}
