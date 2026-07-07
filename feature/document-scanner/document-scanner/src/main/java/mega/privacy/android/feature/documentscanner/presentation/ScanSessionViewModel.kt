package mega.privacy.android.feature.documentscanner.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.feature.documentscanner.domain.boundary.DocumentBoundaryDetector
import mega.privacy.android.feature.documentscanner.domain.boundary.StabilityTracker
import mega.privacy.android.feature.documentscanner.domain.entity.CaptureMode
import mega.privacy.android.feature.documentscanner.domain.entity.StabilityState
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

    private var lastFrameTimestampMs: Long = 0L
    private var lastCaptureTimestampMs: Long? = null

    /** Called when camera permission is granted. */
    fun onCameraPermissionGranted() {
        _uiState.update { it.copy(isCameraPermissionGranted = true) }
    }

    /** Called when camera permission is denied. */
    fun onCameraPermissionDenied() {
        _uiState.update { it.copy(isCameraPermissionGranted = false) }
    }

    /** Toggle auto-capture on/off from the top bar (AUTO ↔ MANUAL). */
    fun onToggleAutoCapture() {
        _uiState.update {
            val next = if (it.captureMode == CaptureMode.AUTO) CaptureMode.MANUAL else CaptureMode.AUTO
            it.copy(captureMode = next)
        }
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
        lastFrameTimestampMs = timestamp
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
        val autoCapture = _uiState.value.captureMode == CaptureMode.AUTO &&
            stability == StabilityState.STABLE &&
            cooldownElapsed(timestamp)
        if (autoCapture) lastCaptureTimestampMs = timestamp
        _uiState.update {
            it.copy(
                boundaryOverlayState = BoundaryOverlayState(
                    boundary = smoothed.boundary,
                    frameWidth = smoothed.frameWidth,
                    frameHeight = smoothed.frameHeight,
                ),
                stabilityState = stability,
                captureEvent = if (autoCapture) triggered else it.captureEvent,
            )
        }
    }

    /**
     * Manual shutter tap — capture immediately, regardless of stability or mode.
     * Resets the auto-capture cooldown so the two paths don't double-fire.
     */
    fun onManualCapture() {
        lastCaptureTimestampMs = lastFrameTimestampMs
        _uiState.update { it.copy(captureEvent = triggered) }
    }

    /** The screen has grabbed the frame for the pending capture. */
    fun onCaptureHandled() {
        _uiState.update { it.copy(captureEvent = consumed) }
    }

    private fun cooldownElapsed(now: Long): Boolean =
        lastCaptureTimestampMs?.let { now - it >= AUTO_CAPTURE_COOLDOWN_MS } ?: true

    override fun onCleared() {
        boundaryDetector.release()
    }

    private companion object {
        const val AUTO_CAPTURE_COOLDOWN_MS = 3_000L
    }
}
