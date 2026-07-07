package mega.privacy.android.feature.documentscanner.data.boundary

import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The four detected corners expressed in source-image pixels, ordered
 * top-left → top-right → bottom-right → bottom-left (clockwise).
 */
internal data class PixelQuad(
    val topLeft: Point,
    val topRight: Point,
    val bottomRight: Point,
    val bottomLeft: Point,
)

/**
 * Projects a normalised (0–1) [DocumentBoundary] onto pixel coordinates for a
 * [srcWidth] × [srcHeight] image.
 */
internal fun DocumentBoundary.toPixelQuad(srcWidth: Int, srcHeight: Int): PixelQuad = PixelQuad(
    topLeft = Point(topLeft.x * srcWidth, topLeft.y * srcHeight),
    topRight = Point(topRight.x * srcWidth, topRight.y * srcHeight),
    bottomRight = Point(bottomRight.x * srcWidth, bottomRight.y * srcHeight),
    bottomLeft = Point(bottomLeft.x * srcWidth, bottomLeft.y * srcHeight),
)

/**
 * Output size for the rectified page: width is the longer of the two horizontal
 * edges and height the longer of the two vertical edges, so neither axis is
 * squashed relative to what the camera saw. Clamped to at least 1×1.
 */
internal fun PixelQuad.warpTargetSize(): Pair<Int, Int> {
    val width = max(edgeLength(topLeft, topRight), edgeLength(bottomLeft, bottomRight))
    val height = max(edgeLength(topLeft, bottomLeft), edgeLength(topRight, bottomRight))
    return width.roundToInt().coerceAtLeast(1) to height.roundToInt().coerceAtLeast(1)
}

private fun edgeLength(a: Point, b: Point): Float = hypot(a.x - b.x, a.y - b.y)
