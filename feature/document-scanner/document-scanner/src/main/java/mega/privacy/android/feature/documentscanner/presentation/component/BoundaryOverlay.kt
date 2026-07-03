package mega.privacy.android.feature.documentscanner.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import mega.privacy.android.feature.documentscanner.domain.entity.StabilityState
import mega.privacy.android.feature.documentscanner.presentation.model.BoundaryOverlayState

/**
 * Draws the detected document boundary as a colour-coded quadrilateral over the
 * camera preview.
 *
 * The quad's colour reflects [stabilityState] so the user can tell when the
 * document is held still enough to capture. Normalised detection corners are
 * mapped to view pixels via [BoundaryOverlayMapper], matching the preview's
 * FILL_CENTER scaling.
 *
 * Colours follow the initial PoC design and are intentionally provisional — the
 * final palette (translucent blue per PRD §F1) lands in the UI polish pass.
 *
 * @param overlayState The latest boundary + frame dimensions (nothing is drawn
 *   when [BoundaryOverlayState.boundary] is null).
 * @param stabilityState Drives the overlay colour.
 * @param modifier Modifier for the overlay canvas.
 */
@Composable
internal fun BoundaryOverlay(
    overlayState: BoundaryOverlayState,
    stabilityState: StabilityState,
    modifier: Modifier = Modifier,
) {
    val boundary = overlayState.boundary ?: return

    val color = when (stabilityState) {
        StabilityState.SEARCHING -> Color.Red
        StabilityState.UNSTABLE -> Color.Yellow
        StabilityState.STABILIZING -> Color.Yellow
        StabilityState.STABLE -> Color.Green
    }

    Canvas(modifier = modifier) {
        val corners = BoundaryOverlayMapper.map(
            boundary = boundary,
            frameWidth = overlayState.frameWidth,
            frameHeight = overlayState.frameHeight,
            viewWidth = size.width,
            viewHeight = size.height,
        )
        if (corners.size != 4) return@Canvas

        val (tl, tr, br, bl) = corners
        val path = Path().apply {
            moveTo(tl.x, tl.y)
            lineTo(tr.x, tr.y)
            lineTo(br.x, br.y)
            lineTo(bl.x, bl.y)
            close()
        }

        drawPath(path = path, color = color.copy(alpha = 0.1f))
        drawPath(path = path, color = color, style = Stroke(width = 3f))

        corners.forEach { corner ->
            drawCircle(color = color, radius = 8f, center = Offset(corner.x, corner.y))
        }
    }
}
