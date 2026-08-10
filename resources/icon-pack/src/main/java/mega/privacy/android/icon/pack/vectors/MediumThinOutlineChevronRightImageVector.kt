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

public fun createMediumThinOutlineChevronRightImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_chevron-right_medium_thin_outline",
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
        moveTo(8.46967f, 5.46967f)
        curveTo(8.76256f, 5.17678f, 9.23744f, 5.17678f, 9.53033f, 5.46967f)
        lineTo(15.5303f, 11.4697f)
        curveTo(15.8232f, 11.7626f, 15.8232f, 12.2374f, 15.5303f, 12.5303f)
        lineTo(9.53033f, 18.5303f)
        curveTo(9.23744f, 18.8232f, 8.76256f, 18.8232f, 8.46967f, 18.5303f)
        curveTo(8.17678f, 18.2374f, 8.17678f, 17.7626f, 8.46967f, 17.4697f)
        lineTo(13.9393f, 12.0f)
        lineTo(8.46967f, 6.53033f)
        curveTo(8.17678f, 6.23744f, 8.17678f, 5.76256f, 8.46967f, 5.46967f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineChevronRightImageVectorPreview() {
    Icon(
        createMediumThinOutlineChevronRightImageVector(),
        contentDescription = "ChevronRight"
    )
}
