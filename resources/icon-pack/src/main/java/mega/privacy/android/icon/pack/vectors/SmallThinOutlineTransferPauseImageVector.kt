//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: transfer_pause
//
package mega.privacy.android.icon.pack.vectors

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

public fun createSmallThinOutlineTransferPauseImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_transfer-pause_small_thin_outline",
    defaultWidth = 24.0.dp,
    defaultHeight = 24.0.dp,
    viewportWidth = 24.0f,
    viewportHeight = 24.0f
).apply {
    path(
        fill = SolidColor(Color(0xFF303233)),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 0.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 4.0f,
        pathFillType = PathFillType.EvenOdd,
    ) {
        // Subpath 1
        moveTo(17.8f, 11.6f)
        curveTo(14.3758f, 11.6f, 11.6f, 14.3758f, 11.6f, 17.8f)
        curveTo(11.6f, 21.2242f, 14.3758f, 24.0f, 17.8f, 24.0f)
        curveTo(21.2242f, 24.0f, 24.0f, 21.2242f, 24.0f, 17.8f)
        curveTo(24.0f, 14.3758f, 21.2242f, 11.6f, 17.8f, 11.6f)
        close()
        // Subpath 2 (hole)
        moveTo(17.0499f, 15.8f)
        curveTo(17.0499f, 15.3857f, 16.7142f, 15.05f, 16.2999f, 15.05f)
        curveTo(15.8857f, 15.05f, 15.5499f, 15.3857f, 15.5499f, 15.8f)
        lineTo(15.5499f, 19.8f)
        curveTo(15.5499f, 20.2142f, 15.8857f, 20.55f, 16.2999f, 20.55f)
        curveTo(16.7142f, 20.55f, 17.0499f, 20.2142f, 17.0499f, 19.8f)
        lineTo(17.0499f, 15.8f)
        close()
        // Subpath 3 (hole)
        moveTo(20.0499f, 15.8f)
        curveTo(20.0499f, 15.3857f, 19.7142f, 15.05f, 19.2999f, 15.05f)
        curveTo(18.8857f, 15.05f, 18.5499f, 15.3857f, 18.5499f, 15.8f)
        lineTo(18.5499f, 19.8f)
        curveTo(18.5499f, 20.2142f, 18.8857f, 20.55f, 19.2999f, 20.55f)
        curveTo(19.7142f, 20.55f, 20.0499f, 20.2142f, 20.0499f, 19.8f)
        lineTo(20.0499f, 15.8f)
        close()
    }
}.build()

@Preview
@Composable
private fun SmallThinOutlineTransferPauseImageVectorPreview() {
    Icon(
        createSmallThinOutlineTransferPauseImageVector(),
        contentDescription = "TransferPause"
    )
}
