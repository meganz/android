//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: arrow_up
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

public fun createSmallThinOutlineArrowUpImageVector(): ImageVector = ImageVector.Builder(
    name = "ic_arrow_up_small_thin_outline",
    defaultWidth = 16.0.dp,
    defaultHeight = 16.0.dp,
    viewportWidth = 16.0f,
    viewportHeight = 16.0f
).apply {
    path(
        fill = SolidColor(Color(0xFF04101E)),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 0.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 4.0f,
        pathFillType = PathFillType.NonZero,
    ) {
        moveTo(8.354f, 3.146f)
        curveTo(8.158f, 2.951f, 7.842f, 2.951f, 7.646f, 3.146f)
        lineTo(3.146f, 7.646f)
        curveTo(2.951f, 7.842f, 2.951f, 8.158f, 3.146f, 8.354f)
        curveTo(3.342f, 8.549f, 3.658f, 8.549f, 3.854f, 8.354f)
        lineTo(7.5f, 4.707f)
        lineTo(7.5f, 12.5f)
        curveTo(7.5f, 12.776f, 7.724f, 13.0f, 8.0f, 13.0f)
        curveTo(8.276f, 13.0f, 8.5f, 12.776f, 8.5f, 12.5f)
        lineTo(8.5f, 4.707f)
        lineTo(12.146f, 8.354f)
        curveTo(12.342f, 8.549f, 12.658f, 8.549f, 12.854f, 8.354f)
        curveTo(13.049f, 8.158f, 13.049f, 7.842f, 12.854f, 7.646f)
        lineTo(8.354f, 3.146f)
        close()
    }
}.build()

@Preview
@Composable
private fun SmallThinOutlineArrowUpImageVectorPreview() {
    Icon(
        createSmallThinOutlineArrowUpImageVector(),
        contentDescription = "ArrowUp"
    )
}
