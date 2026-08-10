//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: transfer_error
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

public fun createSmallThinOutlineTransferErrorImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_transfer-error_small_thin_outline",
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
        moveTo(17.6f, 19.35f)
        curveTo(17.1858f, 19.35f, 16.85f, 19.6858f, 16.85f, 20.1f)
        curveTo(16.8501f, 20.5142f, 17.1858f, 20.85f, 17.6f, 20.85f)
        curveTo(18.0142f, 20.85f, 18.3499f, 20.5142f, 18.35f, 20.1f)
        curveTo(18.35f, 19.6858f, 18.0142f, 19.35f, 17.6f, 19.35f)
        close()
        // Subpath 3 (hole)
        moveTo(17.6f, 14.6f)
        curveTo(17.1858f, 14.6f, 16.85f, 14.9358f, 16.85f, 15.35f)
        lineTo(16.85f, 17.85f)
        curveTo(16.85f, 18.2642f, 17.1858f, 18.6f, 17.6f, 18.6f)
        curveTo(18.0142f, 18.6f, 18.35f, 18.2642f, 18.35f, 17.85f)
        lineTo(18.35f, 15.35f)
        curveTo(18.35f, 14.9358f, 18.0142f, 14.6f, 17.6f, 14.6f)
        close()
    }
}.build()

@Preview
@Composable
private fun SmallThinOutlineTransferErrorImageVectorPreview() {
    Icon(
        createSmallThinOutlineTransferErrorImageVector(),
        contentDescription = "TransferError"
    )
}
