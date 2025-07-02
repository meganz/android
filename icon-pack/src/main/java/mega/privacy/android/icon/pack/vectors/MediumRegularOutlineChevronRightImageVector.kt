//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: chevron_right
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

public fun createMediumRegularOutlineChevronRightImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_chevron-right_medium_regular_outline",
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
        pathFillType = PathFillType.NonZero,
    ) {
        moveTo(8.29289f, 5.29289f)
        curveTo(8.68342f, 4.90237f, 9.31658f, 4.90237f, 9.70711f, 5.29289f)
        lineTo(15.7071f, 11.2929f)
        curveTo(16.0976f, 11.6834f, 16.0976f, 12.3166f, 15.7071f, 12.7071f)
        lineTo(9.70711f, 18.7071f)
        curveTo(9.31658f, 19.0976f, 8.68342f, 19.0976f, 8.29289f, 18.7071f)
        curveTo(7.90237f, 18.3166f, 7.90237f, 17.6834f, 8.29289f, 17.2929f)
        lineTo(13.5858f, 12.0f)
        lineTo(8.29289f, 6.70711f)
        curveTo(7.90237f, 6.31658f, 7.90237f, 5.68342f, 8.29289f, 5.29289f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumRegularOutlineChevronRightImageVectorPreview() {
    Icon(
        createMediumRegularOutlineChevronRightImageVector(),
        contentDescription = "ChevronRight"
    )
}
