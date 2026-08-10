package mega.privacy.android.feature.documentscanner.domain.boundary

import mega.privacy.android.feature.documentscanner.domain.entity.DetectionResult
import mega.privacy.android.feature.documentscanner.domain.entity.StabilityState

/**
 * Tracks the stability of detected document boundaries across consecutive frames.
 *
 * Call [onDetectionResult] for every analysed frame. The tracker compares consecutive
 * boundaries and determines whether the document is being held still enough for capture.
 */
interface StabilityTracker {

    /**
     * Feed a new detection result (or null if no document was found) and get the
     * updated stability state.
     */
    fun onDetectionResult(result: DetectionResult?): StabilityState

    /**
     * Forget all per-frame state. The next [onDetectionResult] call reports
     * SEARCHING / UNSTABLE as if no frames had been seen.
     */
    fun reset()
}
