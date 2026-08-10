package mega.privacy.android.feature.documentscanner.data.smoother

import mega.privacy.android.feature.documentscanner.data.boundary.distance
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import mega.privacy.android.feature.documentscanner.domain.smoother.BoundarySmoother
import javax.inject.Inject
import kotlin.math.max

/**
 * EMA smoother: each new corner is blended with the previous smoothed corner.
 * If the new corners jumped more than [SNAP_THRESHOLD] (normalised units) from
 * the previous frame, the smoother snaps to the new position instead of
 * blending — that avoids a slow drift when the user re-positions the camera.
 *
 * Tuning knobs are kept conservative: ALPHA = 0.7 means the displayed corner
 * is 70 % previous + 30 % new each frame, giving a noticeably calmer overlay
 * without too much visible lag at the ~5 fps analysis rate.
 *
 * Not thread-safe by design: [smooth] is only ever called from the single
 * CameraX `ImageAnalysis` executor thread, so the internal state needs no
 * synchronisation. [reset] is the one method that may be called from another
 * thread (on session teardown) — worst case it clears state one frame late,
 * which is harmless.
 */
internal class ExponentialMovingAverageBoundarySmoother @Inject constructor() :
    BoundarySmoother {

    private var previous: DocumentBoundary? = null

    override fun smooth(boundary: DocumentBoundary): DocumentBoundary {
        val prev = previous
        if (prev == null || boundary.hasMovedFarFrom(prev)) {
            previous = boundary
            return boundary
        }
        val smoothed = DocumentBoundary(
            topLeft = blend(prev.topLeft, boundary.topLeft),
            topRight = blend(prev.topRight, boundary.topRight),
            bottomLeft = blend(prev.bottomLeft, boundary.bottomLeft),
            bottomRight = blend(prev.bottomRight, boundary.bottomRight),
            confidence = boundary.confidence,
        )
        previous = smoothed
        return smoothed
    }

    override fun reset() {
        previous = null
    }

    private fun blend(previous: Point, current: Point): Point = Point(
        x = previous.x * ALPHA + current.x * (1f - ALPHA),
        y = previous.y * ALPHA + current.y * (1f - ALPHA),
    )

    private fun DocumentBoundary.hasMovedFarFrom(previous: DocumentBoundary): Boolean {
        val maxShift = max(
            max(distance(topLeft, previous.topLeft), distance(topRight, previous.topRight)),
            max(
                distance(bottomLeft, previous.bottomLeft),
                distance(bottomRight, previous.bottomRight),
            ),
        )
        return maxShift > SNAP_THRESHOLD
    }

    private companion object {
        const val ALPHA = 0.7f
        const val SNAP_THRESHOLD = 0.08f
    }
}
