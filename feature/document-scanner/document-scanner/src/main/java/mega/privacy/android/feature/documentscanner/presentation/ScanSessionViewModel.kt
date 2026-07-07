package mega.privacy.android.feature.documentscanner.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.feature.documentscanner.domain.boundary.DocumentBoundaryDetector
import mega.privacy.android.feature.documentscanner.domain.boundary.StabilityTracker
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelProvider
import mega.privacy.android.feature.documentscanner.domain.smoother.BoundarySmoother
import mega.privacy.android.feature.documentscanner.presentation.model.BoundaryOverlayState
import mega.privacy.android.feature.documentscanner.presentation.model.ScanSessionUiState
import javax.inject.Inject

/**
 * ViewModel for the continuous scan session screen.
 *
 * Owns the per-frame analysis loop: for each throttled analysis frame the screen
 * delivers, it runs boundary detection, smooths the quad, tracks stability, and
 * publishes the result to [uiState] for the overlay to draw.
 *
 * **Threading.** [onAnalysisFrame] runs synchronously on the caller's thread —
 * in production that is CameraX's single `ImageAnalysis` executor, never the
 * main thread. [DocumentBoundaryDetector.detect], [BoundarySmoother] and
 * [StabilityTracker] are all driven from that one thread, matching the
 * detector's single-thread contract; [uiState] is a thread-safe [MutableStateFlow].
 */
@HiltViewModel
internal class ScanSessionViewModel @Inject constructor(
    private val boundaryDetector: DocumentBoundaryDetector,
    private val boundarySmoother: BoundarySmoother,
    private val stabilityTracker: StabilityTracker,
    private val scannerModelProvider: ScannerModelProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanSessionUiState())
    val uiState: StateFlow<ScanSessionUiState> = _uiState.asStateFlow()

    /** Called when camera permission is granted. */
    fun onCameraPermissionGranted() {
        _uiState.update { it.copy(isCameraPermissionGranted = true) }
    }

    /** Called when camera permission is denied. */
    fun onCameraPermissionDenied() {
        _uiState.update { it.copy(isCameraPermissionGranted = false) }
    }

    /**
     * Analyse a single grayscale frame from the CameraX analyzer.
     *
     * Runs detect → smooth → stability and publishes the outcome to [uiState].
     * When no document is found, the smoother is reset so the next detection
     * snaps to its real position instead of drifting in from a stale one.
     *
     * If the scanner model is not on disk, detection is skipped and
     * [ScanSessionUiState.isModelMissing] is raised so the screen can route back
     * / show an error rather than crash — the prepare screen normally guarantees
     * the model is cached before this screen is shown.
     */
    fun onAnalysisFrame(
        grayBytes: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        timestamp: Long,
    ) {
        if (scannerModelProvider.cachedModelFile() == null) {
            _uiState.update { it.copy(isModelMissing = true) }
            return
        }

        val detection = boundaryDetector.detect(grayBytes, width, height, rotationDegrees, timestamp)
        if (detection == null) {
            boundarySmoother.reset()
            val stability = stabilityTracker.onDetectionResult(null)
            _uiState.update {
                it.copy(
                    boundaryOverlayState = BoundaryOverlayState(),
                    stabilityState = stability,
                )
            }
            return
        }

        val smoothed = detection.copy(boundary = boundarySmoother.smooth(detection.boundary))
        val stability = stabilityTracker.onDetectionResult(smoothed)
        _uiState.update {
            it.copy(
                boundaryOverlayState = BoundaryOverlayState(
                    boundary = smoothed.boundary,
                    frameWidth = smoothed.frameWidth,
                    frameHeight = smoothed.frameHeight,
                ),
                stabilityState = stability,
            )
        }
    }

    override fun onCleared() {
        boundaryDetector.release()
    }
}
