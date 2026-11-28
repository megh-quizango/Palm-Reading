package app.palm.feature.scan

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.palm.R
import app.palm.core.designsystem.NocturneTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI-SPEC "Permission and fallback screens": every state's message and actions (SCAN-02, T-04-13). */
@RunWith(AndroidJUnit4::class)
class ScanFallbackTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableMapOf<String, Int>()
    private var screen by mutableStateOf(GateScreen.Requesting)

    private fun s(@StringRes id: Int) = compose.activity.getString(id)
    private fun count(name: String) = calls[name] ?: 0
    private fun hit(name: String): () -> Unit = { calls[name] = count(name) + 1 }

    private fun show(state: GateScreen) {
        screen = state
        compose.setContent {
            NocturneTheme {
                ScanFallbackScreen(
                    screen,
                    onAllow = hit("allow"),
                    onOpenSettings = hit("settings"),
                    onGallery = hit("gallery"),
                    onRetry = hit("retry"),
                    onBack = hit("back"),
                )
            }
        }
    }

    private fun tap(@StringRes id: Int) = compose.onNodeWithText(s(id)).performScrollTo().performClick()
    private fun shown(@StringRes id: Int) = compose.onNodeWithText(s(id)).performScrollTo().assertIsDisplayed()
    private fun absent(@StringRes vararg ids: Int) = ids.forEach { compose.onNodeWithText(s(it)).assertDoesNotExist() }

    @Test fun denied() {
        show(GateScreen.Denied)
        shown(R.string.scan_perm_title)
        shown(R.string.scan_perm_body)
        tap(R.string.scan_perm_allow)
        tap(R.string.scan_choose_gallery)
        assertEquals(1, count("allow"))
        assertEquals(1, count("gallery"))
        absent(R.string.scan_perm_open_settings, R.string.scan_camera_retry)
    }

    @Test fun permanentlyDenied() {
        show(GateScreen.PermanentlyDenied)
        shown(R.string.scan_perm_blocked_title)
        shown(R.string.scan_perm_blocked_body)
        tap(R.string.scan_perm_open_settings)
        tap(R.string.scan_choose_gallery)
        assertEquals(1, count("settings"))
        assertEquals(1, count("gallery"))
        absent(R.string.scan_perm_allow, R.string.scan_camera_retry)
    }

    @Test fun noCamera() {
        show(GateScreen.NoCamera)
        shown(R.string.scan_no_camera_title)
        shown(R.string.scan_no_camera_body)
        tap(R.string.scan_choose_gallery)
        assertEquals(1, count("gallery"))
        absent(R.string.scan_perm_allow, R.string.scan_perm_open_settings, R.string.scan_camera_retry)
    }

    @Test fun cameraError() {
        show(GateScreen.CameraError)
        shown(R.string.scan_no_camera_title)
        shown(R.string.scan_camera_error_body)
        tap(R.string.scan_camera_retry)
        tap(R.string.scan_choose_gallery)
        assertEquals(1, count("retry"))
        assertEquals(1, count("gallery"))
        absent(R.string.scan_perm_allow, R.string.scan_perm_open_settings)
    }

    @Test fun requestingHasOnlyBack() {
        show(GateScreen.Requesting)
        absent(
            R.string.scan_perm_allow,
            R.string.scan_perm_open_settings,
            R.string.scan_choose_gallery,
            R.string.scan_camera_retry,
        )
        compose.onNodeWithContentDescription(s(R.string.common_back)).performClick()
        assertEquals(1, count("back"))
    }

    @Test fun backAndHeadingInEveryState() {
        show(GateScreen.Requesting)
        val titles = mapOf(
            GateScreen.Denied to R.string.scan_perm_title,
            GateScreen.PermanentlyDenied to R.string.scan_perm_blocked_title,
            GateScreen.NoCamera to R.string.scan_no_camera_title,
            GateScreen.CameraError to R.string.scan_no_camera_title,
        )
        titles.entries.forEachIndexed { i, (state, title) ->
            screen = state
            compose.onNode(hasText(s(title)) and isHeading()).assertExists()
            compose.onNodeWithContentDescription(s(R.string.common_back)).performClick()
            assertEquals(i + 1, count("back"))
        }
    }
}
