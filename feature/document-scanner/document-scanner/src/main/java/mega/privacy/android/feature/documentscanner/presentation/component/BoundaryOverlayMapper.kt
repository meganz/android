package mega.privacy.android.feature.documentscanner.presentation.component

import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import kotlin.math.max

/**
 * Maps a document boundary from normalised frame coordinates ([0,1] relative to
 * the analysed frame) into view-pixel coordinates, matching a FILL_CENTER
 * (centre-crop) `PreviewView` scale type.
 *
 * FILL_CENTER scales the frame by the *larger* of the two axis ratios so it
 * fully covers the view, preserving aspect ratio and cropping the overflow
 * equally on both sides. The overlay must apply the exact same transform, or
 * the drawn quad drifts away from the real document edges — especially when the
 * frame and view aspect ratios differ.
 *
 * Pure and framework-free so it can be unit-tested on the JVM; the composable
 * converts the returned [Point]s to Compose `Offset`s.
 */
internal object BoundaryOverlayMapper {

    /**
     * @param boundary Normalised (0..1) corners in analysed-frame space.
     * @param frameWidth Width of the analysed (post-rotation) frame, px.
     * @param frameHeight Height of the analysed (post-rotation) frame, px.
     * @param viewWidth Width of the preview view, px.
     * @param viewHeight Height of the preview view, px.
     * @return The four corners (TL, TR, BR, BL) in view-pixel coordinates, or an
     *   empty list when any dimension is non-positive (nothing sensible to map).
     */
    fun map(
        boundary: DocumentBoundary,
        frameWidth: Int,
        frameHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
    ): List<Point> {
        if (frameWidth <= 0 || frameHeight <= 0 || viewWidth <= 0f || viewHeight <= 0f) {
            return emptyList()
        }
        val scale = max(viewWidth / frameWidth, viewHeight / frameHeight)
        val scaledWidth = frameWidth * scale
        val scaledHeight = frameHeight * scale
        val offsetX = (viewWidth - scaledWidth) / 2f
        val offsetY = (viewHeight - scaledHeight) / 2f

        fun project(p: Point) = Point(
            x = offsetX + p.x * scaledWidth,
            y = offsetY + p.y * scaledHeight,
        )

        return listOf(
            project(boundary.topLeft),
            project(boundary.topRight),
            project(boundary.bottomRight),
            project(boundary.bottomLeft),
        )
    }
}
