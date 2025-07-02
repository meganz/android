//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: check
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

public fun createMediumRegularOutlineCheckImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_check_medium_regular_outline",
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
        moveTo(19.7071f, 6.29289f)
        curveTo(20.0976f, 6.68342f, 20.0976f, 7.31658f, 19.7071f, 7.70711f)
        lineTo(9.70711f, 17.7071f)
        curveTo(9.31658f, 18.0976f, 8.68342f, 18.0976f, 8.29289f, 17.7071f)
        lineTo(4.29289f, 13.7071f)
        curveTo(3.90237f, 13.3166f, 3.90237f, 12.6834f, 4.29289f, 12.2929f)
        curveTo(4.68342f, 11.9024f, 5.31658f, 11.9024f, 5.70711f, 12.2929f)
        lineTo(9.0f, 15.5858f)
        lineTo(18.2929f, 6.29289f)
        curveTo(18.6834f, 5.90237f, 19.3166f, 5.90237f, 19.7071f, 6.29289f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumRegularOutlineCheckImageVectorPreview() {
    Icon(
        createMediumRegularOutlineCheckImageVector(),
        contentDescription = "Check"
    )
}
