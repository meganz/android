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

public fun createMediumThinOutlineCircleSmallImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_circle-small_medium_thin_outline",
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
        moveTo(12.0f, 7.75f)
        curveTo(9.65279f, 7.75f, 7.75f, 9.65279f, 7.75f, 12.0f)
        curveTo(7.75f, 14.3472f, 9.65279f, 16.25f, 12.0f, 16.25f)
        curveTo(14.3472f, 16.25f, 16.25f, 14.3472f, 16.25f, 12.0f)
        curveTo(16.25f, 9.65279f, 14.3472f, 7.75f, 12.0f, 7.75f)
        close()
        // Subpath 2 (hole)
        moveTo(6.25f, 12.0f)
        curveTo(6.25f, 8.82436f, 8.82436f, 6.25f, 12.0f, 6.25f)
        curveTo(15.1756f, 6.25f, 17.75f, 8.82436f, 17.75f, 12.0f)
        curveTo(17.75f, 15.1756f, 15.1756f, 17.75f, 12.0f, 17.75f)
        curveTo(8.82436f, 17.75f, 6.25f, 15.1756f, 6.25f, 12.0f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineCircleSmallImageVectorPreview() {
    Icon(
        createMediumThinOutlineCircleSmallImageVector(),
        contentDescription = "CircleSmall"
    )
}
