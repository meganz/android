//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: transfer_check_circle
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

public fun createSmallThinOutlineTransferCheckCircleImageVector(): ImageVector =
        ImageVector.Builder(
    name = "icon_transfer-check-circle_small_thin_outline",
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
        moveTo(17.8002f, 11.6f)
        curveTo(21.2243f, 11.6001f, 24.0004f, 14.3761f, 24.0004f, 17.8002f)
        curveTo(24.0003f, 21.2242f, 21.2242f, 24.0003f, 17.8002f, 24.0004f)
        curveTo(14.3761f, 24.0004f, 11.6001f, 21.2243f, 11.6f, 17.8002f)
        curveTo(11.6f, 14.376f, 14.376f, 11.6f, 17.8002f, 11.6f)
        close()
        // Subpath 2 (hole)
        moveTo(21.0307f, 15.4701f)
        curveTo(20.7378f, 15.1773f, 20.263f, 15.1774f, 19.9701f, 15.4701f)
        lineTo(16.8998f, 18.5395f)
        lineTo(15.6303f, 17.2699f)
        curveTo(15.3374f, 16.9772f, 14.8626f, 16.9772f, 14.5697f, 17.2699f)
        curveTo(14.2771f, 17.5628f, 14.277f, 18.0376f, 14.5697f, 18.3305f)
        lineTo(16.3695f, 20.1303f)
        curveTo(16.6624f, 20.4229f, 17.1373f, 20.4229f, 17.4301f, 20.1303f)
        lineTo(21.0307f, 16.5307f)
        curveTo(21.3232f, 16.2379f, 21.3231f, 15.763f, 21.0307f, 15.4701f)
        close()
    }
}.build()

@Preview
@Composable
private fun SmallThinOutlineTransferCheckCircleImageVectorPreview() {
    Icon(
        createSmallThinOutlineTransferCheckCircleImageVector(),
        contentDescription = "TransferCheckCircle"
    )
}
