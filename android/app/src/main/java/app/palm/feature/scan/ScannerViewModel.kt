package app.palm.feature.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.core.vision.GuidanceState
import app.palm.core.vision.HandFrame
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val CAPTURE_ERROR_MS = 4_000L // UI-SPEC: the capture-failed chip stays up this long

data class ScannerUi(
    val shown: GuidanceState = GuidanceState.NoHand,
    val captureEnabled: Boolean = false,
    val guideUnavailable: Boolean = false,
    val capturing: Boolean = false,
    val captureFailed: Boolean = false,
)

/** Tracer behaviour: raw palm / no palm per result. 04-02 replaces it with the classifier and stabiliser. */
@HiltViewModel
class ScannerViewModel @Inject constructor() : ViewModel() {
    private val _ui = MutableStateFlow(ScannerUi())
    val ui: StateFlow<ScannerUi> = _ui.asStateFlow() // StateFlow drops equal values: no emission per unchanged frame
    private var failedJob: Job? = null

    /** Called from the landmarker thread; [timestampMs] feeds 04-02's stabiliser. */
    fun onFrame(frame: HandFrame?, @Suppress("UNUSED_PARAMETER") timestampMs: Long) {
        val shown = if (frame != null) GuidanceState.PalmDetected else GuidanceState.NoHand
        _ui.update {
            if (it.shown == shown) it else enabled(it.copy(shown = shown, captureFailed = false))
        }
    }

    fun onGuideError() = _ui.update { enabled(it.copy(guideUnavailable = true)) }

    fun onCaptureStarted() = _ui.update { enabled(it.copy(capturing = true, captureFailed = false)) }

    fun onCaptureDone() = _ui.update { enabled(it.copy(capturing = false)) }

    fun onCaptureFailed() {
        _ui.update { enabled(it.copy(capturing = false, captureFailed = true)) }
        failedJob?.cancel()
        failedJob = viewModelScope.launch {
            delay(CAPTURE_ERROR_MS)
            _ui.update { it.copy(captureFailed = false) }
        }
    }

    private fun enabled(ui: ScannerUi) =
        ui.copy(captureEnabled = ui.shown == GuidanceState.PalmDetected && !ui.capturing && !ui.guideUnavailable)
}
