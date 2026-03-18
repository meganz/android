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
    name = "icon_arrow-up_small_thin_outline",
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
        moveTo(8.35355f, 3.14645f)
        curveTo(8.15829f, 2.95118f, 7.84171f, 2.95118f, 7.64645f, 3.14645f)
        lineTo(3.14645f, 7.64645f)
        curveTo(2.95118f, 7.84171f, 2.95118f, 8.15829f, 3.14645f, 8.35355f)
        curveTo(3.34171f, 8.54882f, 3.65829f, 8.54882f, 3.85355f, 8.35355f)
        lineTo(7.5f, 4.70711f)
        lineTo(7.5f, 12.5f)
        curveTo(7.5f, 12.7761f, 7.72386f, 13.0f, 8.0f, 13.0f)
        curveTo(8.27614f, 13.0f, 8.5f, 12.7761f, 8.5f, 12.5f)
        lineTo(8.5f, 4.70711f)
        lineTo(12.1464f, 8.35355f)
        curveTo(12.3417f, 8.54882f, 12.6583f, 8.54882f, 12.8536f, 8.35355f)
        curveTo(13.0488f, 8.15829f, 13.0488f, 7.84171f, 12.8536f, 7.64645f)
        lineTo(8.35355f, 3.14645f)
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
