package app.palm.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.core.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun onDate(date: LocalDate) {
        if (isValidBirthDate(date, LocalDate.now())) _state.update { it.copy(date = date) }
    }

    fun onTime(time: LocalTime) {
        _state.update { it.copy(time = time) }
    }

    /** The last onboarding step: saves the birth data, then marks onboarding complete. */
    fun finish(next: () -> Unit) {
        val s = _state.value
        val date = s.date ?: return
        viewModelScope.launch {
            repo.saveBirth(date, s.time, s.timeUnknown)
            repo.completeOnboarding()
            next()
        }
    }
}
