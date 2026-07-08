package mega.privacy.android.feature.documentscanner.presentation.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.feature.documentscanner.domain.entity.CaptureMode
import mega.privacy.android.feature.documentscanner.domain.entity.StabilityState

/**
 * UI state for the continuous scan session screen.
 *
 * @property isCameraPermissionGranted Whether camera permission has been granted.
 * @property boundaryOverlayState The latest detected document quad to draw over
 *   the preview (empty when nothing is detected).
 * @property stabilityState How stable the detected boundary is across frames —
 *   drives overlay colour and, later, the auto-capture trigger (U7).
 * @property isModelMissing True when the scanner model file is not on disk, so
 *   detection cannot run. The screen routes back / surfaces an error instead of
 *   calling the detector. Normally the prepare screen guarantees the model is
 *   cached before this screen is shown.
 * @property captureMode Whether capture fires automatically on a stable document
 *   ([CaptureMode.AUTO]) or only on the manual shutter ([CaptureMode.MANUAL]).
 *   Toggled from the scanner top bar.
 * @property captureEvent One-shot signal telling the screen to capture the current
 *   frame — fired on a stable document (AUTO, off cooldown) or a manual shutter tap.
 *   The screen grabs the frame, then calls back to consume it.
 * @property capturedPageThumbnails Thumbnail URIs of the most recent persisted pages
 *   (oldest first), for the bottom deck. Bounded to a small recent window.
 * @property capturedPageCount Total number of pages captured in this session so far.
 */
internal data class ScanSessionUiState(
    val isCameraPermissionGranted: Boolean = false,
    val boundaryOverlayState: BoundaryOverlayState = BoundaryOverlayState(),
    val stabilityState: StabilityState = StabilityState.SEARCHING,
    val isModelMissing: Boolean = false,
    val captureMode: CaptureMode = CaptureMode.AUTO,
    val captureEvent: StateEvent = consumed,
    val capturedPageThumbnails: List<String> = emptyList(),
    val capturedPageCount: Int = 0,
)
