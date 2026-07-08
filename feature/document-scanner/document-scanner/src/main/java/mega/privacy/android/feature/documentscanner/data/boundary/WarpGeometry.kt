package mega.privacy.android.feature.documentscanner.data.boundary

import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
 * Output size for the rectified page.
 *
 * A tilted document projects to a perspective trapezoid, so the on-image edge
 * lengths do **not** equal the real page proportions — sizing the output to the
 * raw edges collapses tall pages toward square. Instead we recover the page's
 * true width:height with [estimateWidthHeightRatio] (a projective estimate from
 * the four corners and the camera's principal point) and derive the height from
 * a fixed reference width. When the estimate is unstable (degenerate quad, no
 * perspective) we fall back to the longer-edge heuristic.
 *
 * @param imageWidth Width of the source image the quad lives in, px.
 * @param imageHeight Height of the source image the quad lives in, px.
 */
internal fun PixelQuad.warpTargetSize(imageWidth: Int, imageHeight: Int): Pair<Int, Int> {
    val referenceWidth = max(edgeLength(topLeft, topRight), edgeLength(bottomLeft, bottomRight))
    val ratio = estimateWidthHeightRatio(imageWidth, imageHeight) ?: averageEdgeRatio()
    val height = if (ratio > 0f) referenceWidth / ratio else referenceWidth
    return referenceWidth.roundToInt().coerceAtLeast(1) to height.roundToInt().coerceAtLeast(1)
}

/**
 * Fallback aspect from the mean of each opposing edge pair — used when the
 * projective estimate is degenerate (e.g. a pure single-axis tilt, where one
 * pair of edges is parallel in the image and the focal length can't be recovered
 * from the corners alone). Steadier than the longer-edge heuristic.
 */
private fun PixelQuad.averageEdgeRatio(): Float {
    val width = (edgeLength(topLeft, topRight) + edgeLength(bottomLeft, bottomRight)) / 2f
    val height = (edgeLength(topLeft, bottomLeft) + edgeLength(topRight, bottomRight)) / 2f
    return if (height > 0f) width / height else 1f
}

/**
 * Estimates the true width:height ratio of the physical rectangle from its
 * perspective projection, following Zhang's closed form (the camera's focal
 * length is recovered from the quad itself, with the principal point assumed at
 * the image centre). Returns null when the geometry is degenerate or too close
 * to affine to solve, so the caller can fall back.
 */
private fun PixelQuad.estimateWidthHeightRatio(imageWidth: Int, imageHeight: Int): Float? {
    val u0 = imageWidth / 2.0
    val v0 = imageHeight / 2.0

    // Corners relative to the principal point. m1=TL, m2=TR, m3=BL, m4=BR.
    val m1x = topLeft.x - u0; val m1y = topLeft.y - v0
    val m2x = topRight.x - u0; val m2y = topRight.y - v0
    val m3x = bottomLeft.x - u0; val m3y = bottomLeft.y - v0
    val m4x = bottomRight.x - u0; val m4y = bottomRight.y - v0

    val k2Den = (m2y - m4y) * m3x - (m2x - m4x) * m3y + m2x * m4y - m2y * m4x
    val k3Den = (m3y - m4y) * m2x - (m3x - m4x) * m2y + m3x * m4y - m3y * m4x
    if (abs(k2Den) < EPSILON || abs(k3Den) < EPSILON) return null

    val k2 = ((m1y - m4y) * m3x - (m1x - m4x) * m3y + m1x * m4y - m1y * m4x) / k2Den
    val k3 = ((m1y - m4y) * m2x - (m1x - m4x) * m2y + m1x * m4y - m1y * m4x) / k3Den

    // Near-affine (no perspective): ratio is just the edge-length ratio.
    if (abs(k2 - 1.0) < EPSILON && abs(k3 - 1.0) < EPSILON) {
        return affineRatio(m1x, m1y, m2x, m2y, m3x, m3y)
    }

    // A single-axis tilt leaves one pair of edges parallel (k≈1), so the focal
    // length is unrecoverable from the corners — bail to the caller's fallback.
    val focalDen = (k3 - 1.0) * (k2 - 1.0)
    if (abs(focalDen) < FOCAL_DEN_MIN) return null
    val focalSq = -(
        (k3 * m3y - m1y) * (k2 * m2y - m1y) + (k3 * m3x - m1x) * (k2 * m2x - m1x)
        ) / focalDen
    if (focalSq <= 0.0) return null

    val ratioSq = (
        square(k2 - 1.0) + square(k2 * m2y - m1y) / focalSq + square(k2 * m2x - m1x) / focalSq
        ) / (
        square(k3 - 1.0) + square(k3 * m3y - m1y) / focalSq + square(k3 * m3x - m1x) / focalSq
        )
    val ratio = sqrt(ratioSq)
    return if (ratio.isFinite() && ratio in MIN_RATIO..MAX_RATIO) ratio.toFloat() else null
}

private fun affineRatio(
    m1x: Double, m1y: Double, m2x: Double, m2y: Double, m3x: Double, m3y: Double,
): Float? {
    val width = hypot(m2x - m1x, m2y - m1y)
    val height = hypot(m3x - m1x, m3y - m1y)
    return if (height > 0.0) (width / height).toFloat() else null
}

private fun square(value: Double): Double = value * value

private fun edgeLength(a: Point, b: Point): Float = hypot(a.x - b.x, a.y - b.y)

private const val EPSILON = 1e-6

/** Below this, the focal-length denominator is treated as degenerate (parallel edges). */
private const val FOCAL_DEN_MIN = 1e-4

/** Sane page-aspect bounds; an estimate outside these is rejected as unstable. */
private const val MIN_RATIO = 0.1
private const val MAX_RATIO = 10.0
