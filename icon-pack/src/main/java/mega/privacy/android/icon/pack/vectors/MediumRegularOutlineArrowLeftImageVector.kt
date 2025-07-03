//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: arrow_left
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

public fun createMediumRegularOutlineArrowLeftImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_arrow-left_medium_regular_outline",
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
        moveTo(12.7071f, 5.70711f)
        curveTo(13.0976f, 5.31658f, 13.0976f, 4.68342f, 12.7071f, 4.29289f)
        curveTo(12.3166f, 3.90237f, 11.6834f, 3.90237f, 11.2929f, 4.29289f)
        lineTo(4.29289f, 11.2929f)
        curveTo(3.90237f, 11.6834f, 3.90237f, 12.3166f, 4.29289f, 12.7071f)
        lineTo(11.2929f, 19.7071f)
        curveTo(11.6834f, 20.0976f, 12.3166f, 20.0976f, 12.7071f, 19.7071f)
        curveTo(13.0976f, 19.3166f, 13.0976f, 18.6834f, 12.7071f, 18.2929f)
        lineTo(7.41421f, 13.0f)
        lineTo(19.0f, 13.0f)
        curveTo(19.5523f, 13.0f, 20.0f, 12.5523f, 20.0f, 12.0f)
        curveTo(20.0f, 11.4477f, 19.5523f, 11.0f, 19.0f, 11.0f)
        lineTo(7.41421f, 11.0f)
        lineTo(12.7071f, 5.70711f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumRegularOutlineArrowLeftImageVectorPreview() {
    Icon(
        createMediumRegularOutlineArrowLeftImageVector(),
        contentDescription = "ArrowLeft"
    )
}
