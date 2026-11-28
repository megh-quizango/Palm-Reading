package app.palm.core.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
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
import java.io.InputStream
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

    /** BitmapFactory + ExifInterface instead of ImageDecoder; tests set it to run that path on any device. */
    @VisibleForTesting
    internal var useLegacyDecode: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.P

    /** A file above [MAX_DECODE_PIXELS]: refused before its pixels are decoded. */
    private class TooLarge : RuntimeException()

    override suspend fun process(source: PhotoSource): ProcessResult = withContext(Dispatchers.Default) {
        val decodeError = if (source is PhotoSource.Gallery) ProcessResult.Unsupported else ProcessResult.Failed
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val rotated = if (useLegacyDecode || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                decodeLegacy(source, bitmaps)
            } else {
                rotateCapture(source, decode(source).also(bitmaps::add))
            }.also(bitmaps::add)
            val upright = (if (rotated.config == Bitmap.Config.ARGB_8888) rotated else rotated.copy(Bitmap.Config.ARGB_8888, false))
                .also(bitmaps::add)
            cropAndEncode(upright, bitmaps)
        } catch (e: CancellationException) {
            throw e
        } catch (_: TooLarge) {
            ProcessResult.Unsupported
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
            if (info.size.width.toLong() * info.size.height > MAX_DECODE_PIXELS) throw TooLarge()
            decoder.setTargetSampleSize(sampleSizeFor(info.size.width, info.size.height))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun open(source: PhotoSource): InputStream = when (source) {
        is PhotoSource.Capture -> ByteArrayInputStream(source.jpeg)
        is PhotoSource.Gallery -> context.contentResolver.openInputStream(source.uri) ?: throw IOException("no stream")
    }

    /** API 26-27 (Pattern 6): bounds pass, pixel cap, sampled decode, then the EXIF orientation applied once. */
    private fun decodeLegacy(source: PhotoSource, bitmaps: MutableList<Bitmap>): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(source).use { BitmapFactory.decodeStream(it, null, bounds) }
        // No size means BitmapFactory cannot read it: this is how HEIC fails on API 26-27
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("undecodable")
        if (bounds.outWidth.toLong() * bounds.outHeight > MAX_DECODE_PIXELS) throw TooLarge()
        val orientation = open(source).use(::exifOrientation)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = open(source).use { BitmapFactory.decodeStream(it, null, opts) } ?: throw IOException("undecodable")
        bitmaps.add(decoded)
        val t = exifTransform(orientation)
        // The 04-01 rule: EXIF when it carries a rotation or flip, else the capture's rotationDegrees, never both (A5)
        val m = Matrix()
        if (t != ExifTransform(0, false)) {
            m.postRotate(t.rotationDegrees.toFloat())
            if (t.flipHorizontal) m.postScale(-1f, 1f)
        } else if (source is PhotoSource.Capture && source.rotationDegrees != 0) {
            m.postRotate(source.rotationDegrees.toFloat())
        } else {
            return decoded
        }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
    }

    private fun exifOrientation(stream: InputStream): Int = try {
        ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
    } catch (_: IOException) {
        ExifInterface.ORIENTATION_UNDEFINED
    }

    /** Devices whose HAL writes no EXIF rotation: apply rotationDegrees once, never on top of EXIF (A5). */
    private fun rotateCapture(source: PhotoSource, bitmap: Bitmap): Bitmap {
        if (source !is PhotoSource.Capture || source.rotationDegrees == 0) return bitmap
        val orientation = exifOrientation(ByteArrayInputStream(source.jpeg))
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
