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

public fun createMediumThinOutlineArrowLeftImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_arrow-left_medium_thin_outline",
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
        moveTo(12.5303f, 5.28033f)
        curveTo(12.8232f, 4.98744f, 12.8232f, 4.51256f, 12.5303f, 4.21967f)
        curveTo(12.2374f, 3.92678f, 11.7626f, 3.92678f, 11.4697f, 4.21967f)
        lineTo(4.21967f, 11.4697f)
        curveTo(3.92678f, 11.7626f, 3.92678f, 12.2374f, 4.21967f, 12.5303f)
        lineTo(11.4697f, 19.7803f)
        curveTo(11.7626f, 20.0732f, 12.2374f, 20.0732f, 12.5303f, 19.7803f)
        curveTo(12.8232f, 19.4874f, 12.8232f, 19.0126f, 12.5303f, 18.7197f)
        lineTo(6.56066f, 12.75f)
        lineTo(19.25f, 12.75f)
        curveTo(19.6642f, 12.75f, 20.0f, 12.4142f, 20.0f, 12.0f)
        curveTo(20.0f, 11.5858f, 19.6642f, 11.25f, 19.25f, 11.25f)
        lineTo(6.56066f, 11.25f)
        lineTo(12.5303f, 5.28033f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineArrowLeftImageVectorPreview() {
    Icon(
        createMediumThinOutlineArrowLeftImageVector(),
        contentDescription = "ArrowLeft"
    )
}
