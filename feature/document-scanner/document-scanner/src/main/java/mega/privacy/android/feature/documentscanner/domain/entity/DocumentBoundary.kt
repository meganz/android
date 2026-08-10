package mega.privacy.android.feature.documentscanner.domain.entity

/**
 * A 2D point with float coordinates.
 *
 * @property x Horizontal coordinate
 * @property y Vertical coordinate
 */
data class Point(val x: Float, val y: Float)

/**
 * Detected boundary of a document in a camera frame.
 *
 * @property topLeft Top-left corner point
 * @property topRight Top-right corner point
 * @property bottomLeft Bottom-left corner point
 * @property bottomRight Bottom-right corner point
 * @property confidence Detection confidence score (0.0 to 1.0)
 */
data class DocumentBoundary(
    val topLeft: Point,
    val topRight: Point,
    val bottomLeft: Point,
    val bottomRight: Point,
    val confidence: Float,
)
