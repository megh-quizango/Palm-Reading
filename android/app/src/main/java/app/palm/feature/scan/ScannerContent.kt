package app.palm.feature.scan

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneGradientAngles
import app.palm.core.designsystem.NocturneGradients
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneChip
import app.palm.core.designsystem.nocturneBrush
import app.palm.core.photo.PhotoSource
import app.palm.core.vision.GuidanceState
import app.palm.core.vision.LiveHandTracker
import app.palm.feature.handselect.handLabel
import java.util.concurrent.Executors

enum class CameraProblem { NoBackCamera, OpenFailed }

private val ControlSize = 52.dp
private val ShutterSize = 76.dp
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
            controller.cameraInfo?.cameraState?.observe(lifecycleOwner) { state ->
                if (state.error?.code in OpenFailedCodes) problem(CameraProblem.OpenFailed)
            }
        }, main)
        onDispose {
            disposed = true
            controller.clearImageAnalysisAnalyzer()
            tracker.close()
            controller.unbind()
            captureExecutor.shutdown()
        }
    }

    val lifecycle by lifecycleOwner.lifecycle.currentStateAsState()
    val analysing = lifecycle.isAtLeast(Lifecycle.State.RESUMED) && !busy
    LaunchedEffect(analysing) { tracker.active = analysing }

    val takePhoto = {
        viewModel.onCaptureStarted()
        val main = ContextCompat.getMainExecutor(context)
        controller.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val source = try {
                        val rotation = image.imageInfo.rotationDegrees
                        val buffer = image.planes[0].buffer
                        PhotoSource.Capture(ByteArray(buffer.remaining()).also(buffer::get), rotation)
                    } catch (_: Exception) {
                        null
                    } finally {
                        image.close()
                    }
                    main.execute {
                        if (source == null) {
                            viewModel.onCaptureFailed()
                        } else {
                            viewModel.onCaptureDone()
                            captured(source)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    main.execute { viewModel.onCaptureFailed() }
                }
            },
        )
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
            .semantics { this.paneTitle = paneTitle },
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewModel.onViewSize(it.width, it.height) }
                .semantics { contentDescription = previewCd },
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
            modifier = Modifier.fillMaxSize(),
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
            CameraIconButton(R.drawable.ic_gallery, stringResource(R.string.scan_choose_gallery), onGallery, enabled = !busy)
            Shutter(enabled = ui.captureEnabled && !busy, onClick = takePhoto)
            Spacer(Modifier.size(ControlSize))
        }
    }
}

@Composable
internal fun CameraIconButton(icon: Int, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier
            .size(ControlSize)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(CircleShape)
            .background(NocturneColors.SurfaceScrim.copy(alpha = 0.72f), CircleShape)
            .border(NocturneBorderWidth, NocturneColors.BorderHairline.copy(alpha = 0.35f), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = NocturneColors.TextPrimary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun Shutter(enabled: Boolean, onClick: () -> Unit) {
    val brush = remember { nocturneBrush(NocturneGradients.Primary, NocturneGradientAngles.Primary) }
    val description = stringResource(R.string.scan_capture_cd)
    Box(
        Modifier
            .size(ShutterSize)
            .alpha(if (enabled) 1f else 0.6f)
            .border(4.dp, if (enabled) NocturneColors.StatusSuccess else NocturneColors.TextPrimary.copy(alpha = 0.4f), CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val disc = Modifier.size(60.dp)
        Box(
            if (enabled) disc.background(brush, CircleShape) else disc.background(NocturneColors.TextPrimary.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_camera), contentDescription = null, tint = NocturneColors.TextOnPrimary, modifier = Modifier.size(24.dp))
        }
    }
}
