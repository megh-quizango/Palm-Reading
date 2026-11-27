package app.palm.feature.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.palm.MainActivity
import app.palm.core.data.AppLanguage
import app.palm.core.data.ProfileEntryPoint
import app.palm.core.data.ProfileRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The real graph and the real AppCompat locale: tapping Hindi re-renders Language in Hindi without leaving it. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class LanguageSwitchTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val repo: ProfileRepository = EntryPointAccessors
        .fromApplication(instrumentation.targetContext.applicationContext, ProfileEntryPoint::class.java)
        .profileRepository()
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun setLocales(tags: String) =
        instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags)) }

    private fun waitForText(text: String) = compose.waitUntilAtLeastOneExists(hasText(text), 10_000)

    private fun storedLanguage() = runBlocking { repo.profile.first().language }

    @Before fun start() {
        runBlocking { repo.clear() }
        setLocales("en")
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun reset() {
        scenario.close()
        setLocales("")
    }

    @Test fun hindiThenEnglishSwitchInPlace() {
        // On API 33+ the locale set in @Before reaches the process asynchronously, so start from an English tap
        waitForText("हिन्दी")
        compose.onNodeWithText("English").performClick()
        waitForText("Choose your language")

        compose.onNodeWithText("हिन्दी").performClick()
        waitForText("अपनी भाषा चुनें")
        compose.waitUntil(10_000) { storedLanguage() == AppLanguage.HI }

        compose.onNodeWithText("English").performClick()
        waitForText("Choose your language")
        compose.waitUntil(10_000) { storedLanguage() == AppLanguage.EN }
    }
}
