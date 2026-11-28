package app.palm.feature.scan

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RejectReason { NoHand, Unsupported, Failed }

data class Rejection(val reason: RejectReason, val origin: PhotoOrigin)

data class ScanUi(val processing: Boolean = false, val rejected: Rejection? = null)

/** Runs a capture or an import through the one PhotoProcessor and hands the result to ScanReady (D-10, D-12). */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val processor: PhotoProcessor,
    private val holder: ScanResultHolder,
) : ViewModel() {
    private val _ui = MutableStateFlow(ScanUi())
    val ui: StateFlow<ScanUi> = _ui.asStateFlow()

    fun process(source: PhotoSource, origin: PhotoOrigin, hand: HandSide, onReady: () -> Unit) {
        if (_ui.value.processing) return
        _ui.update { it.copy(processing = true, rejected = null) }
        viewModelScope.launch {
            val reason = when (val result = processor.process(source)) {
                is ProcessResult.Ok -> try {
                    withContext(Dispatchers.IO) {
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
            if (reason == null) {
                _ui.update { it.copy(processing = false) }
                onReady()
            } else {
                // Nothing is kept from a refused photo (D-12)
                withContext(Dispatchers.IO) { holder.clear() }
                _ui.update { it.copy(processing = false, rejected = Rejection(reason, origin)) }
            }
        }
    }

    fun dismissRejection() = _ui.update { it.copy(rejected = null) }
}
