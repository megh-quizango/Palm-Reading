package app.palm.feature.onboarding

import android.view.KeyEvent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.palm.MainActivity
import app.palm.core.data.Gender
import app.palm.core.data.ProfileEntryPoint
import app.palm.core.data.ProfileRepository
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The whole first run on the real graph: Language -> Intro -> Name -> Birth -> Home. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val repo: ProfileRepository = EntryPointAccessors
        .fromApplication(instrumentation.targetContext.applicationContext, ProfileEntryPoint::class.java)
        .profileRepository()
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun setLocales(tags: String) =
        instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags)) }

    private fun waitForText(text: String) = compose.waitUntilAtLeastOneExists(hasText(text, substring = true), 10_000)

    @Before fun start() {
        runBlocking { repo.clear() }
        setLocales("en")
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun reset() {
        scenario.close()
        setLocales("")
    }

    private fun back() {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun continueInEnglish() {
        // The locale from @Before can arrive after the first frame (API 33+), so pick English explicitly
        waitForText("हिन्दी")
        compose.onNodeWithText("English").performClick()
        waitForText("Continue in English")
        compose.onNodeWithText("Continue in English").performClick()
    }

    @Test fun firstRunReachesHomeWithProfileSaved() {
        continueInEnglish()

        waitForText("Your palm, your story")
        compose.onNodeWithText("Your palm, your story").performTouchInput { swipeLeft() }
        waitForText("Your stars, day by day")
        compose.onNodeWithText("Next").performClick()
        waitForText("Meet [Guide]")
        compose.onNodeWithText("Get started").performClick()

        waitForText("What should we call you?")
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("A")
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("sha")
        compose.onNode(hasSetTextAction()).assert(hasText("Asha"))
        compose.onNodeWithText("Continue").assertIsEnabled()
        compose.onNodeWithText("Female").performClick()
        compose.onNodeWithText("Continue").performClick()

        waitForText("When were you born?")
        compose.onNodeWithText("Choose a date").performClick()
        // Day cells merge their semantics into the full date ("Saturday, January 15, 2000"); the "15" text is cleared
        compose.waitUntilAtLeastOneExists(hasText("January 15, 2000", substring = true) and hasClickAction(), 10_000)
        compose.onNode(hasText("January 15, 2000", substring = true) and hasClickAction()).performClick()
        compose.onNodeWithText("Set date").performClick()
        compose.onNodeWithText("Continue").performClick()

        waitForText("Asha")
        compose.waitUntil(10_000) { runBlocking { repo.profile.first().onboardingComplete } }
        val p = runBlocking { repo.profile.first() }
        assertEquals("Asha", p.firstName)
        assertEquals(Gender.FEMALE, p.gender)
        assertEquals(LocalDate.of(2000, 1, 15), p.birthDate)
        assertTrue(p.onboardingComplete)
    }

    @Test fun timeUnknownAndBackWalkTheStepsInOrder() {
        continueInEnglish()
        waitForText("Your palm, your story")
        compose.onNodeWithText("Next").performClick()
        waitForText("Your stars, day by day")
        compose.onNodeWithText("Next").performClick()
        waitForText("Meet [Guide]")
        compose.onNodeWithText("Get started").performClick()
        waitForText("What should we call you?")
        compose.onNode(hasSetTextAction()).performTextInput("Asha")
        compose.onNodeWithText("Continue").performClick()

        waitForText("When were you born?")
        compose.onNodeWithText("Choose a time").assertIsEnabled()
        compose.onNodeWithText("I don't know my birth time").performClick()
        waitForText("No problem.")
        compose.onNodeWithText("Choose a time").assertIsNotEnabled()

        back()
        waitForText("What should we call you?")
        compose.onNode(hasSetTextAction()).assert(hasText("Asha"))
        back()
        waitForText("Get started")
        back()
        waitForText("Choose your language")
    }
}
