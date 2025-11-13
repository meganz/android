//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: arrow_down_circle
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

public fun createSmallThinOutlineArrowDownCircleImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_arrow-down-circle_small_thin_outline",
    defaultWidth = 16.0.dp,
    defaultHeight = 16.0.dp,
    viewportWidth = 16.0f,
    viewportHeight = 16.0f
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
        pathFillType = PathFillType.NonZero,
    ) {
        moveTo(8.0f, 5.0f)
        curveTo(8.27614f, 5.0f, 8.5f, 5.22386f, 8.5f, 5.5f)
        lineTo(8.5f, 9.29289f)
        lineTo(10.1464f, 7.64645f)
        curveTo(10.3417f, 7.45118f, 10.6583f, 7.45118f, 10.8536f, 7.64645f)
        curveTo(11.0488f, 7.84171f, 11.0488f, 8.15829f, 10.8536f, 8.35355f)
        lineTo(8.35355f, 10.8536f)
        curveTo(8.15829f, 11.0488f, 7.84171f, 11.0488f, 7.64645f, 10.8536f)
        lineTo(5.14645f, 8.35355f)
        curveTo(4.95118f, 8.15829f, 4.95118f, 7.84171f, 5.14645f, 7.64645f)
        curveTo(5.34171f, 7.45118f, 5.65829f, 7.45118f, 5.85355f, 7.64645f)
        lineTo(7.5f, 9.29289f)
        lineTo(7.5f, 5.5f)
        curveTo(7.5f, 5.22386f, 7.72386f, 5.0f, 8.0f, 5.0f)
        close()
    }
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
        moveTo(1.0f, 8.0f)
        curveTo(1.0f, 4.13401f, 4.13401f, 1.0f, 8.0f, 1.0f)
        curveTo(11.866f, 1.0f, 15.0f, 4.13401f, 15.0f, 8.0f)
        curveTo(15.0f, 11.866f, 11.866f, 15.0f, 8.0f, 15.0f)
        curveTo(4.13401f, 15.0f, 1.0f, 11.866f, 1.0f, 8.0f)
        close()
        // Subpath 2 (hole)
        moveTo(8.0f, 2.0f)
        curveTo(4.68629f, 2.0f, 2.0f, 4.68629f, 2.0f, 8.0f)
        curveTo(2.0f, 11.3137f, 4.68629f, 14.0f, 8.0f, 14.0f)
        curveTo(11.3137f, 14.0f, 14.0f, 11.3137f, 14.0f, 8.0f)
        curveTo(14.0f, 4.68629f, 11.3137f, 2.0f, 8.0f, 2.0f)
        close()
    }
}.build()

@Preview
@Composable
private fun SmallThinOutlineArrowDownCircleImageVectorPreview() {
    Icon(
        createSmallThinOutlineArrowDownCircleImageVector(),
        contentDescription = "ArrowDownCircle"
    )
}
