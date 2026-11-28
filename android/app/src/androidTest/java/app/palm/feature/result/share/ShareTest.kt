package app.palm.feature.result.share

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.palm.R
import app.palm.contract.PalmReadingDto
import app.palm.core.designsystem.NocturneTheme
import app.palm.core.reading.ContractJson
import app.palm.feature.result.PhotoState
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** D-21, D-22: the off-screen card renders to a 1080 x 1350 PNG in one overwritten file (RESEARCH A1 gate). */
@RunWith(AndroidJUnit4::class)
class ShareTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val context get() = compose.activity
    private val sample: PalmReadingDto by lazy {
        val text = context.assets.open("fixtures/palm_reading.en.json").use { it.readBytes().decodeToString() }
        ContractJson.decodeFromString<PalmReadingDto>(text)
    }

    /** A plain gradient, never a real hand. */
    private fun photo(): ImageBitmap {
        val b = Bitmap.createBitmap(960, 1280, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply { shader = LinearGradient(0f, 0f, 960f, 1280f, 0xFFC8AA96.toInt(), 0xFF5A4650.toInt(), Shader.TileMode.CLAMP) }
        android.graphics.Canvas(b).drawRect(0f, 0f, 960f, 1280f, paint)
        return b.asImageBitmap()
    }

    private class Job(val reading: PalmReadingDto, val photo: PhotoState)

    private var job by mutableStateOf<Job?>(null)
    private var captured: ImageBitmap? = null
    private var started = false

    /** setContent once per test; each call swaps the job and waits for the captured image, never on frames. */
    private fun capture(reading: PalmReadingDto, photo: PhotoState): ImageBitmap {
        captured = null
        compose.runOnIdle { job = null }
        if (!started) {
            started = true
            compose.setContent {
                NocturneTheme {
                    // ComponentActivity.setContent wraps content, and a 0 x 0 window is never drawn: give it the screen as the app does
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.testTag(HOST)) {
                            job?.let { j -> ShareCapture(j.reading, j.photo) { captured = it } }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { job = Job(reading, photo) }
        compose.waitUntil(timeoutMillis = 5_000) { captured != null }
        return captured!!
    }

    private fun shareDir() = File(context.cacheDir, "share")

    @Test fun cardRendersTo1080x1350PngWithContent() {
        val image = capture(sample, PhotoState.Ready(photo()))
        assertEquals(1080, image.width)
        assertEquals(1350, image.height)
        val file = runBlocking { writeSharePng(context.cacheDir, image) }
        assertEquals(File(shareDir(), "reading.png"), file)
        val decoded = BitmapFactory.decodeFile(file.path)
        assertNotNull(decoded)
        assertEquals(1080, decoded.width)
        assertEquals(1350, decoded.height)
        val colours = HashSet<Int>()
        for (y in 0 until decoded.height step 20) for (x in 0 until decoded.width step 20) colours += decoded.getPixel(x, y)
        assertTrue("only ${colours.size} distinct colours", colours.size >= 50)
    }

    @Test fun twoWritesLeaveOneFileAndEdgeCasesKeepTheSize() {
        val missing = capture(sample, PhotoState.Unavailable)
        assertEquals(1080 to 1350, missing.width to missing.height)
        val long = sample.copy(palm = sample.palm?.copy(summary = "A long summary sentence. ".repeat(24).take(600)))
        val longImage = capture(long, PhotoState.Ready(photo()))
        assertEquals(1080 to 1350, longImage.width to longImage.height)
        runBlocking {
            writeSharePng(context.cacheDir, missing)
            writeSharePng(context.cacheDir, longImage)
        }
        assertEquals(listOf("reading.png"), shareDir().list()!!.toList())
    }

    @Test fun captureHostIsInvisibleToLayoutAndAccessibility() {
        capture(sample, PhotoState.Ready(photo()))
        compose.onNodeWithTag(HOST).assertWidthIsEqualTo(0.dp).assertHeightIsEqualTo(0.dp)
        val title = context.getString(R.string.share_card_title)
        val disclaimer = context.getString(R.string.result_disclaimer)
        val summary = sample.palm!!.summary
        listOf(title, disclaimer, summary, context.getString(R.string.share_card_footer)).forEach { t ->
            assertEquals("visible to semantics: $t", 0, compose.onAllNodes(hasText(t, substring = true)).fetchSemanticsNodes().size)
        }
    }

    private companion object {
        const val HOST = "share-host"
    }
}
