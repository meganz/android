//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: arrow_down
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

public fun createSmallThinOutlineArrowDownImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_arrow-down_small_thin_outline",
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
        moveTo(8.5f, 3.5f)
        curveTo(8.5f, 3.22386f, 8.27614f, 3.0f, 8.0f, 3.0f)
        curveTo(7.72386f, 3.0f, 7.5f, 3.22386f, 7.5f, 3.5f)
        lineTo(7.5f, 11.2929f)
        lineTo(3.85355f, 7.64645f)
        curveTo(3.65829f, 7.45118f, 3.34171f, 7.45118f, 3.14645f, 7.64645f)
        curveTo(2.95118f, 7.84171f, 2.95118f, 8.15829f, 3.14645f, 8.35355f)
        lineTo(7.64645f, 12.8536f)
        curveTo(7.84171f, 13.0488f, 8.15829f, 13.0488f, 8.35355f, 12.8536f)
        lineTo(12.8536f, 8.35355f)
        curveTo(13.0488f, 8.15829f, 13.0488f, 7.84171f, 12.8536f, 7.64645f)
        curveTo(12.6583f, 7.45118f, 12.3417f, 7.45118f, 12.1464f, 7.64645f)
        lineTo(8.5f, 11.2929f)
        lineTo(8.5f, 3.5f)
        close()
    }
}.build()

@Preview
@Composable
private fun SmallThinOutlineArrowDownImageVectorPreview() {
    Icon(
        createSmallThinOutlineArrowDownImageVector(),
        contentDescription = "ArrowDown"
    )
}
