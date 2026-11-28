package app.palm.feature.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Asks for CAMERA on entering Scan, not before (D-04). Denied vs blocked comes only from the answer to a real
 * request plus the rationale flag, so "Open settings" never shows before Android has answered. The permission
 * is re-checked on every resume (Settings round trip); camera problems become NoCamera / CameraError.
 */
@Composable
fun CameraPermissionGate(
    onGallery: () -> Unit,
    onBack: () -> Unit,
    content: @Composable (onCameraProblem: (CameraProblem) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    fun checkGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    // Not saved: after process death (a Settings revoke kills the process) it must come from the system, not the old answer
    var granted by remember { mutableStateOf(checkGranted()) }
    var asked by rememberSaveable { mutableStateOf(false) } // a request is in flight or answered: no second launch on recreation
    var requested by rememberSaveable { mutableStateOf(false) }
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var problem by rememberSaveable { mutableStateOf<CameraProblem?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        requested = true
        showRationale = activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }
    LaunchedEffect(granted) { // keyed on granted: a permission lost on resume before any request asks again
        if (!granted && !asked) {
            asked = true
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = checkGranted() }

    when (val screen = gateScreen(cameraPerm(granted, requested, showRationale), problem)) {
        GateScreen.Scanner -> content { problem = it }
        else -> ScanFallbackScreen(
            screen,
            onAllow = { launcher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                // This package only (T-04-12)
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
            onGallery = onGallery,
            onRetry = { problem = null }, // the scanner re-enters composition and rebinds the camera
            onBack = onBack,
        )
    }
}
