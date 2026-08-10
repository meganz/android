package mega.privacy.android.feature.documentscanner.data.boundary

import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import kotlin.math.hypot

/**
 * Small shared geometry helpers for boundary maths, kept in one place so the
 * stability tracker and the smoother don't each carry their own copy of the
 * same distance formula. Operates in normalised (0–1) corner coordinates.
 */

/** Euclidean distance between two normalised points. */
internal fun distance(a: Point, b: Point): Float = hypot(a.x - b.x, a.y - b.y)

/**
 * Largest per-corner Euclidean distance between two boundaries — i.e. how far
 * the most-moved corner travelled between the two frames.
 */
internal fun maxCornerDrift(a: DocumentBoundary, b: DocumentBoundary): Float = maxOf(
    distance(a.topLeft, b.topLeft),
    distance(a.topRight, b.topRight),
    distance(a.bottomLeft, b.bottomLeft),
    distance(a.bottomRight, b.bottomRight),
)
