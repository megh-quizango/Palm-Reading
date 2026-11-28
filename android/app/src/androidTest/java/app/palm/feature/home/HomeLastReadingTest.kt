package app.palm.feature.home

import android.view.KeyEvent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.palm.MainActivity
import app.palm.core.data.ProfileEntryPoint
import app.palm.core.reading.ReadingEntryPoint
import app.palm.feature.handselect.setEnglish
import app.palm.testing.seedReading
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Home "Your last reading" (D-19) on the shared debug install: other readings may exist, so the test only checks
 * the reading it creates and deletes only that one (RESEARCH Pitfall 10). The empty case is 05-05's device check.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HomeLastReadingTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext
    private val profile = EntryPointAccessors.fromApplication(app, ProfileEntryPoint::class.java).profileRepository()
    private val store = EntryPointAccessors.fromApplication(app, ReadingEntryPoint::class.java).readingStore()
    private val created = mutableListOf<String>()
    private val marker = "test-${UUID.randomUUID()}"
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before fun start() {
        runBlocking {
            profile.clear()
            profile.saveName("Asha", null)
            profile.saveBirth(LocalDate.of(1996, 3, 12), null, timeUnknown = true)
            profile.completeOnboarding()
            created += seedReading(app, store) { r -> r.copy(palm = r.palm?.copy(summary = marker)) }
        }
        setEnglish(instrumentation)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun reset() {
        scenario.close()
        runBlocking { created.forEach { store.delete(it) } }
        instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
    }

    private fun back() {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private val card = hasContentDescription("Your last reading", substring = true)

    @Test fun cardOpensTheNewestReading() {
        compose.waitUntilAtLeastOneExists(card and hasContentDescription("Right hand", substring = true), 10_000)
        compose.onNodeWithContentDescription("Your last reading", substring = true).performScrollTo().performClick()
        compose.waitUntilAtLeastOneExists(hasText("Your palm reading"), 10_000)
        compose.waitUntilAtLeastOneExists(hasText(marker), 10_000)
        compose.onNodeWithText(marker).performScrollTo()
        back()
        compose.waitUntilAtLeastOneExists(hasText("Read your palm"), 10_000)
    }

    @Test fun aDeletedReadingIsNoLongerOpened() {
        compose.waitUntilAtLeastOneExists(card, 10_000)
        val id = created.removeAt(0)
        runBlocking {
            store.delete(id)
            assertNotEquals(id, store.latest().first()?.id)
        }
        compose.waitForIdle()
        // Readings this test did not create may still show a card; it must not open the deleted one
        if (compose.onAllNodes(card).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("Your last reading", substring = true).performScrollTo().performClick()
            compose.waitUntilAtLeastOneExists(hasText("Your palm reading"), 10_000)
            compose.waitForIdle()
            compose.onNodeWithText(marker).assertDoesNotExist()
        }
    }
}
