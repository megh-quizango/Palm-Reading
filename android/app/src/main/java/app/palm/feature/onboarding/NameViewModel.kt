package app.palm.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.core.data.Gender
import app.palm.core.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NameUiState(
    val name: String = "",
    val gender: Gender? = null,
    val check: NameCheck = NameCheck.EMPTY,
    val showError: Boolean = false,
    val saveFailed: Boolean = false,
)

@HiltViewModel
class NameViewModel @Inject constructor(private val repo: ProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(NameUiState())
    val state: StateFlow<NameUiState> = _state.asStateFlow()

    fun onNameChange(text: String) {
        if (text.length > NAME_MAX) return
        _state.update { it.copy(name = text, check = checkName(text)) }
    }

    /** Tapping the selected tile clears the choice (gender is optional). */
    fun onGenderTap(gender: Gender) {
        _state.update { it.copy(gender = if (it.gender == gender) null else gender) }
    }

    fun revealError() {} // RED stub, implemented in the GREEN step

    fun save(next: () -> Unit) {
        val s = _state.value
        if (s.check != NameCheck.OK) return
        viewModelScope.launch {
            repo.saveName(s.name.trim(), s.gender)
            next()
        }
    }
}
