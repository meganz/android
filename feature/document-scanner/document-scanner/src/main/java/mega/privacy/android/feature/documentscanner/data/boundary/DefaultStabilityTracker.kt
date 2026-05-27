package mega.privacy.android.feature.documentscanner.data.boundary

import androidx.annotation.VisibleForTesting
import mega.privacy.android.feature.documentscanner.domain.boundary.StabilityTracker
import mega.privacy.android.feature.documentscanner.domain.entity.DetectionResult
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.StabilityState
import javax.inject.Inject

/**
 * Default [StabilityTracker] that compares corner drift across consecutive frames.
 *
 * Transitions the tracker actually emits:
 * - No detection → [StabilityState.SEARCHING]
 * - First detection after SEARCHING → [StabilityState.UNSTABLE]
 * - Corners drifting > [DRIFT_THRESHOLD] from the previous frame → [StabilityState.UNSTABLE]
 * - Corners within threshold for [STABLE_FRAMES] consecutive frames → [StabilityState.STABLE]
 *
 * Note: with [STABILIZING_FRAMES] == [STABLE_FRAMES], [StabilityState.STABILIZING]
 * is currently unreachable. The enum value is retained for forward
 * compatibility (and so the disabled test can be re-enabled if we later
 * widen the gap).
 */
class DefaultStabilityTracker @Inject constructor() : StabilityTracker {

    private var stableFrameCount = 0
    private var lastBoundary: DocumentBoundary? = null

    @Synchronized
    override fun onDetectionResult(result: DetectionResult?): StabilityState {
        if (result == null) {
            reset()
            return StabilityState.SEARCHING
        }

        val previous = lastBoundary
        lastBoundary = result.boundary

        if (previous == null) {
            stableFrameCount = 1
            return StabilityState.UNSTABLE
        }

        val drift = maxCornerDrift(previous, result.boundary)

        if (drift > DRIFT_THRESHOLD) {
            stableFrameCount = 1
            return StabilityState.UNSTABLE
        }

        stableFrameCount++

        return when {
            stableFrameCount >= STABLE_FRAMES -> StabilityState.STABLE
            stableFrameCount >= STABILIZING_FRAMES -> StabilityState.STABILIZING
            else -> StabilityState.UNSTABLE
        }
    }

    @Synchronized
    override fun reset() {
        stableFrameCount = 0
        lastBoundary = null
    }

    companion object {
        /**
         * Maximum allowed corner movement (in normalised 0–1 frame coords)
         * between two consecutive frames to still count as "still". 0.05 ≈
         * 5% of the frame width — about 50 px on a 1080p preview, forgiving
         * enough that natural hand tremor doesn't reset the stability streak.
         */
        @VisibleForTesting
        const val DRIFT_THRESHOLD = 0.05f

        /**
         * Frames-within-threshold count needed to enter STABILIZING. Kept on
         * the companion (even though STABILIZING is currently unreachable) so
         * the gap with [STABLE_FRAMES] can be reopened by changing one number.
         */
        @VisibleForTesting
        const val STABILIZING_FRAMES = 2

        /**
         * Number of consecutive stable frames to enter STABLE state.
         * 2 frames ≈ 400 ms at the analysis cadence — fast enough that a
         * steady hold lands the shutter almost immediately. The boundary-
         * region motion gate in the ViewModel filters out the one-off
         * corner jitter that 3-frame counting used to absorb.
         */
        @VisibleForTesting
        const val STABLE_FRAMES = 2
    }
}
