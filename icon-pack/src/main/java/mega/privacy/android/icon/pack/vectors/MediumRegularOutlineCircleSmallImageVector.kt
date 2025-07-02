//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: circle_small
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

public fun createMediumRegularOutlineCircleSmallImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_circle-small_medium_regular_outline",
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
        moveTo(12.0f, 8.0f)
        curveTo(9.79086f, 8.0f, 8.0f, 9.79086f, 8.0f, 12.0f)
        curveTo(8.0f, 14.2091f, 9.79086f, 16.0f, 12.0f, 16.0f)
        curveTo(14.2091f, 16.0f, 16.0f, 14.2091f, 16.0f, 12.0f)
        curveTo(16.0f, 9.79086f, 14.2091f, 8.0f, 12.0f, 8.0f)
        close()
        // Subpath 2 (hole)
        moveTo(6.0f, 12.0f)
        curveTo(6.0f, 8.68629f, 8.68629f, 6.0f, 12.0f, 6.0f)
        curveTo(15.3137f, 6.0f, 18.0f, 8.68629f, 18.0f, 12.0f)
        curveTo(18.0f, 15.3137f, 15.3137f, 18.0f, 12.0f, 18.0f)
        curveTo(8.68629f, 18.0f, 6.0f, 15.3137f, 6.0f, 12.0f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumRegularOutlineCircleSmallImageVectorPreview() {
    Icon(
        createMediumRegularOutlineCircleSmallImageVector(),
        contentDescription = "CircleSmall"
    )
}
