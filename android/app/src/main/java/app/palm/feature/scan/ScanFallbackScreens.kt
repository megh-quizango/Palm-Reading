package app.palm.feature.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.palm.R
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneStateBlock
import app.palm.core.designsystem.components.NocturneTopBar

/** Permission and camera fallback screens (UI-SPEC "Permission and fallback screens"); the gate renders Scanner itself. */
@Composable
fun ScanFallbackScreen(
    screen: GateScreen,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onGallery: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(NocturneColors.BgCamera)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        NocturneTopBar(stringResource(R.string.scan_title), onBack, Modifier.padding(horizontal = NocturneSpace.S16))
        if (screen == GateScreen.Requesting || screen == GateScreen.Scanner) return@Column // system dialog is up
        val gallery = stringResource(R.string.scan_choose_gallery) to onGallery
        // Centred when it fits, scrolls at large font scale
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, top = NocturneSpace.S24, bottom = NocturneSpace.S24),
            ) {
                when (screen) {
                    GateScreen.Denied -> NocturneStateBlock(
                        painterResource(R.drawable.ic_camera_off),
                        NocturneColors.StatusWarning,
                        stringResource(R.string.scan_perm_title),
                        stringResource(R.string.scan_perm_body),
                        primary = stringResource(R.string.scan_perm_allow) to onAllow,
                        secondary = gallery,
                    )
                    GateScreen.PermanentlyDenied -> NocturneStateBlock(
                        painterResource(R.drawable.ic_camera_off),
                        NocturneColors.StatusWarning,
                        stringResource(R.string.scan_perm_blocked_title),
                        stringResource(R.string.scan_perm_blocked_body),
                        primary = stringResource(R.string.scan_perm_open_settings) to onOpenSettings,
                        secondary = gallery,
                    )
                    GateScreen.NoCamera -> NocturneStateBlock(
                        painterResource(R.drawable.ic_camera_off),
                        NocturneColors.StatusError,
                        stringResource(R.string.scan_no_camera_title),
                        stringResource(R.string.scan_no_camera_body),
                        primary = gallery,
                    )
                    GateScreen.CameraError -> NocturneStateBlock(
                        painterResource(R.drawable.ic_alert),
                        NocturneColors.StatusError,
                        stringResource(R.string.scan_no_camera_title),
                        stringResource(R.string.scan_camera_error_body),
                        primary = gallery,
                        secondary = stringResource(R.string.scan_camera_retry) to onRetry,
                    )
                    GateScreen.Requesting, GateScreen.Scanner -> Unit
                }
            }
        }
    }
}
