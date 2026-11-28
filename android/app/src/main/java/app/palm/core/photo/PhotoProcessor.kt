package app.palm.core.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import app.palm.core.vision.HandDetector
import app.palm.core.vision.P3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class PhotoOrigin { CAMERA, GALLERY }

sealed interface PhotoSource {
    /** An in-memory capture; [rotationDegrees] is used only when the JPEG carries no EXIF rotation. */
    class Capture(val jpeg: ByteArray, val rotationDegrees: Int) : PhotoSource
    data class Gallery(val uri: Uri) : PhotoSource
}

sealed interface ProcessResult {
    /** Upright JPEG cropped to the hand, long edge <= 1280, no EXIF; landmarks normalised to it. */
    class Ok(val jpeg: ByteArray, val width: Int, val height: Int, val landmarks: List<P3>) : ProcessResult
    data object NoHand : ProcessResult
    data object Unsupported : ProcessResult
    data object Failed : ProcessResult
}

/** One decode -> rotate -> detect -> crop -> scale -> encode pipeline for captures and imports (D-10). */
interface PhotoProcessor {
    suspend fun process(source: PhotoSource): ProcessResult
}

private const val JPEG_QUALITY = 85

class AndroidPhotoProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detector: HandDetector,
) : PhotoProcessor {

    override suspend fun process(source: PhotoSource): ProcessResult = withContext(Dispatchers.Default) {
        // shortcut: API 26-27 has no ImageDecoder; 04-04 adds the BitmapFactory + ExifInterface path
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@withContext ProcessResult.Unsupported
        val decodeError = if (source is PhotoSource.Gallery) ProcessResult.Unsupported else ProcessResult.Failed
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val decoded = decode(source).also(bitmaps::add)
            val rotated = rotateCapture(source, decoded).also(bitmaps::add)
            val upright = (if (rotated.config == Bitmap.Config.ARGB_8888) rotated else rotated.copy(Bitmap.Config.ARGB_8888, false))
                .also(bitmaps::add)
            cropAndEncode(upright, bitmaps)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) { // includes ImageDecoder.DecodeException
            decodeError
        } catch (_: IllegalArgumentException) {
            decodeError
        } catch (_: SecurityException) {
            decodeError
        } catch (_: OutOfMemoryError) {
            ProcessResult.Failed
        } catch (_: RuntimeException) { // the landmarker failed
            ProcessResult.Failed
        } finally {
            bitmaps.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decode(source: PhotoSource): Bitmap {
        val src = when (source) {
            is PhotoSource.Capture -> ImageDecoder.createSource(ByteBuffer.wrap(source.jpeg))
            is PhotoSource.Gallery -> ImageDecoder.createSource(context.contentResolver, source.uri)
        }
        // The decoder applies the EXIF orientation itself; software pixels for MediaPipe (Pitfalls 5, 6)
        return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            decoder.setTargetSampleSize(sampleSizeFor(info.size.width, info.size.height))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    /** Devices whose HAL writes no EXIF rotation: apply rotationDegrees once, never on top of EXIF (A5). */
    private fun rotateCapture(source: PhotoSource, bitmap: Bitmap): Bitmap {
        if (source !is PhotoSource.Capture || source.rotationDegrees == 0) return bitmap
        val orientation = try {
            ExifInterface(ByteArrayInputStream(source.jpeg))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        } catch (_: IOException) {
            ExifInterface.ORIENTATION_UNDEFINED
        }
        if (orientation != ExifInterface.ORIENTATION_UNDEFINED && orientation != ExifInterface.ORIENTATION_NORMAL) return bitmap
        val m = Matrix().apply { postRotate(source.rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    private fun cropAndEncode(upright: Bitmap, bitmaps: MutableList<Bitmap>): ProcessResult {
        val frame = detector.detect(upright) ?: return ProcessResult.NoHand
        val w = upright.width
        val h = upright.height
        val crop = cropFor(frame.points, w, h)
        val cropped = Bitmap.createBitmap(upright, crop.left, crop.top, crop.width, crop.height).also(bitmaps::add)
        val (sw, sh) = scaledSize(crop.width, crop.height)
        val scaled = Bitmap.createScaledBitmap(cropped, sw, sh, true).also(bitmaps::add)
        val out = ByteArrayOutputStream()
        // Re-encoding writes no EXIF, so GPS and camera tags are gone (D-12)
        if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) return ProcessResult.Failed
        val points = frame.points.map { remapToCrop(it, w, h, crop) }
        if (!landmarksValid(points)) return ProcessResult.Failed
        return ProcessResult.Ok(out.toByteArray(), sw, sh, points)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PhotoModule {
    @Provides
    fun processor(impl: AndroidPhotoProcessor): PhotoProcessor = impl

    @Provides
    @Singleton
    fun holder(@ApplicationContext context: Context): ScanResultHolder = ScanResultHolder(File(context.cacheDir, "scan"))
}
