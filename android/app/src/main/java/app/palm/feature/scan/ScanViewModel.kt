package app.palm.feature.scan

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.contract.HandSide
import app.palm.core.photo.PhotoOrigin
import app.palm.core.photo.PhotoProcessor
import app.palm.core.photo.PhotoSource
import app.palm.core.photo.ProcessResult
import app.palm.core.photo.ScanResultHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RejectReason { NoHand, Unsupported, Failed }

data class Rejection(val reason: RejectReason, val origin: PhotoOrigin)

/** [ready] is a one-shot: the screen navigates and calls consumeReady(), so a config change never loses it. */
data class ScanUi(val processing: Boolean = false, val rejected: Rejection? = null, val ready: Boolean = false)

/** Runs a capture or an import through the one PhotoProcessor and hands the result to ScanReady (D-10, D-12). */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val processor: PhotoProcessor,
    private val holder: ScanResultHolder,
) : ViewModel() {
    private val _ui = MutableStateFlow(ScanUi())
    val ui: StateFlow<ScanUi> = _ui.asStateFlow()

    @VisibleForTesting internal var io: CoroutineDispatcher = Dispatchers.IO

    private var job: Job? = null
    private var cleanup: Job? = null

    fun process(source: PhotoSource, origin: PhotoOrigin, hand: HandSide) {
        if (_ui.value.processing) return
        _ui.update { it.copy(processing = true, rejected = null, ready = false) }
        val previousCleanup = cleanup
        job = viewModelScope.launch {
            previousCleanup?.join() // a cancelled photo's deletion never races this one's put()
            val reason = when (val result = processor.process(source)) {
                is ProcessResult.Ok -> try {
                    withContext(io) {
                        ensureActive() // a result that arrives after cancel() is dropped
                        holder.put(result.jpeg, result.width, result.height, hand, result.landmarks, origin)
                    }
                    null
                } catch (_: IOException) {
                    RejectReason.Failed
                }
                ProcessResult.NoHand -> RejectReason.NoHand
                ProcessResult.Unsupported -> RejectReason.Unsupported
                ProcessResult.Failed -> RejectReason.Failed
            }
            ensureActive()
            if (reason == null) {
                // No composition-scoped callback here: it would push onto a back stack a rotation already replaced
                _ui.update { it.copy(processing = false, ready = true) }
            } else {
                // Nothing is kept from a refused photo (D-12)
                withContext(io) { holder.clear() }
                _ui.update { it.copy(processing = false, rejected = Rejection(reason, origin)) }
            }
        }
    }

    /** Back while processing: stop, keep nothing, stay on the current screen (D-12). */
    fun cancel() {
        if (!_ui.value.processing) return
        val running = job ?: return
        running.cancel()
        _ui.update { it.copy(processing = false) }
        // NonCancellable: also deletes a capture a late put() may have written just before the cancel landed
        cleanup = viewModelScope.launch(io + NonCancellable) {
            running.join()
            holder.clear()
        }
    }

    fun consumeReady() = _ui.update { it.copy(ready = false) }

    fun dismissRejection() = _ui.update { it.copy(rejected = null) }
}
