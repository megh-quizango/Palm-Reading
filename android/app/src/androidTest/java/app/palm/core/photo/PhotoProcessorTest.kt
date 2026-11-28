package app.palm.core.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import androidx.heifwriter.HeifWriter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.palm.core.vision.HandDetector
import app.palm.core.vision.HandFrame
import app.palm.core.vision.MediaPipeHandDetector
import app.palm.core.vision.P3
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNoException
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The decode matrix on the real platform, both decode paths (D-10, D-16; research Pattern 6). */
@RunWith(AndroidJUnit4::class)
class PhotoProcessorTest {
    private val target = InstrumentationRegistry.getInstrumentation().targetContext
    private val testAssets = InstrumentationRegistry.getInstrumentation().context.assets
    private val dir = File(target.cacheDir, "test-photos").apply { mkdirs() }

    /** 21 points spanning the whole image, so the crop is the full frame. */
    private class FullFrame(private val nan: Boolean = false) : HandDetector {
        override fun detect(bitmap: Bitmap) = HandFrame(
            points = List(21) { i ->
                val v = i / 20f
                if (nan && i == 7) P3(Float.NaN, v, 0f) else P3(v, v, 0f)
            },
            handedness = null,
            handednessScore = 0f,
            width = bitmap.width,
            height = bitmap.height,
            timestampMs = 0L,
        )
    }

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun processor(legacy: Boolean, detector: HandDetector = FullFrame()) =
        AndroidPhotoProcessor(target, detector).apply { useLegacyDecode = legacy }

    private fun asset(name: String) = testAssets.open("photos/$name").use { it.readBytes() }

    private fun galleryOf(name: String): PhotoSource.Gallery {
        val file = File(dir, name)
        testAssets.open("photos/$name").use { input -> file.outputStream().use { input.copyTo(it) } }
        return PhotoSource.Gallery(Uri.fromFile(file))
    }

    private fun run(source: PhotoSource, legacy: Boolean, detector: HandDetector = FullFrame()) =
        runBlocking { processor(legacy, detector).process(source) }

    private fun bothPaths(block: (legacy: Boolean) -> Unit) = listOf(false, true).forEach(block)

    private fun decodeOut(ok: ProcessResult.Ok): Bitmap = BitmapFactory.decodeByteArray(ok.jpeg, 0, ok.jpeg.size)

    private fun isRed(c: Int) = Color.red(c) > 200 && Color.green(c) < 60 && Color.blue(c) < 60
    private fun isGreen(c: Int) = Color.green(c) > 200 && Color.red(c) < 60 && Color.blue(c) < 60

    /** Red at the top-left, green at the top-right, portrait, long edge <= 1280. */
    private fun assertUprightPortrait(result: ProcessResult, label: String) {
        assertTrue("$label: $result", result is ProcessResult.Ok)
        val ok = result as ProcessResult.Ok
        assertTrue("$label: portrait ${ok.width}x${ok.height}", ok.width < ok.height)
        assertTrue("$label: long edge", maxOf(ok.width, ok.height) <= MAX_EDGE_PX)
        val bmp = decodeOut(ok)
        assertEquals(ok.width, bmp.width)
        assertTrue("$label: top-left red", isRed(bmp.getPixel(8, 8)))
        assertTrue("$label: top-right green", isGreen(bmp.getPixel(bmp.width - 9, 8)))
        assertTrue("$label: landmarks", landmarksValid(ok.landmarks))
    }

    @Test
    fun orientation6GalleryIsUprightAndExifFree() = bothPaths { legacy ->
        val result = run(galleryOf("orient6_gps.jpg"), legacy)
        assertUprightPortrait(result, "legacy=$legacy")
        val out = (result as ProcessResult.Ok).jpeg
        val exif = ExifInterface(ByteArrayInputStream(out))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(exif.latLong)
        assertNull(exif.getAttribute(ExifInterface.TAG_MAKE))
        assertNull(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
        val head = String(out.copyOf(minOf(out.size, 64 * 1024)), Charsets.ISO_8859_1)
        assertFalse("APP1 Exif present", head.contains("Exif\u0000\u0000"))
        assertFalse("APP1 marker present", head.contains("ÿá"))
    }

    @Test
    fun orientation6CaptureAppliesExifOnceAndIgnoresRotationDegrees() = bothPaths { legacy ->
        assertUprightPortrait(run(PhotoSource.Capture(asset("orient6_gps.jpg"), 90), legacy), "legacy=$legacy")
    }

    /** 1200x900, no EXIF (Bitmap.compress writes none): red block bottom-left, green top-left. */
    private fun landscapeCaptureBytes(): ByteArray {
        val bmp = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply {
            drawColor(Color.GRAY)
            drawRect(0f, 800f, 100f, 900f, Paint().apply { color = Color.RED })
            drawRect(0f, 0f, 100f, 100f, Paint().apply { color = Color.GREEN })
        }
        return ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }.toByteArray()
    }

    @Test
    fun captureWithoutExifIsRotatedOnceByRotationDegrees() = bothPaths { legacy ->
        val bytes = landscapeCaptureBytes()
        assertUprightPortrait(run(PhotoSource.Capture(bytes, 90), legacy), "legacy=$legacy")
        val flat = run(PhotoSource.Capture(bytes, 0), legacy) as ProcessResult.Ok
        assertTrue("legacy=$legacy: stays landscape", flat.width > flat.height)
    }

    @Test
    fun hundredMegapixelImportIsSampledDown() = bothPaths { legacy ->
        val result = run(galleryOf("huge_100mp.jpg"), legacy)
        assertTrue("legacy=$legacy: $result", result is ProcessResult.Ok)
        val ok = result as ProcessResult.Ok
        assertTrue(maxOf(ok.width, ok.height) <= MAX_EDGE_PX)
    }

    @Test
    fun decompressionBombIsRefusedFast() = bothPaths { legacy ->
        val source = galleryOf("bomb_256mp.png")
        val result = runBlocking { withTimeout(5_000) { processor(legacy).process(source) } }
        assertEquals("legacy=$legacy", ProcessResult.Unsupported, result)
    }

    @Test
    fun garbageIsUnsupportedAsImportAndFailedAsCapture() = bothPaths { legacy ->
        val junk = Random(7).nextBytes(4096)
        val file = File(dir, "junk.bin").apply { writeBytes(junk) }
        assertEquals("legacy=$legacy", ProcessResult.Unsupported, run(PhotoSource.Gallery(Uri.fromFile(file)), legacy))
        assertEquals("legacy=$legacy", ProcessResult.Failed, run(PhotoSource.Capture(junk, 0), legacy))
    }

    @Test
    fun invalidLandmarksFail() = bothPaths { legacy ->
        assertEquals(ProcessResult.Failed, run(galleryOf("orient6_gps.jpg"), legacy, FullFrame(nan = true)))
    }

    @Test
    fun noHandRejected() {
        val result = run(galleryOf("no_hand.jpg"), legacy = false, detector = MediaPipeHandDetector(target))
        assertEquals(ProcessResult.NoHand, result)
    }

    @Test
    fun heicImport() {
        // heifwriter's minSdk is 29; check it before any HeifWriter class loads
        assumeTrue("HeifWriter needs API 29", Build.VERSION.SDK_INT >= 29)
        assumeTrue("no HEIF decoder", ImageDecoder.isMimeTypeSupported("image/heif"))
        val bmp = Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply {
            drawColor(Color.GRAY)
            drawRect(0f, 0f, 100f, 100f, Paint().apply { color = Color.RED })
            drawRect(800f, 0f, 900f, 100f, Paint().apply { color = Color.GREEN })
        }
        val file = File(dir, "palm.heic")
        try {
            HeifWriter.Builder(file.path, 900, 1200, HeifWriter.INPUT_MODE_BITMAP).setQuality(90).build().use { w ->
                w.start()
                w.addBitmap(bmp)
                w.stop(5_000)
            }
        } catch (e: Exception) {
            assumeNoException("this device cannot encode HEIF", e)
        }
        assertUprightPortrait(run(PhotoSource.Gallery(Uri.fromFile(file)), legacy = false), "heic")
    }
}
