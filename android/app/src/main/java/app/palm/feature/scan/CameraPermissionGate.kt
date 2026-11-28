package app.palm.feature.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import app.palm.R
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneStateBlock
import app.palm.core.designsystem.components.NocturneTopBar

private enum class Access { Checking, Granted, Refused }

/** Asks for CAMERA on entering Scan, not before (D-04); every refusal or camera problem offers the gallery. */
@Composable
fun CameraPermissionGate(
    onGallery: () -> Unit,
    onBack: () -> Unit,
    content: @Composable (onCameraProblem: (CameraProblem) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var access by rememberSaveable { mutableStateOf(if (granted()) Access.Granted else Access.Checking) }
    var problem by rememberSaveable { mutableStateOf<CameraProblem?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        access = if (ok) Access.Granted else Access.Refused
    }
    LaunchedEffect(Unit) {
        if (access == Access.Checking) launcher.launch(Manifest.permission.CAMERA)
    }

    val current = problem
    when {
        current != null -> StateScreen(onBack) {
            NocturneStateBlock(
                painterResource(R.drawable.ic_camera_off),
                NocturneColors.StatusError,
                stringResource(R.string.scan_no_camera_title),
                stringResource(R.string.scan_no_camera_body),
                primary = stringResource(R.string.scan_choose_gallery) to onGallery,
            )
        }
        access == Access.Granted -> content { problem = it }
        access == Access.Refused -> StateScreen(onBack) {
            NocturneStateBlock(
                painterResource(R.drawable.ic_camera_off),
                NocturneColors.StatusWarning,
                stringResource(R.string.scan_perm_title),
                stringResource(R.string.scan_perm_body),
                primary = stringResource(R.string.scan_perm_allow) to { launcher.launch(Manifest.permission.CAMERA) },
                secondary = stringResource(R.string.scan_choose_gallery) to onGallery,
            )
        }
        // While the system dialog is up: bg.camera and the back button only
        else -> Box(Modifier.fillMaxSize().background(NocturneColors.BgCamera)) {
            CameraIconButton(
                R.drawable.ic_back,
                stringResource(R.string.common_back),
                onBack,
                Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = NocturneSpace.S16, top = NocturneSpace.S8),
            )
        }
    }
}

@Composable
private fun StateScreen(onBack: () -> Unit, block: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(NocturneColors.BgCamera)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = NocturneSpace.S16),
    ) {
        NocturneTopBar(stringResource(R.string.scan_title), onBack)
        // Centred when it fits, scrolls at large font scale
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = NocturneSpace.S24)) {
                block()
            }
        }
    }
}
