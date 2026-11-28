package app.palm.core.camera

import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import app.palm.core.photo.PhotoSource
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Focus, torch and capture for the scanner (D-05), behind an interface so the ViewModel is testable. */
interface ScanCamera {
    val hasFlash: Boolean
    suspend fun focusAt(viewX: Float, viewY: Float)
    suspend fun takeJpeg(): PhotoSource.Capture
    fun setTorch(on: Boolean)
}

/** CameraX implementation over the scanner's bound controller. Call on the main thread. */
class CameraXScanCamera(
    private val controller: LifecycleCameraController,
    private val previewView: PreviewView,
    private val captureExecutor: Executor,
) : ScanCamera {
    override val hasFlash: Boolean get() = controller.cameraInfo?.hasFlashUnit() == true

    /** Throws or never completes on fixed-focus lenses; the caller caps it and captures anyway (research Pattern 4). */
    override suspend fun focusAt(viewX: Float, viewY: Float) {
        val control = controller.cameraControl ?: return
        val point = previewView.meteringPointFactory.createPoint(viewX, viewY)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE).build()
        control.startFocusAndMetering(action).await()
    }

    override suspend fun takeJpeg(): PhotoSource.Capture = suspendCancellableCoroutine { cont ->
        controller.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // the processor applies rotationDegrees only when the JPEG has no EXIF rotation (04-01)
                    val result = runCatching {
                        val buffer = image.planes[0].buffer
                        PhotoSource.Capture(ByteArray(buffer.remaining()).also(buffer::get), image.imageInfo.rotationDegrees)
                    }
                    image.close()
                    result.fold({ cont.resume(it) }, { cont.resumeWithException(it) })
                }

                override fun onError(exception: ImageCaptureException) = cont.resumeWithException(exception)
            },
        )
    }

    override fun setTorch(on: Boolean) {
        controller.enableTorch(on)
    }
}
