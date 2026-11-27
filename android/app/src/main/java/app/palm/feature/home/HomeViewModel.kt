package app.palm.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.core.data.Profile
import app.palm.core.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(repo: ProfileRepository) : ViewModel() {
    /** Null until the first read. */
    val profile: StateFlow<Profile?> = repo.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
