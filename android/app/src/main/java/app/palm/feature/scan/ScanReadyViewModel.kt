package app.palm.feature.scan

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.contract.HandSide
import app.palm.contract.InvalidReason
import app.palm.core.data.AppLanguage
import app.palm.core.locale.AppLocale
import app.palm.core.photo.PhotoDecoder
import app.palm.core.photo.ProcessedCapture
import app.palm.core.photo.ScanResultHolder
import app.palm.core.reading.PalmReadingRepository
import app.palm.core.reading.ReadingRules
import app.palm.core.reading.ReadingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The "Read my palm" action (D-03). */
sealed interface ReadState {
    data object Idle : ReadState
    data object Reading : ReadState
    data class Saved(val readingId: String) : ReadState
    data object Failed : ReadState

    /** Unusable photo (D-14): nothing saved, the capture already deleted (D-15). */
    data class Invalid(val reason: InvalidReason, val hand: HandSide) : ReadState
}

@HiltViewModel
class ScanReadyViewModel @Inject constructor(
    private val holder: ScanResultHolder,
    private val repository: PalmReadingRepository,
    private val store: ReadingStore,
    private val locale: AppLocale,
    decoder: PhotoDecoder,
) : ViewModel() {
    @VisibleForTesting internal var io: CoroutineDispatcher = Dispatchers.IO

    val capture: ProcessedCapture? = holder.latest

    private val _bitmap = MutableStateFlow<ImageBitmap?>(null)
    val bitmap: StateFlow<ImageBitmap?> = _bitmap.asStateFlow()

    /** No capture (process death: the holder was cleared at app start) or its file no longer decodes. */
    private val _gone = MutableStateFlow(capture == null)
    val gone: StateFlow<Boolean> = _gone.asStateFlow()

    private val _readState = MutableStateFlow<ReadState>(ReadState.Idle)
    val readState: StateFlow<ReadState> = _readState.asStateFlow()

    init {
        val file = capture?.file
        if (file != null) {
            viewModelScope.launch {
                val image = decoder.decode(file)
                if (image == null) _gone.value = true else _bitmap.value = image
            }
        }
    }

    /** Reads, checks and saves; only from Idle or Failed, so a second tap starts nothing. */
    fun read() {
        val c = capture ?: return
        if (holder.latest == null) return // already saved or deleted as invalid
        val state = _readState.value
        if (state != ReadState.Idle && state != ReadState.Failed) return
        _readState.value = ReadState.Reading
        viewModelScope.launch {
            _readState.value = try {
                val reading = repository.read(c, locale.current() ?: AppLanguage.EN)
                val verdict = reading.verdict
                when {
                    ReadingRules.check(reading).isNotEmpty() -> ReadState.Failed
                    verdict.ok -> {
                        // NonCancellable: leaving the screen must not stop a save halfway (RESEARCH Pitfall 2)
                        val id = withContext(io + NonCancellable) {
                            store.save(reading, c).also { holder.clear() }
                        }
                        ReadState.Saved(id)
                    }
                    else -> {
                        // Never saved, never counted; deleted now, before InvalidPhoto opens (D-15, RESEARCH Pitfall 11)
                        withContext(io + NonCancellable) { holder.clear() }
                        ReadState.Invalid(requireNotNull(verdict.reason), c.hand)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ReadState.Failed
            }
        }
    }

    fun consumeSaved() {
        if (_readState.value is ReadState.Saved) _readState.value = ReadState.Idle
    }

    fun consumeInvalid() {
        if (_readState.value is ReadState.Invalid) _readState.value = ReadState.Idle
    }

    /** Deletes the capture (D-12). NonCancellable: the screen pops right after, which clears this scope. */
    fun retake() {
        if (_readState.value == ReadState.Reading) return
        viewModelScope.launch(io + NonCancellable) { holder.clear() }
    }
}
