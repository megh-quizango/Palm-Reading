package app.palm.feature.handselect

import android.view.KeyEvent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.palm.MainActivity
import app.palm.core.data.ProfileEntryPoint
import app.palm.core.data.ProfileRepository
import app.palm.core.designsystem.NocturneColors
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Home hero -> hand selection (Right by default, tips) -> Scan(hand) on opaque bg.camera -> Back. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HandSelectTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val repo: ProfileRepository = EntryPointAccessors
        .fromApplication(instrumentation.targetContext.applicationContext, ProfileEntryPoint::class.java)
        .profileRepository()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before fun start() {
        runBlocking {
            repo.clear()
            repo.saveName("Asha", null)
            repo.saveBirth(LocalDate.of(1996, 3, 12), null, timeUnknown = true)
            repo.completeOnboarding()
        }
        setEnglish(instrumentation)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun reset() {
        scenario.close()
        instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
    }

    private fun waitForText(text: String) = compose.waitUntilAtLeastOneExists(hasText(text, substring = true), 10_000)

    private fun back() {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    @Test fun heroOpensHandSelectionAndScanCarriesTheHand() {
        waitForText("Scan my palm")
        compose.onNodeWithText("Scan my palm").performClick()

        waitForText("Which hand should we read?")
        compose.onNodeWithText("Right hand").assertIsSelected()
        compose.onNodeWithText("Left hand").assertIsNotSelected()
        for (tip in listOf("Good photo", "Too far", "Blurry or dark", "Fingers closed", "Jewellery or bent")) {
            compose.onNodeWithText(tip).performScrollTo()
        }

        compose.onNodeWithText("Left hand").performScrollTo().performClick()
        compose.onNodeWithText("Left hand").assertIsSelected()
        compose.onNodeWithText("Continue").performClick()

        waitForText("Scanner arrives in the next update")
        waitForText("Hand: Left hand")
        compose.waitForIdle()
        val image = compose.onRoot().captureToImage()
        assertEquals(NocturneColors.BgCamera, image.toPixelMap()[0, image.height / 2])

        back()
        waitForText("Which hand should we read?")
    }
}

/**
 * Sets the app locale to English and waits until the process sees it: on API 33+ the change reaches
 * the process asynchronously, so an activity launched straight after could still start in Hindi.
 */
internal fun setEnglish(instrumentation: android.app.Instrumentation) {
    instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
    val deadline = System.currentTimeMillis() + 5_000
    while (instrumentation.targetContext.resources.configuration.locales[0].language != "en" &&
        System.currentTimeMillis() < deadline
    ) {
        Thread.sleep(50)
    }
}
