//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: transfer_warning
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

public fun createSmallThinOutlineTransferWarningImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_transfer-warning_small_thin_outline",
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
        moveTo(15.4848f, 12.5541f)
        curveTo(16.2191f, 11.2823f, 18.0543f, 11.2823f, 18.7886f, 12.5541f)
        lineTo(23.7456f, 21.1381f)
        curveTo(24.4798f, 22.4098f, 23.5617f, 23.9993f, 22.0932f, 23.9994f)
        lineTo(12.1802f, 23.9994f)
        curveTo(10.7118f, 23.9992f, 9.79457f, 22.4098f, 10.5288f, 21.1381f)
        lineTo(15.4848f, 12.5541f)
        close()
        // Subpath 2 (hole)
        moveTo(16.938f, 20.0502f)
        curveTo(16.5238f, 20.0502f, 16.188f, 20.386f, 16.188f, 20.8002f)
        curveTo(16.1882f, 21.2142f, 16.5239f, 21.5502f, 16.938f, 21.5502f)
        curveTo(17.3519f, 21.55f, 17.6878f, 21.2141f, 17.688f, 20.8002f)
        curveTo(17.688f, 20.3861f, 17.352f, 20.0504f, 16.938f, 20.0502f)
        close()
        // Subpath 3 (hole)
        moveTo(16.938f, 15.3002f)
        curveTo(16.5238f, 15.3002f, 16.188f, 15.636f, 16.188f, 16.0502f)
        lineTo(16.188f, 18.5502f)
        curveTo(16.1881f, 18.9643f, 16.5238f, 19.3002f, 16.938f, 19.3002f)
        curveTo(17.3519f, 19.3f, 17.6878f, 18.9642f, 17.688f, 18.5502f)
        lineTo(17.688f, 16.0502f)
        curveTo(17.688f, 15.6361f, 17.352f, 15.3004f, 16.938f, 15.3002f)
        close()
    }
}.build()

@Preview
@Composable
private fun SmallThinOutlineTransferWarningImageVectorPreview() {
    Icon(
        createSmallThinOutlineTransferWarningImageVector(),
        contentDescription = "TransferWarning"
    )
}
