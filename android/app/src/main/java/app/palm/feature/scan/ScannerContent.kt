package app.palm.feature.scan

import android.content.pm.ActivityInfo
import android.util.Size
import androidx.activity.compose.LocalActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import app.palm.R
import app.palm.contract.HandSide
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneChip
import app.palm.core.camera.CameraXScanCamera
import app.palm.core.photo.PhotoSource
import app.palm.core.vision.GuidanceState
import app.palm.core.vision.LiveHandTracker
import app.palm.feature.handselect.handLabel
import java.util.concurrent.Executors

enum class CameraProblem { NoBackCamera, OpenFailed }

private val TopGradientHeight = 128.dp

private val OpenFailedCodes = setOf(
    CameraState.ERROR_CAMERA_IN_USE,
    CameraState.ERROR_MAX_CAMERAS_IN_USE,
    CameraState.ERROR_CAMERA_DISABLED,
    CameraState.ERROR_CAMERA_FATAL_ERROR,
    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED,
)

private fun selector(width: Int, height: Int, rule: Int): ResolutionSelector = ResolutionSelector.Builder()
    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
    .setResolutionStrategy(ResolutionStrategy(Size(width, height), rule))
    .build()

/**
 * Problem to show once CameraX init has settled, or null when the back camera is ready.
 * A failed init leaves the provider null, so hasCamera would throw on the main thread.
 */
internal fun cameraInitProblem(awaitInit: () -> Unit, hasBackCamera: () -> Boolean): CameraProblem? = when {
    runCatching(awaitInit).isFailure -> CameraProblem.OpenFailed
    else -> when (runCatching(hasBackCamera).getOrNull()) {
        true -> null
        false -> CameraProblem.NoBackCamera
        null -> CameraProblem.OpenFailed
    }
}

/** S08 live scanner: back-camera preview, hand chip, guidance chip and shutter (D-02, D-03, D-05, D-13). */
@Composable
fun ScannerContent(
    hand: HandSide,
    busy: Boolean,
    onCaptured: (PhotoSource.Capture) -> Unit,
    onGallery: () -> Unit,
    onCameraProblem: (CameraProblem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val problem by rememberUpdatedState(onCameraProblem)
    val captured by rememberUpdatedState(onCaptured)

    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS or CameraController.IMAGE_CAPTURE)
            isTapToFocusEnabled = false // our overlay handles taps and draws the ring
            setAutoRotationEnabled(false) // rotation follows the display, not the motion sensor (Pitfall 2)
            previewResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
            // then-higher only applies where closest-lower alone would throw at bind (D-03)
            imageAnalysisResolutionSelector = selector(640, 480, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
            imageCaptureResolutionSelector = selector(1920, 1440, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
            imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
    }
    val tracker = remember {
        LiveHandTracker(
            context,
            onFrame = { frame, timestampMs -> viewModel.onFrame(frame, timestampMs) },
            onError = { viewModel.onGuideError() },
        )
    }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            keepScreenOn = true // only while the scanner is composed: a slow scan never sleeps the phone
        }
    }
    val camera = remember { CameraXScanCamera(controller, previewView, captureExecutor) }

    // Portrait lock on Scan only (UI-SPEC Layout); restored on leave
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val saved = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { if (saved != null) activity.requestedOrientation = saved }
    }

    DisposableEffect(controller, tracker) {
        tracker.start()
        val main = ContextCompat.getMainExecutor(context)
        var disposed = false
        controller.initializationFuture.addListener({
            if (disposed) return@addListener
            val initProblem = cameraInitProblem(
                awaitInit = { controller.initializationFuture.get() },
                hasBackCamera = { controller.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) },
            )
            if (initProblem != null) {
                problem(initProblem)
                return@addListener
            }
            previewView.controller = controller
            if (runCatching { controller.bindToLifecycle(lifecycleOwner) }.isFailure) {
                problem(CameraProblem.OpenFailed)
                return@addListener
            }
            controller.setImageAnalysisAnalyzer(tracker.executor, tracker)
            viewModel.onCameraReady(camera)
            controller.cameraInfo?.cameraState?.observe(lifecycleOwner) { state ->
                if (state.error?.code in OpenFailedCodes) problem(CameraProblem.OpenFailed)
            }
        }, main)
        onDispose {
            disposed = true
            viewModel.torchOff(camera)
            controller.clearImageAnalysisAnalyzer()
            tracker.close()
            controller.unbind()
            captureExecutor.shutdown()
        }
    }

    val lifecycle by lifecycleOwner.lifecycle.currentStateAsState()
    val analysing = lifecycle.isAtLeast(Lifecycle.State.RESUMED) && !busy
    LaunchedEffect(analysing) {
        tracker.active = analysing
        if (!analysing) viewModel.torchOff(camera) // paused or processing: the torch never stays on (T-04-10)
    }

    LaunchedEffect(hand) { viewModel.setHand(hand) }

    val previewCd = stringResource(R.string.scan_preview_cd)
    val paneTitle = stringResource(R.string.scan_title)
    // layout positions in root px, read only by the overlay's draw cache
    var rootTop by remember { mutableFloatStateOf(0f) }
    var chipBottom by remember { mutableFloatStateOf(0f) }
    var barTop by remember { mutableFloatStateOf(0f) }
    val gap = with(LocalDensity.current) { NocturneSpace.S16.toPx() }
    Box(
        modifier
            .fillMaxSize()
            .background(NocturneColors.BgCamera)
            .onGloballyPositioned { rootTop = it.positionInRoot().y }
            .semantics {
                this.paneTitle = paneTitle
                isTraversalGroup = true // TalkBack: back, hand chip, guidance chip, preview, gallery, shutter, torch
            },
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewModel.onViewSize(it.width, it.height) },
        )
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars.add(WindowInsets(top = TopGradientHeight)))
                .background(Brush.verticalGradient(listOf(NocturneColors.SurfaceScrim.copy(alpha = 0.6f), Color.Transparent))),
        )
        PalmGuideOverlay(
            hand = hand,
            detected = ui.shown == GuidanceState.PalmDetected,
            busy = busy,
            focusPoint = ui.focusPoint,
            freeTop = { chipBottom - rootTop + gap },
            freeBottom = { barTop - rootTop - gap },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(camera) { detectTapGestures { viewModel.focusAt(camera, it.x, it.y) } }
                // the preview's semantics live here: the AndroidView holder is invisible to accessibility
                .semantics {
                    contentDescription = previewCd
                    traversalIndex = 1f
                    onClick { viewModel.focusAt(camera, previewView.width / 2f, previewView.height / 2f); true }
                },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, top = NocturneSpace.S8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().height(ControlSize)) {
                CameraIconButton(
                    R.drawable.ic_back,
                    stringResource(R.string.common_back),
                    onBack,
                    Modifier.align(Alignment.CenterStart),
                    floating = true,
                )
                NocturneChip(stringResource(handLabel(hand)), Modifier.align(Alignment.Center), textColor = NocturneColors.TextPrimary)
            }
            Spacer(Modifier.height(NocturneSpace.S8))
            GuidanceChip(ui, Modifier.onGloballyPositioned { chipBottom = it.boundsInRoot().bottom })
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onGloballyPositioned { barTop = it.boundsInRoot().top }
                .semantics { traversalIndex = 2f }
                .background(
                    NocturneColors.SurfaceScrim.copy(alpha = 0.6f),
                    RoundedCornerShape(topStart = NocturneRadius.Xl, topEnd = NocturneRadius.Xl),
                )
                .padding(top = NocturneSpace.S16, start = NocturneSpace.S32, end = NocturneSpace.S32)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = NocturneSpace.S24),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val idle = !busy && !ui.capturing
            CameraIconButton(R.drawable.ic_gallery, stringResource(R.string.scan_choose_gallery), onGallery, enabled = idle)
            ShutterButton(
                enabled = ui.captureEnabled && !busy,
                busy = ui.capturing,
                onClick = { viewModel.capture(camera) { captured(it) } },
            )
            if (ui.hasFlash) {
                TorchButton(on = ui.torchOn, enabled = idle, onToggle = { viewModel.toggleTorch(camera) })
            } else {
                Spacer(Modifier.size(ControlSize)) // keeps the shutter centred
            }
        }
    }
}
