package mega.privacy.android.feature.documentscanner.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Draws a rounded-corner rectangle guide showing the user where to place their
 * document. Static chrome — independent of detection.
 *
 * Paddings/colours follow the initial PoC design and are provisional pending the
 * UI polish pass.
 *
 * @param modifier Modifier for the overlay canvas.
 */
@Composable
internal fun ScanGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val horizontalPadding = 32.dp.toPx()
        val topPadding = 80.dp.toPx()
        val bottomPadding = 200.dp.toPx()
        val cornerRadius = 12.dp.toPx()
        val strokeWidth = 2.dp.toPx()

        val rectLeft = horizontalPadding
        val rectTop = topPadding
        val rectWidth = size.width - horizontalPadding * 2
        val rectHeight = size.height - topPadding - bottomPadding

        if (rectWidth > 0 && rectHeight > 0) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}
