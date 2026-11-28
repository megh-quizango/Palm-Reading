package app.palm.feature.result.share

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasContentDescription
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import app.palm.MainActivity
import app.palm.core.data.ProfileEntryPoint
import app.palm.core.reading.ReadingEntryPoint
import app.palm.feature.handselect.setEnglish
import app.palm.testing.seedReading
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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
import androidx.compose.ui.test.ExperimentalTestApi
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
import app.palm.feature.result.ResultActions
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * D-21, D-22: the off-screen card renders to a 1080 x 1350 PNG in one overwritten file (RESEARCH A1 gate), and is shared
 * only through a per-URI grant on the non-exported provider limited to cacheDir/share (T-05-12, T-05-13).
 */
@OptIn(ExperimentalTestApi::class)
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

    @Test fun sendAndChooserCarryTheUriAndTheReadGrant() {
        val uri = Uri.parse("content://${context.packageName}.share/share/reading.png")
        val send = shareIntent(uri, "text")
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("image/png", send.type)
        assertEquals(uri, IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java))
        assertEquals("text", send.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(uri, send.clipData!!.getItemAt(0).uri)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val chooser = shareChooser(send, "title")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertTrue(IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)!!.filterEquals(send))
        assertEquals(uri, chooser.clipData!!.getItemAt(0).uri)
        assertTrue(chooser.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test fun onlyTheSharePngIsServedByAPrivateProvider() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val image = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888).asImageBitmap()
        val file = runBlocking { writeSharePng(app.cacheDir, image) }
        val uri = shareUri(app, file)
        assertEquals("content", uri.scheme)
        assertEquals("${app.packageName}.share", uri.authority)
        val decoded = app.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
        assertEquals(1080 to 1350, decoded.width to decoded.height)
        assertThrows(IllegalArgumentException::class.java) { shareUri(app, File(app.filesDir, "readings/x.jpg")) }
        val info = app.packageManager.getProviderInfo(ComponentName(app, FileProvider::class.java), 0)
        assertEquals("${app.packageName}.share", info.authority)
        assertFalse(info.exported)
        assertTrue(info.grantUriPermissions)
    }

    @Test fun tappingShareWritesTheCardAndOpensTheShareSheet() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext
        val profile = EntryPointAccessors.fromApplication(app, ProfileEntryPoint::class.java).profileRepository()
        val store = EntryPointAccessors.fromApplication(app, ReadingEntryPoint::class.java).readingStore()
        val id = runBlocking {
            profile.clear()
            profile.saveName("Asha", null)
            profile.saveBirth(LocalDate.of(1996, 3, 12), null, timeUnknown = true)
            profile.completeOnboarding()
            seedReading(app, store)
        }
        val png = File(app.cacheDir, "share/reading.png").apply { delete() }
        setEnglish(instrumentation)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitUntilAtLeastOneExists(hasContentDescription("Your last reading", substring = true), 10_000)
            compose.onNodeWithContentDescription("Your last reading", substring = true).performScrollTo().performClick()
            compose.waitUntilAtLeastOneExists(hasText("Your palm reading"), 10_000)
            compose.onNodeWithText("Share").performScrollTo().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { png.exists() }
            val decoded = BitmapFactory.decodeFile(png.path)
            assertEquals(1080 to 1350, decoded.width to decoded.height)
            // The system chooser (another app; on API 35 the sheet is intentresolver's ChooserActivityLauncher) covers Result:
            // once it is the top resumed activity close it with a
            // shell key event, wait for Result to resume (a paused activity has no hierarchy to query), then check the button
            assertTrue("share sheet did not open", waitFor { CHOOSER.containsMatchIn(topActivity()) })
            instrumentation.uiAutomation.executeShellCommand("input keyevent ${KeyEvent.KEYCODE_BACK}").close()
            assertTrue("Result did not come back", waitFor { scenario.state == Lifecycle.State.RESUMED })
            compose.waitUntilAtLeastOneExists(hasText("Share"), 10_000)
            assertEquals(0, compose.onAllNodes(hasText(app.getString(R.string.result_share_preparing))).fetchSemanticsNodes().size)
        } finally {
            scenario.close()
            runBlocking { store.delete(id) }
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
        }
    }

    @Test fun shareTappedWhileThePhotoDecodesWaitsForIt() {
        var state by mutableStateOf<PhotoState>(PhotoState.Loading)
        val png = File(shareDir(), "reading.png").apply { delete() }
        compose.setContent { NocturneTheme { Box(Modifier.fillMaxSize()) { ResultActions(sample, state, {}, {}) } } }
        compose.onNodeWithText(context.getString(R.string.result_share)).performClick()
        compose.waitForIdle()
        // Still Loading: no "Photo unavailable" card is captured or written
        compose.onNodeWithText(context.getString(R.string.result_share_preparing)).assertExists()
        assertFalse(png.exists())
        compose.runOnIdle { state = PhotoState.Ready(photo()) }
        compose.waitUntil(timeoutMillis = 10_000) { png.exists() }
        assertTrue("share sheet did not open", waitFor { CHOOSER.containsMatchIn(topActivity()) })
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent ${KeyEvent.KEYCODE_BACK}").close()
    }

    private fun waitFor(condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return false
    }

    private fun topActivity(): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("dumpsys activity activities")
        val text = ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { it.readText() }
        return text.lineSequence().firstOrNull { "topResumedActivity" in it }.orEmpty()
    }

    private companion object {
        val CHOOSER = Regex("""[Cc]hooser|[Rr]esolver|intentresolver""")
        const val HOST = "share-host"
    }
}
