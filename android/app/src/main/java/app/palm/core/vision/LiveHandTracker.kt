package app.palm.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.palm.BuildConfig
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val MIN_FRAME_GAP_MS = 83L // at most 12 fps into MediaPipe (D-03)
private const val STATS_EVERY_MS = 5_000L
private const val TAG = "PalmScan"

/**
 * CameraX analyzer feeding a LIVE_STREAM landmarker on its own single thread (D-03, D-13).
 * Set as the analyzer with [executor]; the caller clears the analyzer before [close].
 */
class LiveHandTracker(
    private val context: Context,
    private val onFrame: (frame: HandFrame?, timestampMs: Long) -> Unit,
    private val onError: (Throwable) -> Unit,
) : ImageAnalysis.Analyzer, Closeable {
    val executor: ExecutorService = Executors.newSingleThreadExecutor()

    /** False below RESUMED or while a photo is processing: frames are dropped unread. */
    @Volatile var active: Boolean = false

    @Volatile private var closed = false
    private var landmarker: HandLandmarker? = null // touched only on [executor]
    private var lastMs = 0L

    // Debug-only counters: counts and timings, never points, labels or pixels (D-12)
    private var statsFrom = 0L
    private var statsFrames = 0
    private var statsInferenceMs = 0L

    fun start() {
        executor.execute {
            if (closed) return@execute
            try {
                landmarker = HandLandmarker.createFromOptions(
                    context,
                    landmarkerOptions(RunningMode.LIVE_STREAM)
                        .setResultListener { result, input ->
                            if (closed) return@setResultListener
                            val ts = result.timestampMs()
                            if (BuildConfig.DEBUG) stats(SystemClock.uptimeMillis() - ts)
                            onFrame(result.toHandFrame(input.width, input.height, ts), ts)
                        }
                        .setErrorListener { e -> if (!closed) onError(e) }
                        .build(),
                )
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    override fun analyze(image: ImageProxy) {
        try {
            val target = landmarker ?: return
            val now = SystemClock.uptimeMillis()
            if (!active || closed || now - lastMs < MIN_FRAME_GAP_MS) return
            lastMs = now
            val src = image.toBitmap() // RGBA row-stride safe, no YUV code
            val degrees = image.imageInfo.rotationDegrees
            val upright = if (degrees == 0) {
                src
            } else {
                Bitmap.createBitmap(src, 0, 0, src.width, src.height, Matrix().apply { postRotate(degrees.toFloat()) }, false)
            }
            target.detectAsync(BitmapImageBuilder(upright).build(), now)
        } catch (e: Exception) {
            if (!closed) onError(e)
        } finally {
            image.close()
        }
    }

    private fun stats(inferenceMs: Long) {
        val now = SystemClock.uptimeMillis()
        if (statsFrom == 0L) statsFrom = now
        statsFrames++
        statsInferenceMs += inferenceMs
        if (now - statsFrom >= STATS_EVERY_MS) {
            Log.i(TAG, "analyzed $statsFrames frames, inference ${statsInferenceMs / statsFrames} ms")
            statsFrom = now
            statsFrames = 0
            statsInferenceMs = 0
        }
    }

    override fun close() {
        closed = true
        executor.execute {
            landmarker?.close()
            landmarker = null
        }
        executor.shutdown()
    }
}
