package app.palm.feature.result

import android.view.KeyEvent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.palm.MainActivity
import app.palm.contract.PalmReadingDto
import app.palm.contract.TraceSource
import app.palm.core.data.ProfileEntryPoint
import app.palm.core.reading.ReadingEntryPoint
import app.palm.feature.handselect.setEnglish
import app.palm.testing.seedReading
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * S10 Result on the real graph (RES-01, RES-03, RES-04, D-11, D-12): opened from Home's "Your last reading" card.
 * Seeds its own readings and deletes only those (RESEARCH Pitfall 10); performScrollTo before every display check (Pitfall 8).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ResultScreenTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext
    private val profile = EntryPointAccessors.fromApplication(app, ProfileEntryPoint::class.java).profileRepository()
    private val store = EntryPointAccessors.fromApplication(app, ReadingEntryPoint::class.java).readingStore()
    private val created = mutableListOf<String>()
    private val marker = "test-${UUID.randomUUID()}"
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun start() {
        runBlocking {
            profile.clear()
            profile.saveName("Asha", null)
            profile.saveBirth(LocalDate.of(1996, 3, 12), null, timeUnknown = true)
            profile.completeOnboarding()
        }
        setEnglish(instrumentation)
    }

    @After fun reset() {
        scenario?.close()
        runBlocking { created.forEach { store.delete(it) } }
        instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
    }

    /** Seeds a reading (summary = marker so the newest card is ours), optionally deletes its photo, opens it from Home. */
    private fun open(deletePhoto: Boolean = false, width: Int = 960, height: Int = 1280, edit: (PalmReadingDto) -> PalmReadingDto = { it }): String {
        val id = runBlocking { seedReading(app, store, width = width, height = height) { r -> edit(r).let { it.copy(palm = it.palm?.copy(summary = marker)) } } }
        created += id
        if (deletePhoto) assertTrue(File(app.filesDir, "readings/$id.jpg").delete())
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntilAtLeastOneExists(hasContentDescription("Your last reading", substring = true), 10_000)
        compose.onNodeWithContentDescription("Your last reading", substring = true).performScrollTo().performClick()
        compose.waitUntilAtLeastOneExists(hasText(marker), 10_000)
        return id
    }

    private fun wait(m: SemanticsMatcher) = compose.waitUntilAtLeastOneExists(m, 10_000)
    private fun shown(m: SemanticsMatcher) {
        wait(m)
        compose.onNode(m).performScrollTo().assertIsDisplayed()
    }
    private fun shown(text: String) = shown(hasText(text))
    private fun tab(label: String) {
        compose.onNode(hasText(label) and isTab).performScrollTo().performClick()
        compose.waitForIdle()
    }
    private fun disclaimerShown() = shown(hasText("For entertainment only.", substring = true))

    private fun back() {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private val isTab = SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab)

    @Test fun tabsShowTheirCardsAndTheDisclaimer() {
        open()
        compose.onNode(hasText("Palm") and isTab).assertIsSelected()
        shown("Overview")
        listOf("Warm-hearted", "Imaginative", "Adventurous").forEach { shown(it) }
        shown("Your lines")
        shown(hasText("Heart line") and hasText("Approximate"))
        shown(hasText("Fate line") and hasText("Not clearly visible"))
        disclaimerShown()

        tab("Love")
        listOf("How you love", "Ideal partner", "Challenges").forEach { shown(it) }
        disclaimerShown()

        tab("Career")
        listOf("Strengths", "Challenges", "Ideal industries").forEach { shown(it) }
        disclaimerShown()
    }

    @Test fun theTabSurvivesRecreationAndActionsLandRight() {
        open()
        tab("Career")
        scenario!!.recreate()
        wait(hasText("Ideal industries"))
        compose.onNode(hasText("Career") and isTab).assertIsSelected()

        compose.onNode(hasText("Scan again")).performScrollTo().performClick()
        wait(hasText("Which hand should we read?"))
        back()
        wait(hasText("Your palm reading"))

        compose.onNode(hasText("Ask [Guide]")).performScrollTo().performClick()
        wait(hasText("Coming soon"))
        compose.onNode(hasText("OK")).performClick()
        compose.waitUntilDoesNotExist(hasText("Coming soon"), 5_000)

        compose.onNodeWithContentDescription("Back").performClick()
        wait(hasText("Explore your destiny"))
    }

    @Test fun theSampleShowsTheApproximateNote() {
        open()
        shown("Lines are drawn approximately.")
        compose.onNode(hasText("We couldn't trace your lines clearly on this photo.")).assertDoesNotExist()
    }

    // A 9:16 photo is taller than the 55% cap: the box must shrink in width, not overflow onto the top bar and legend
    @Test fun aTallPhotoStaysAboveItsLegend() {
        open(width = 720, height = 1280)
        wait(hasText("Heart"))
        val box = compose.onNodeWithContentDescription("Your palm photo with lines drawn", substring = true).fetchSemanticsNode()
        val chip = compose.onNode(hasText("Heart")).fetchSemanticsNode()
        val root = compose.onAllNodes(SemanticsMatcher("root") { it.parent == null }).fetchSemanticsNodes().first()
        assertTrue("photo ${box.boundsInRoot} overlaps legend ${chip.boundsInRoot}", box.boundsInRoot.bottom <= chip.boundsInRoot.top)
        assertTrue("photo ${box.boundsInRoot} taller than 55% of ${root.size}", box.boundsInRoot.height <= root.size.height * 0.55f + 1f)
        assertTrue("photo ${box.boundsInRoot} not 9:16", kotlin.math.abs(box.boundsInRoot.width / box.boundsInRoot.height - 720f / 1280f) < 0.01f)
    }

    @Test fun noDrawnLinesShowTheNoLinesNote() {
        open { r -> r.copy(lines = r.lines.map { it.copy(trace = emptyList(), traceSource = TraceSource.NONE) }) }
        shown("We couldn't trace your lines clearly on this photo.")
        compose.onNode(hasText("Lines are drawn approximately.")).assertDoesNotExist()
        compose.onNode(hasText("Heart")).assertDoesNotExist()
        shown(hasText("Heart line") and hasText("Not clearly visible"))
    }

    @Test fun aMissingPhotoDegradesButTabsStillWork() {
        open(deletePhoto = true)
        shown("Photo unavailable")
        compose.onNode(hasText("Heart")).assertDoesNotExist() // the legend chip, matched exactly
        compose.onNode(hasText("Lines are drawn approximately.")).assertDoesNotExist()
        disclaimerShown()
        tab("Love")
        shown("How you love")
        tab("Career")
        shown("Ideal industries")
    }
}
