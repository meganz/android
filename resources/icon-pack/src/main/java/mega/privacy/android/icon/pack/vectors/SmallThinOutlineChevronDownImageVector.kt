//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: chevron_down
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

public fun createSmallThinOutlineChevronDownImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_chevron-down_small_thin_outline",
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
        moveTo(3.64645f, 5.64645f)
        curveTo(3.84171f, 5.45118f, 4.15829f, 5.45118f, 4.35355f, 5.64645f)
        lineTo(8.0f, 9.29289f)
        lineTo(11.6464f, 5.64645f)
        curveTo(11.8417f, 5.45118f, 12.1583f, 5.45118f, 12.3536f, 5.64645f)
        curveTo(12.5488f, 5.84171f, 12.5488f, 6.15829f, 12.3536f, 6.35355f)
        lineTo(8.35355f, 10.3536f)
        curveTo(8.15829f, 10.5488f, 7.84171f, 10.5488f, 7.64645f, 10.3536f)
        lineTo(3.64645f, 6.35355f)
        curveTo(3.45118f, 6.15829f, 3.45118f, 5.84171f, 3.64645f, 5.64645f)
        close()
    }
}.build()

@Preview
@Composable
private fun SmallThinOutlineChevronDownImageVectorPreview() {
    Icon(
        createSmallThinOutlineChevronDownImageVector(),
        contentDescription = "ChevronDown"
    )
}
