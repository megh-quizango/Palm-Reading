package app.palm.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.core.data.Gender
import app.palm.core.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NameUiState(
    val name: String = "",
    val gender: Gender? = null,
    val check: NameCheck = NameCheck.EMPTY,
    val showError: Boolean = false,
    val saveFailed: Boolean = false,
)

private fun NameCheck.isError() = this != NameCheck.OK && this != NameCheck.EMPTY

@HiltViewModel
class NameViewModel @Inject constructor(private val repo: ProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(NameUiState())
    val state: StateFlow<NameUiState> = _state.asStateFlow()

    private var touched = false

    init {
        // Resume an interrupted onboarding with the saved values (D-04)
        viewModelScope.launch {
            val saved = try { repo.profile.first() } catch (_: IOException) { null } ?: return@launch
            if (!touched) _state.update { it.copy(name = saved.firstName, gender = saved.gender, check = checkName(saved.firstName)) }
        }
    }

    fun onNameChange(text: String) {
        if (text.length > NAME_MAX) return
        touched = true
        val check = checkName(text)
        // A shown error stays while the name is still wrong, and hides once it is valid or empty
        _state.update { it.copy(name = text, check = check, showError = it.showError && check.isError()) }
    }

    /** Tapping the selected tile clears the choice (gender is optional). */
    fun onGenderTap(gender: Gender) {
        touched = true
        _state.update { it.copy(gender = if (it.gender == gender) null else gender) }
    }

    /** Focus left the field or Done was pressed; an empty field never shows an error. */
    fun revealError() {
        _state.update { it.copy(showError = it.check.isError()) }
    }

    fun save(next: () -> Unit) {
        val s = _state.value
        if (s.check != NameCheck.OK) return
        viewModelScope.launch {
            try {
                repo.saveName(s.name.trim(), s.gender)
                _state.update { it.copy(saveFailed = false) }
                next()
            } catch (_: IOException) {
                _state.update { it.copy(saveFailed = true) }
            }
        }
    }
}
