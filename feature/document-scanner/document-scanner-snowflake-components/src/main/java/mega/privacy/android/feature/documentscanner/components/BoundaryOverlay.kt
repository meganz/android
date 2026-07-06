package mega.privacy.android.feature.documentscanner.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens

/** How stable the detected boundary is — drives the overlay colour. */
enum class ScanBoundaryStability { SEARCHING, UNSTABLE, STABILIZING, STABLE }

/**
 * Draws the detected document boundary as a colour-coded quadrilateral over the
 * camera preview, using design-token support colours.
 *
 * @param normalisedCorners The four corners (TL, TR, BR, BL) in normalised
 *   [0,1] frame coordinates, or null when nothing is detected.
 * @param frameWidth Width of the analysed (post-rotation) frame, px.
 * @param frameHeight Height of the analysed (post-rotation) frame, px.
 * @param stability Drives the overlay colour.
 */
@Composable
fun BoundaryOverlay(
    normalisedCorners: List<Offset>?,
    frameWidth: Int,
    frameHeight: Int,
    stability: ScanBoundaryStability,
    modifier: Modifier = Modifier,
) {
    val corners = normalisedCorners ?: return

    val color = when (stability) {
        ScanBoundaryStability.STABLE -> DSTokens.colors.support.success
        else -> DSTokens.colors.support.info
    }

    Canvas(modifier = modifier) {
        val viewCorners = BoundaryOverlayMapper.map(
            normalisedCorners = corners,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            viewWidth = size.width,
            viewHeight = size.height,
        )
        if (viewCorners.size != 4) return@Canvas

        val (tl, tr, br, bl) = viewCorners
        val path = Path().apply {
            moveTo(tl.x, tl.y)
            lineTo(tr.x, tr.y)
            lineTo(br.x, br.y)
            lineTo(bl.x, bl.y)
            close()
        }

        drawPath(path = path, color = color.copy(alpha = 0.15f))
        drawPath(path = path, color = color, style = Stroke(width = 4.dp.toPx()))

        viewCorners.forEach { corner ->
            drawCircle(color = color, radius = 5.dp.toPx(), center = corner)
        }
    }
}
