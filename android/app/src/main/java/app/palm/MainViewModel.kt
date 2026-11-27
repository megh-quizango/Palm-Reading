package app.palm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.core.data.Profile
import app.palm.core.data.ProfileRepository
import app.palm.core.locale.AppLocale
import app.palm.core.navigation.Route
import app.palm.core.navigation.startRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Activity-scoped, so it survives the locale recreate and the splash never blocks the new window (Pitfall 2).
 * [start] is null until the first profile read returns.
 */
@HiltViewModel
class MainViewModel @Inject constructor(repo: ProfileRepository, appLocale: AppLocale) : ViewModel() {
    val start: StateFlow<Route?> = flow {
        val profile = try {
            repo.profile.first()
        } catch (_: IOException) {
            Profile()
        }
        emit(startRoute(profile))
        // AppCompat's locale is the source of truth (it also tracks Android 13+ Settings changes)
        val locale = appLocale.current()
        if (locale != null && locale != profile.language) {
            try {
                repo.setLanguage(locale)
            } catch (_: IOException) {
                // retried on the next launch
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
