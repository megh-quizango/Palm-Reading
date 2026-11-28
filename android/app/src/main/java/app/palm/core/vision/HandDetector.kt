package app.palm.core.vision

import android.content.Context
import android.graphics.Bitmap
import app.palm.contract.HandSide
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/** Bundled float16 Hand Landmarker (D-06), stored uncompressed in the APK. */
const val HAND_MODEL_ASSET = "hand_landmarker.task"

/** IMAGE-mode detection on a still. ARGB_8888 software bitmap in; null = no hand; throws when the landmarker fails. */
interface HandDetector {
    fun detect(bitmap: Bitmap): HandFrame?
}

internal fun landmarkerOptions(mode: RunningMode): HandLandmarker.HandLandmarkerOptions.Builder =
    HandLandmarker.HandLandmarkerOptions.builder()
        .setBaseOptions(BaseOptions.builder().setModelAssetPath(HAND_MODEL_ASSET).setDelegate(Delegate.CPU).build())
        .setRunningMode(mode)
        .setNumHands(1)
        .setMinHandDetectionConfidence(0.5f)
        .setMinHandPresenceConfidence(0.5f)
        .setMinTrackingConfidence(0.5f)

class MediaPipeHandDetector @Inject constructor(@ApplicationContext private val context: Context) : HandDetector {
    // One landmarker per still: a capture or import is rare, and nothing stays in memory between scans
    override fun detect(bitmap: Bitmap): HandFrame? {
        val landmarker = HandLandmarker.createFromOptions(context, landmarkerOptions(RunningMode.IMAGE).build())
        try {
            return landmarker.detect(BitmapImageBuilder(bitmap).build()).toHandFrame(bitmap.width, bitmap.height, 0L)
        } finally {
            landmarker.close()
        }
    }
}

/** First hand only (numHands 1); the raw model label, no swap. */
internal fun HandLandmarkerResult.toHandFrame(width: Int, height: Int, timestampMs: Long): HandFrame? {
    val hand = landmarks().firstOrNull() ?: return null
    val category = handedness().firstOrNull()?.firstOrNull()
    return HandFrame(
        points = hand.map { P3(it.x(), it.y(), it.z()) },
        handedness = when (category?.categoryName()) {
            "Left" -> HandSide.LEFT
            "Right" -> HandSide.RIGHT
            else -> null
        },
        handednessScore = category?.score() ?: 0f,
        width = width,
        height = height,
        timestampMs = timestampMs,
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class VisionModule {
    @Binds
    abstract fun detector(impl: MediaPipeHandDetector): HandDetector
}
