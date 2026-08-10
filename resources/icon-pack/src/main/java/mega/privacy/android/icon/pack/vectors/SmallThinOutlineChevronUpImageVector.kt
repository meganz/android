//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: chevron_up
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

public fun createSmallThinOutlineChevronUpImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_chevron-up_small_thin_outline",
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
        moveTo(7.64645f, 5.64645f)
        curveTo(7.84171f, 5.45118f, 8.15829f, 5.45118f, 8.35355f, 5.64645f)
        lineTo(12.3536f, 9.64645f)
        curveTo(12.5488f, 9.84171f, 12.5488f, 10.1583f, 12.3536f, 10.3536f)
        curveTo(12.1583f, 10.5488f, 11.8417f, 10.5488f, 11.6464f, 10.3536f)
        lineTo(8.0f, 6.70711f)
        lineTo(4.35355f, 10.3536f)
        curveTo(4.15829f, 10.5488f, 3.84171f, 10.5488f, 3.64645f, 10.3536f)
        curveTo(3.45118f, 10.1583f, 3.45118f, 9.84171f, 3.64645f, 9.64645f)
        lineTo(7.64645f, 5.64645f)
        close()
    }
}.build()

@Preview
@Composable
private fun SmallThinOutlineChevronUpImageVectorPreview() {
    Icon(
        createSmallThinOutlineChevronUpImageVector(),
        contentDescription = "ChevronUp"
    )
}
