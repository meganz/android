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

public fun createMediumThinOutlineCheckImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_check_medium_thin_outline",
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
        moveTo(19.5303f, 6.46967f)
        curveTo(19.8232f, 6.76256f, 19.8232f, 7.23744f, 19.5303f, 7.53033f)
        lineTo(9.53033f, 17.5303f)
        curveTo(9.23744f, 17.8232f, 8.76256f, 17.8232f, 8.46967f, 17.5303f)
        lineTo(4.46967f, 13.5303f)
        curveTo(4.17678f, 13.2374f, 4.17678f, 12.7626f, 4.46967f, 12.4697f)
        curveTo(4.76256f, 12.1768f, 5.23744f, 12.1768f, 5.53033f, 12.4697f)
        lineTo(9.0f, 15.9393f)
        lineTo(18.4697f, 6.46967f)
        curveTo(18.7626f, 6.17678f, 19.2374f, 6.17678f, 19.5303f, 6.46967f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineCheckImageVectorPreview() {
    Icon(
        createMediumThinOutlineCheckImageVector(),
        contentDescription = "Check"
    )
}
