package app.palm.feature.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.contract.HandSide
import app.palm.core.camera.ScanCamera
import app.palm.core.photo.PhotoSource
import app.palm.core.vision.FillCenterMapping
import app.palm.core.vision.GuidanceReading
import app.palm.core.vision.GuidanceStabilizer
import app.palm.core.vision.GuidanceState
import app.palm.core.vision.HandFrame
import app.palm.core.vision.HandGuidance
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

const val CAPTURE_ERROR_MS = 4_000L // UI-SPEC: the capture-failed chip stays up this long

/** Shown (stabilised) scanner state. Changes only when the chip, hint or shutter would change, never per frame. */
data class ScannerUi(
    val shown: GuidanceState = GuidanceState.NoHand,
    val captureEnabled: Boolean = false,
    val wrongHand: HandSide? = null,
    val guideUnavailable: Boolean = false,
    val capturing: Boolean = false,
    val captureFailed: Boolean = false,
    val focusPoint: Pair<Float, Float>? = null, // view pixels; the overlay draws the focus ring there
    val hasFlash: Boolean = false,
    val torchOn: Boolean = false,
)

/** Per-frame sample for the debug HUD (04-05) only; never logged. */
data class ScanDebugSample(val frame: HandFrame?, val reading: GuidanceReading, val viewWidth: Int, val viewHeight: Int)

/** Live guidance (D-07, D-08): frame -> HandGuidance -> GuidanceStabilizer -> ScannerUi. */
@HiltViewModel
class ScannerViewModel @Inject constructor() : ViewModel() {
    private val _ui = MutableStateFlow(ScannerUi())
    val ui: StateFlow<ScannerUi> = _ui.asStateFlow() // StateFlow drops equal values: no emission per unchanged frame
    private val _debug = MutableStateFlow<ScanDebugSample?>(null)
    val debug: StateFlow<ScanDebugSample?> = _debug.asStateFlow()
    private var failedJob: Job? = null

    // onFrame runs on the landmarker thread, setHand / onViewSize on main
    private val lock = Any()
    private var hand: HandSide? = null
    private var guidance: HandGuidance? = null
    private var stabilizer = GuidanceStabilizer()
    private var viewWidth = 0
    private var viewHeight = 0
    private var stableCapture = false
    private var palmPx: Pair<Float, Float>? = null // last palm centre in analysis-image pixels
    private var frameWidth = 0
    private var frameHeight = 0
    private var focusJob: Job? = null

    fun setHand(hand: HandSide) {
        synchronized(lock) {
            if (this.hand == hand) return
            this.hand = hand
            guidance = HandGuidance(hand)
            stabilizer = GuidanceStabilizer()
            stableCapture = false
            _ui.update { enabled(it.copy(shown = GuidanceState.NoHand, wrongHand = null)) }
        }
    }

    fun onViewSize(width: Int, height: Int) {
        synchronized(lock) {
            viewWidth = width
            viewHeight = height
        }
    }

    /** Called from the landmarker thread with the frame timestamp. Frames before [setHand] are ignored. */
    fun onFrame(frame: HandFrame?, timestampMs: Long) {
        synchronized(lock) {
            val g = guidance ?: return
            val visible = if (frame != null && viewWidth > 0 && viewHeight > 0) {
                FillCenterMapping(frame.width, frame.height, viewWidth, viewHeight).visibleRect()
            } else {
                null
            }
            val reading = g.read(frame, visible)
            val stable = stabilizer.update(reading.raw, reading.wrongHand, timestampMs)
            stableCapture = stable.captureEnabled
            val centre = reading.palmCenter
            if (frame != null && centre != null) {
                palmPx = centre.x * frame.width to centre.y * frame.height
                frameWidth = frame.width
                frameHeight = frame.height
            }
            _ui.update {
                val changed = it.shown != stable.shown
                enabled(it.copy(shown = stable.shown, wrongHand = stable.wrongHand, captureFailed = it.captureFailed && !changed))
            }
            _debug.value = ScanDebugSample(frame, reading, viewWidth, viewHeight)
        }
    }

    fun onGuideError() = _ui.update { enabled(it.copy(guideUnavailable = true)) }

    /** Called once the camera is bound: the torch toggle shows only with a flash unit. */
    fun onCameraReady(camera: ScanCamera) = _ui.update { it.copy(hasFlash = camera.hasFlash) }

    /**
     * Focus-then-capture (D-05): only while capture is enabled; focus and metering at the palm centre for at
     * most 1 s (a failed or unsupported focus still captures: fixed-focus lenses), then the JPEG.
     */
    fun capture(camera: ScanCamera, onCaptured: (PhotoSource.Capture) -> Unit) {
        if (!_ui.value.captureEnabled) return
        focusJob?.cancel()
        val point = palmViewPoint()
        _ui.update { enabled(it.copy(capturing = true, captureFailed = false, focusPoint = point)) }
        viewModelScope.launch {
            val photo = try {
                if (point != null) focusCapped(camera, point.first, point.second)
                camera.takeJpeg()
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                null
            }
            _ui.update { enabled(it.copy(capturing = false, focusPoint = null)) }
            if (photo == null) showCaptureFailed() else onCaptured(photo)
        }
    }

    /** Tap-to-focus at a view point; the ring shows until the result or the 1 s cap. */
    fun focusAt(camera: ScanCamera, viewX: Float, viewY: Float) {
        if (_ui.value.capturing) return
        focusJob?.cancel()
        _ui.update { it.copy(focusPoint = viewX to viewY) }
        focusJob = viewModelScope.launch {
            focusCapped(camera, viewX, viewY)
            _ui.update { it.copy(focusPoint = null) }
        }
    }

    fun toggleTorch(camera: ScanCamera) {
        if (!camera.hasFlash) return
        val on = !_ui.value.torchOn
        camera.setTorch(on)
        _ui.update { it.copy(torchOn = on) }
    }

    fun torchOff(camera: ScanCamera) {
        camera.setTorch(false)
        _ui.update { it.copy(torchOn = false) }
    }

    private suspend fun focusCapped(camera: ScanCamera, x: Float, y: Float) {
        try {
            withTimeoutOrNull(1_000) { camera.focusAt(x, y) }
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive() // a failed or cancelled focus future is not our cancellation
        }
    }

    private fun palmViewPoint(): Pair<Float, Float>? = synchronized(lock) {
        val p = palmPx
        if (p == null || viewWidth <= 0 || viewHeight <= 0 || frameWidth <= 0) {
            null
        } else {
            FillCenterMapping(frameWidth, frameHeight, viewWidth, viewHeight).imageToView(p.first, p.second)
        }
    }

    private fun showCaptureFailed() {
        _ui.update { enabled(it.copy(captureFailed = true)) }
        failedJob?.cancel()
        failedJob = viewModelScope.launch {
            delay(CAPTURE_ERROR_MS)
            _ui.update { it.copy(captureFailed = false) }
        }
    }

    private fun enabled(ui: ScannerUi) = ui.copy(captureEnabled = stableCapture && !ui.capturing && !ui.guideUnavailable)
}
