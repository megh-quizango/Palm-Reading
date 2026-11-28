package app.palm.feature.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.contract.PalmReadingDto
import app.palm.core.photo.PhotoDecoder
import app.palm.core.reading.ContractJson
import app.palm.core.reading.ReadingRules
import app.palm.core.reading.ReadingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.serialization.SerializationException

sealed interface ResultState {
    data object Loading : ResultState
    /** No row, or a row that fails the strict decode, the rules or the verdict (D-12): never a crash. */
    data object Missing : ResultState
    data class Found(val reading: PalmReadingDto, val photo: PhotoState) : ResultState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ResultViewModel @Inject constructor(store: ReadingStore, decoder: PhotoDecoder) : ViewModel() {
    private val id = MutableStateFlow<String?>(null)

    fun load(readingId: String) {
        id.value = readingId
    }

    val state: StateFlow<ResultState> = id.filterNotNull()
        .flatMapLatest { store.byId(it) }
        .distinctUntilChanged() // Room re-emits byId on any table write; the photo is decoded once per row
        .transformLatest { saved ->
            val reading = saved?.let { decodeValid(it.json) }
            if (saved == null || reading == null) {
                emit(ResultState.Missing)
                return@transformLatest
            }
            emit(ResultState.Found(reading, PhotoState.Loading))
            val image = saved.photo?.let { decoder.decode(it) }
            emit(ResultState.Found(reading, if (image == null) PhotoState.Unavailable else PhotoState.Ready(image)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultState.Loading)

    private fun decodeValid(json: String): PalmReadingDto? {
        val reading = try {
            ContractJson.decodeFromString<PalmReadingDto>(json)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        return reading.takeIf { it.verdict.ok && ReadingRules.check(it).isEmpty() }
    }
}
