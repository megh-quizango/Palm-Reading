package app.palm.feature.invalidphoto

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.palm.R
import app.palm.contract.InvalidReason
import app.palm.core.designsystem.NocturneTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** READ-03, D-13, D-14: every reason's copy, no disclaimer, and where each action goes. */
@RunWith(AndroidJUnit4::class)
class InvalidPhotoTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private var reason by mutableStateOf(InvalidReason.BLURRY_OR_DARK)
    private var retakes = 0
    private var homes = 0

    private fun s(@StringRes id: Int) = compose.activity.getString(id)

    @Before fun setUp() {
        compose.setContent {
            NocturneTheme { InvalidPhotoScreen(reason, onRetake = { retakes++ }, onHome = { homes++ }) }
        }
    }

    @Test fun everyReasonShowsItsTitleAndTipWithoutADisclaimer() {
        InvalidReason.entries.forEach { r ->
            reason = r
            compose.waitForIdle()
            val copy = reasonCopy(r)
            compose.onNodeWithText(s(R.string.invalid_title)).assertIsDisplayed()
            compose.onNodeWithText(s(copy.title)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(s(copy.tip)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(s(R.string.scan_retake)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(s(R.string.common_back_home)).performScrollTo().assertIsDisplayed()
            compose.onNode(hasText(s(R.string.result_disclaimer), substring = true)).assertDoesNotExist()
        }
    }

    @Test fun retakeAndHomeCallTheirCallbacksOnce() {
        compose.onNodeWithText(s(R.string.scan_retake)).performScrollTo().performClick()
        assertEquals(1 to 0, retakes to homes)
        compose.onNodeWithText(s(R.string.common_back_home)).performScrollTo().performClick()
        assertEquals(1 to 1, retakes to homes)
    }

    @Test fun topBarBackIsRetake() {
        compose.onNodeWithContentDescription(s(R.string.common_back)).performClick()
        assertEquals(1 to 0, retakes to homes)
    }
}
