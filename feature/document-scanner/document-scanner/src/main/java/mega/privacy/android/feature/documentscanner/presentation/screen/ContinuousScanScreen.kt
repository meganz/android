package mega.privacy.android.feature.documentscanner.presentation.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.documentscanner.components.BoundaryOverlay
import mega.privacy.android.feature.documentscanner.components.ScanBoundaryStability
import mega.privacy.android.feature.documentscanner.components.ScannerControlBar
import mega.privacy.android.feature.documentscanner.components.ScannerTopBar
import mega.privacy.android.feature.documentscanner.domain.entity.CaptureMode
import mega.privacy.android.feature.documentscanner.domain.entity.StabilityState
import mega.privacy.android.feature.documentscanner.presentation.ScanSessionViewModel
import mega.privacy.android.feature.documentscanner.presentation.analyzer.ScanFrameAnalyzer
import mega.privacy.android.feature.documentscanner.presentation.model.BoundaryOverlayState
import java.util.concurrent.Executors
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber

/** Minimum time between analysed frames (≈5 Hz) — see [ScanFrameAnalyzer]. */
private const val ANALYSIS_INTERVAL_MS = 200L

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
            onToggleAutoCapture = viewModel::onToggleAutoCapture,
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
    onToggleAutoCapture: () -> Unit,
    onClose: () -> Unit,
    onSwitchToLegacy: () -> Unit,
    onFrame: (ByteArray, Int, Int, Int, Long) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
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

        ScannerControlBar(
            onManualShutter = {},
            onSwitchToLegacy = onSwitchToLegacy,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .navigationBarsPadding(),
        )
    }
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
