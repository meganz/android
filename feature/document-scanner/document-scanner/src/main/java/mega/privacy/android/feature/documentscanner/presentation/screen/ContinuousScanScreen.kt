package mega.privacy.android.feature.documentscanner.presentation.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEvent
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.feature.documentscanner.components.BoundaryOverlay
import mega.privacy.android.feature.documentscanner.components.CapturedPagesDeck
import mega.privacy.android.feature.documentscanner.components.ScanBoundaryStability
import mega.privacy.android.feature.documentscanner.components.ScannerControlBar
import mega.privacy.android.feature.documentscanner.components.ScannerTopBar
import mega.privacy.android.feature.documentscanner.domain.entity.CaptureMode
import mega.privacy.android.feature.documentscanner.domain.entity.StabilityState
import mega.privacy.android.feature.documentscanner.presentation.ScanSessionViewModel
import mega.privacy.android.feature.documentscanner.presentation.analyzer.ScanFrameAnalyzer
import mega.privacy.android.feature.documentscanner.presentation.model.BoundaryOverlayState
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber

/** Minimum time between analysed frames (≈5 Hz) — see [ScanFrameAnalyzer]. */
private const val ANALYSIS_INTERVAL_MS = 200L

// The deck fans at most this many recent thumbnails; older ones are dropped once
// they scroll out of the fan so memory does not grow with the page count.
private const val DECK_THUMBNAIL_WINDOW = 4

/**
 * Main scanning screen with CameraX preview, permission handling, and close button.
 *
 * @param onClose Callback when the user closes the scanner
 * @param onSwitchToLegacy Callback to switch to the legacy ML Kit scanner
 * @param viewModel ViewModel managing camera and session state
 */
@Composable
internal fun ContinuousScanScreen(
    onClose: () -> Unit,
    onSwitchToLegacy: () -> Unit,
    viewModel: ScanSessionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.onCameraPermissionGranted()
        } else {
            viewModel.onCameraPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.onCameraPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // The prepare screen normally guarantees the model is cached before we get
    // here; this is a safety net if detection finds it missing (e.g. deep link).
    LaunchedEffect(uiState.isModelMissing) {
        if (uiState.isModelMissing) onClose()
    }

    if (uiState.isCameraPermissionGranted) {
        CameraContent(
            boundaryOverlayState = uiState.boundaryOverlayState,
            stabilityState = uiState.stabilityState,
            captureMode = uiState.captureMode,
            captureEvent = uiState.captureEvent,
            onToggleAutoCapture = viewModel::onToggleAutoCapture,
            onManualCapture = viewModel::onManualCapture,
            onCaptureHandled = viewModel::onCaptureHandled,
            onClose = onClose,
            onSwitchToLegacy = onSwitchToLegacy,
            onFrame = viewModel::onAnalysisFrame,
        )
    } else {
        PermissionDeniedContent(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onClose = onClose,
        )
    }
}

@Composable
private fun CameraContent(
    boundaryOverlayState: BoundaryOverlayState,
    stabilityState: StabilityState,
    captureMode: CaptureMode,
    captureEvent: StateEvent,
    onToggleAutoCapture: () -> Unit,
    onManualCapture: () -> Unit,
    onCaptureHandled: () -> Unit,
    onClose: () -> Unit,
    onSwitchToLegacy: () -> Unit,
    onFrame: (ByteArray, Int, Int, Int, Long) -> Unit,
) {
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    // Decode/rotate/scale of the captured JPEG runs on this executor, off the main thread.
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { captureExecutor.shutdown() }
    }
    // The deck only fans the most recent thumbnails, so we retain a bounded window
    // and a running count rather than every captured page (full page storage is U8).
    val deckThumbnails = remember { mutableStateListOf<ImageBitmap>() }
    var pageCount by remember { mutableIntStateOf(0) }
    var flashKey by remember { mutableIntStateOf(0) }
    var flying by remember { mutableStateOf<ImageBitmap?>(null) }

    // The VM decides WHEN to capture (auto-on-stable or manual); here we grab the
    // frame, flash, and fly it into the deck. Persistence/dedup is U8.
    EventEffect(event = captureEvent, onConsumed = onCaptureHandled) {
        val thumbnail = captureThumbnail(imageCapture, captureExecutor)
        if (thumbnail != null) {
            flashKey++
            flying = thumbnail
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            imageCapture = imageCapture,
            onFrame = onFrame,
            modifier = Modifier.fillMaxSize(),
        )

        BoundaryOverlay(
            normalisedCorners = boundaryOverlayState.boundary?.let {
                listOf(
                    Offset(it.topLeft.x, it.topLeft.y),
                    Offset(it.topRight.x, it.topRight.y),
                    Offset(it.bottomRight.x, it.bottomRight.y),
                    Offset(it.bottomLeft.x, it.bottomLeft.y),
                )
            },
            frameWidth = boundaryOverlayState.frameWidth,
            frameHeight = boundaryOverlayState.frameHeight,
            stability = stabilityState.toScanBoundaryStability(),
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)),
                ),
        )

        ScannerTopBar(
            isAutoOn = captureMode == CaptureMode.AUTO,
            onToggleAutoCapture = onToggleAutoCapture,
            onClose = onClose,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
        )

        CapturedPagesDeck(
            pages = deckThumbnails,
            count = pageCount,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 40.dp),
        )

        ScannerControlBar(
            onManualShutter = onManualCapture,
            onSwitchToLegacy = onSwitchToLegacy,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .navigationBarsPadding(),
        )

        flying?.let { bitmap ->
            FlyingThumbnail(bitmap = bitmap, onLanded = {
                deckThumbnails.add(bitmap)
                pageCount++
                // Drop the reference only; the compositor may still be drawing this
                // thumbnail, so let GC reclaim it rather than recycling it here.
                while (deckThumbnails.size > DECK_THUMBNAIL_WINDOW) {
                    deckThumbnails.removeAt(0)
                }
                flying = null
            })
        }

        CaptureFlash(triggerKey = flashKey)
    }
}

/** Brief white flash over the whole screen when [triggerKey] increments. */
@Composable
private fun CaptureFlash(triggerKey: Int) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        if (triggerKey == 0) return@LaunchedEffect
        alpha.snapTo(0.8f)
        alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 350))
    }
    if (alpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = alpha.value)),
        )
    }
}

/** Animates the just-captured thumbnail from screen centre down to the deck. */
@Composable
private fun FlyingThumbnail(bitmap: ImageBitmap, onLanded: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(bitmap) {
        progress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 420))
        onLanded()
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val targetX = with(density) { -(maxWidth.toPx() / 2f - 60.dp.toPx()) }
        val targetY = with(density) { maxHeight.toPx() / 2f - 100.dp.toPx() }
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.Center)
                .size(140.dp)
                .graphicsLayer {
                    val p = progress.value
                    translationX = targetX * p
                    translationY = targetY * p
                    val scale = 1f - 0.55f * p
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - 0.2f * p
                }
                .clip(RoundedCornerShape(8.dp)),
        )
    }
}

private suspend fun captureThumbnail(imageCapture: ImageCapture, executor: Executor): ImageBitmap? =
    suspendCancellableCoroutine { continuation ->
        imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val result = runCatching {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        val rotated = rotateBitmap(decoded, image.imageInfo.rotationDegrees)
                        if (rotated !== decoded) decoded.recycle()
                        val scaled = scaleToThumbnail(rotated)
                        if (scaled !== rotated) rotated.recycle()
                        scaled.asImageBitmap()
                    }.getOrNull()
                    image.close()
                    continuation.resume(result)
                }

                override fun onError(exception: ImageCaptureException) {
                    Timber.e(exception, "[DocScanner] Capture failed")
                    continuation.resume(null)
                }
            },
        )
    }

private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun scaleToThumbnail(bitmap: Bitmap): Bitmap {
    val maxEdge = 320
    val longest = maxOf(bitmap.width, bitmap.height)
    if (longest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / longest
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt(),
        (bitmap.height * scale).toInt(),
        true,
    )
}

private fun StabilityState.toScanBoundaryStability(): ScanBoundaryStability = when (this) {
    StabilityState.SEARCHING -> ScanBoundaryStability.SEARCHING
    StabilityState.UNSTABLE -> ScanBoundaryStability.UNSTABLE
    StabilityState.STABILIZING -> ScanBoundaryStability.STABILIZING
    StabilityState.STABLE -> ScanBoundaryStability.STABLE
}

// core/ui-components was checked for a CameraX PreviewView wrapper composable;
// no equivalent component exists there.
@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    onFrame: (ByteArray, Int, Int, Int, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanFrameAnalyzer = remember { ScanFrameAnalyzer(ANALYSIS_INTERVAL_MS) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            val plane = imageProxy.planes[0]
                            val frame = scanFrameAnalyzer.analyze(
                                width = imageProxy.width,
                                height = imageProxy.height,
                                rowStride = plane.rowStride,
                                pixelStride = plane.pixelStride,
                                rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                                timestampMs = imageProxy.imageInfo.timestamp / 1_000_000,
                            ) {
                                val buffer = plane.buffer
                                ByteArray(buffer.remaining()).also { buffer.get(it) }
                            }
                            frame?.let {
                                onFrame(it.bytes, it.width, it.height, it.rotationDegrees, it.timestampMs)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "[DocScanner] Analysis frame failed")
                        } finally {
                            imageProxy.close()
                        }
                    }
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                    imageCapture,
                )
            } catch (e: Exception) {
                Timber.e(e, "CameraX bind failed")
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            if (cameraProviderFuture.isDone) {
                cameraProviderFuture.get().unbindAll()
            }
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

// core/ui-components was checked for a permission rationale/request UI composable;
// no equivalent MegaPermissionRequest or PermissionRationaleView component exists there.
// TODO: replace with core-ui component once available/confirmed
@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(sharedR.string.camera_denied_info_message))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text(text = stringResource(sharedR.string.grant_permission))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onClose) {
                Text(text = stringResource(sharedR.string.general_dialog_cancel_button))
            }
        }
    }
}
