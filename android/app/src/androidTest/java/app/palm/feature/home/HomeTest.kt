package app.palm.feature.home

import android.view.KeyEvent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.palm.MainActivity
import app.palm.core.data.ProfileEntryPoint
import app.palm.core.data.ProfileRepository
import app.palm.feature.handselect.setEnglish
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** S06 Home: header, hero, the 7 tiles, Palm reading -> hand selection, the coming-soon sheet over Home. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HomeTest {
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

    private fun waitFor(matcher: SemanticsMatcher) = compose.waitUntilAtLeastOneExists(matcher, 10_000)
    private fun waitForText(text: String) = waitFor(hasText(text, substring = true))

    private fun back() {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    // "Palm reading" is also the hero overline and "Tarot" also the sheet title: match the clickable tile only
    private fun tile(title: String) =
        compose.onNode(hasClickAction() and (hasText(title) or hasContentDescription(title, substring = true)))

    @Test fun showsHeaderHeroAndAllTiles() {
        waitForText("Asha")
        compose.onNodeWithText("[AppName]").assertExists()
        compose.onNodeWithText("Read your palm").assertExists()
        compose.onNodeWithText("Explore your destiny").performScrollTo()
        for (title in listOf("Palm reading", "Tarot", "Horoscope", "Birth chart", "Love test", "Daily report", "Ask [Guide]")) {
            tile(title).performScrollTo()
        }
    }

    @Test fun palmTileOpensHandSelection() {
        waitForText("Asha")
        tile("Palm reading").performScrollTo().performClick()
        waitForText("Which hand should we read?")
        back()
        waitForText("Read your palm")
    }

    @Test fun otherTileOpensComingSoonSheetOverHome() {
        waitForText("Asha")
        tile("Tarot").performScrollTo().performClick()
        waitForText("Coming soon")
        compose.onNode(hasText("Tarot") and isHeading()).assertExists()
        compose.onNodeWithText("Read your palm").assertExists() // Home stays under the sheet
        compose.onNodeWithText("OK").performClick()
        compose.waitUntilDoesNotExist(hasText("Coming soon"), 5_000)

        tile("Tarot").performScrollTo().performClick()
        waitForText("Coming soon")
        back()
        compose.waitUntilDoesNotExist(hasText("Coming soon"), 5_000)
        compose.onNodeWithText("Read your palm").assertExists()
    }
}
