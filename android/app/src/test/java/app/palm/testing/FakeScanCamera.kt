package app.palm.testing

import app.palm.core.camera.ScanCamera
import app.palm.core.photo.PhotoSource
import java.io.IOException
import kotlinx.coroutines.awaitCancellation

/** Records focus points and torch calls; focus and capture behaviour are switchable. */
class FakeScanCamera(override val hasFlash: Boolean = true) : ScanCamera {
    enum class Focus { Return, NeverReturn, Throw }

    var focus = Focus.Return
    var failCapture = false
    val photo = PhotoSource.Capture(byteArrayOf(1, 2, 3), 90)
    val focusPoints = mutableListOf<Pair<Float, Float>>()
    val torchCalls = mutableListOf<Boolean>()
    var takeCount = 0

    override suspend fun focusAt(viewX: Float, viewY: Float) {
        focusPoints += viewX to viewY
        when (focus) {
            Focus.Return -> Unit
            Focus.NeverReturn -> awaitCancellation()
            Focus.Throw -> throw IllegalStateException("fixed-focus lens")
        }
    }

    override suspend fun takeJpeg(): PhotoSource.Capture {
        takeCount++
        if (failCapture) throw IOException("fake capture failure")
        return photo
    }

    override fun setTorch(on: Boolean) {
        torchCalls += on
    }
}
