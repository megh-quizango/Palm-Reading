package app.palm.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.core.data.AppLanguage
import app.palm.core.data.ProfileRepository
import app.palm.core.locale.AppLocale
import app.palm.core.locale.initialLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val appLocale: AppLocale,
) : ViewModel() {
    private val _selected = MutableStateFlow(initialLanguage(appLocale.current(), null, appLocale.systemLanguage()))
    val selected: StateFlow<AppLanguage> = _selected.asStateFlow()

    private val _saveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    private var touched = false

    init {
        // The saved language outranks the system default once it is read (D-06)
        viewModelScope.launch {
            val saved = try { repo.profile.first().language } catch (_: IOException) { null }
            if (!touched) _selected.value = initialLanguage(appLocale.current(), saved, appLocale.systemLanguage())
        }
    }

    /** From a tap only: switches the UI language at once (may recreate the activity) and stores it. */
    fun select(language: AppLanguage) {
        touched = true
        _selected.value = language
        _saveFailed.value = false
        appLocale.apply(language)
        viewModelScope.launch {
            try {
                repo.setLanguage(language)
            } catch (_: IOException) {
                _saveFailed.value = true
            }
        }
    }

    fun continueTo(next: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.setLanguage(_selected.value)
                _saveFailed.value = false
                next()
            } catch (_: IOException) {
                _saveFailed.value = true
            }
        }
    }
}
