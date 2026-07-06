package mega.privacy.android.feature.documentscanner.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.max

/**
 * Maps document-boundary corners from normalised frame coordinates ([0,1]) into
 * view-pixel coordinates, matching a FILL_CENTER (centre-crop) `PreviewView`
 * scale type: the frame is scaled by the larger axis ratio to fully cover the
 * view, cropping the overflow equally on both sides. The overlay must apply the
 * same transform or the drawn quad drifts from the real document edges.
 *
 * Pure and framework-free (operates on [Offset] values), so it is unit-testable.
 */
internal object BoundaryOverlayMapper {

    /**
     * @param normalisedCorners Corners in normalised (0..1) frame space.
     * @param frameWidth Width of the analysed (post-rotation) frame, px.
     * @param frameHeight Height of the analysed (post-rotation) frame, px.
     * @param viewWidth Width of the preview view, px.
     * @param viewHeight Height of the preview view, px.
     * @return The corners in view-pixel coordinates, or an empty list when any
     *   dimension is non-positive.
     */
    fun map(
        normalisedCorners: List<Offset>,
        frameWidth: Int,
        frameHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
    ): List<Offset> {
        if (frameWidth <= 0 || frameHeight <= 0 || viewWidth <= 0f || viewHeight <= 0f) {
            return emptyList()
        }
        val scale = max(viewWidth / frameWidth, viewHeight / frameHeight)
        val scaledWidth = frameWidth * scale
        val scaledHeight = frameHeight * scale
        val offsetX = (viewWidth - scaledWidth) / 2f
        val offsetY = (viewHeight - scaledHeight) / 2f

        return normalisedCorners.map { corner ->
            Offset(offsetX + corner.x * scaledWidth, offsetY + corner.y * scaledHeight)
        }
    }
}
