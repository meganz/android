//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: chevron_left
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

public fun createMediumThinOutlineChevronLeftImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_chevron-left_medium_thin_outline",
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
        moveTo(15.5303f, 5.46967f)
        curveTo(15.8232f, 5.76256f, 15.8232f, 6.23744f, 15.5303f, 6.53033f)
        lineTo(10.0607f, 12.0f)
        lineTo(15.5303f, 17.4697f)
        curveTo(15.8232f, 17.7626f, 15.8232f, 18.2374f, 15.5303f, 18.5303f)
        curveTo(15.2374f, 18.8232f, 14.7626f, 18.8232f, 14.4697f, 18.5303f)
        lineTo(8.46967f, 12.5303f)
        curveTo(8.17678f, 12.2374f, 8.17678f, 11.7626f, 8.46967f, 11.4697f)
        lineTo(14.4697f, 5.46967f)
        curveTo(14.7626f, 5.17678f, 15.2374f, 5.17678f, 15.5303f, 5.46967f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineChevronLeftImageVectorPreview() {
    Icon(
        createMediumThinOutlineChevronLeftImageVector(),
        contentDescription = "ChevronLeft"
    )
}
