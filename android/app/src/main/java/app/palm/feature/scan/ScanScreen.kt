package app.palm.feature.scan

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.R
import app.palm.contract.HandSide
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.components.NocturneDialog
import app.palm.core.photo.PhotoOrigin
import app.palm.core.photo.PhotoSource

/** Scan(hand): permission -> live scanner or fallback; captures and gallery picks share one pipeline (D-10, D-11). */
@Composable
fun ScanScreen(hand: HandSide, onBack: () -> Unit, onReady: () -> Unit, viewModel: ScanViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.ready) {
        if (ui.ready) {
            viewModel.consumeReady()
            onReady()
        }
    }
    // The system photo picker needs no storage permission (D-11)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.process(PhotoSource.Gallery(uri), PhotoOrigin.GALLERY, hand)
    }
    val openGallery = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    Box(Modifier.fillMaxSize().background(NocturneColors.BgCamera)) {
        CameraPermissionGate(onGallery = openGallery, onBack = onBack) { onProblem ->
            ScannerContent(
                hand = hand,
                busy = ui.processing,
                onCaptured = { viewModel.process(it, PhotoOrigin.CAMERA, hand) },
                onGallery = openGallery,
                onCameraProblem = onProblem,
                onBack = onBack,
            )
        }
        ProcessingOverlayHost(ui.processing)
    }
    // Composed after the gate and scanner, so it wins over their Back handlers while processing (UI-SPEC Back behaviour)
    BackHandler(enabled = ui.processing) { viewModel.cancel() }
    val rejected = ui.rejected
    if (rejected != null) {
        val (title, body) = when (rejected.reason) {
            RejectReason.NoHand -> R.string.scan_reject_no_hand_title to R.string.scan_reject_no_hand_body
            RejectReason.Unsupported -> R.string.scan_reject_unsupported_title to R.string.scan_reject_unsupported_body
            RejectReason.Failed -> R.string.scan_reject_failed_title to R.string.scan_reject_failed_body
        }
        NocturneDialog(
            title = stringResource(title),
            body = stringResource(body),
            confirm = if (rejected.origin == PhotoOrigin.GALLERY) {
                stringResource(R.string.scan_choose_another) to {
                    viewModel.dismissRejection()
                    openGallery()
                }
            } else {
                stringResource(R.string.scan_retake) to viewModel::dismissRejection
            },
            dismiss = if (rejected.origin == PhotoOrigin.GALLERY) stringResource(R.string.common_close) to viewModel::dismissRejection else null,
            onDismissRequest = viewModel::dismissRejection,
        )
    }
}
