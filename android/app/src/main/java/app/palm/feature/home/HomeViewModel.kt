package app.palm.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.core.data.Profile
import app.palm.core.data.ProfileRepository
import app.palm.core.photo.PhotoDecoder
import app.palm.core.reading.ReadingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/** 168 px is the 56 dp thumbnail at 3x density. */
private const val THUMB_EDGE_PX = 168

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(repo: ProfileRepository, store: ReadingStore, decoder: PhotoDecoder) : ViewModel() {
    /** Null until the first read. */
    val profile: StateFlow<Profile?> = repo.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The newest saved reading (D-19), live from Room; null hides the card. Placeholder thumbnail first, then the decoded one. */
    val lastReading: StateFlow<LastReading?> = store.latest()
        .transformLatest { saved ->
            if (saved == null) {
                emit(null)
                return@transformLatest
            }
            val base = LastReading(saved.id, saved.hand, saved.createdAt, null)
            emit(base)
            val thumb = saved.photo?.let { decoder.decode(it, THUMB_EDGE_PX) }
            if (thumb != null) emit(base.copy(thumbnail = thumb))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
