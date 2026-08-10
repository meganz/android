package mega.privacy.android.feature.documentscanner.domain.smoother

import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary

/**
 * Smooths the per-frame jitter out of the displayed document quadrilateral so
 * the overlay glides instead of flickering. Operates entirely in normalised
 * (0–1) corner coordinates.
 *
 * Implementations are stateful: callers must invoke [reset] when detection is
 * lost (e.g., the camera moves away from the document) so the next detection
 * snaps to its real position instead of slowly drifting in from the stale one.
 */
interface BoundarySmoother {
    /**
     * @return the [boundary] blended with the previous smoothed corners. If
     *         the new corners moved more than the snap threshold, the
     *         smoother resets to the new boundary and returns it unchanged.
     */
    fun smooth(boundary: DocumentBoundary): DocumentBoundary

    /** Forget the last smoothed boundary. */
    fun reset()
}
