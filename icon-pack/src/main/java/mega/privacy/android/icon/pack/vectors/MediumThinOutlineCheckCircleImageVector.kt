//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: check_circle
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

public fun createMediumThinOutlineCheckCircleImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_check-circle_medium_thin_outline",
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
        moveTo(16.5303f, 9.53033f)
        curveTo(16.8232f, 9.23744f, 16.8232f, 8.76256f, 16.5303f, 8.46967f)
        curveTo(16.2374f, 8.17678f, 15.7626f, 8.17678f, 15.4697f, 8.46967f)
        lineTo(10.5f, 13.4393f)
        lineTo(8.53033f, 11.4697f)
        curveTo(8.23744f, 11.1768f, 7.76256f, 11.1768f, 7.46967f, 11.4697f)
        curveTo(7.17678f, 11.7626f, 7.17678f, 12.2374f, 7.46967f, 12.5303f)
        lineTo(9.96967f, 15.0303f)
        curveTo(10.2626f, 15.3232f, 10.7374f, 15.3232f, 11.0303f, 15.0303f)
        lineTo(16.5303f, 9.53033f)
        close()
    }
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
        moveTo(12.0f, 1.25f)
        curveTo(6.06294f, 1.25f, 1.25f, 6.06294f, 1.25f, 12.0f)
        curveTo(1.25f, 17.9371f, 6.06294f, 22.75f, 12.0f, 22.75f)
        curveTo(17.9371f, 22.75f, 22.75f, 17.9371f, 22.75f, 12.0f)
        curveTo(22.75f, 6.06294f, 17.9371f, 1.25f, 12.0f, 1.25f)
        close()
        // Subpath 2 (hole)
        moveTo(2.75f, 12.0f)
        curveTo(2.75f, 6.89137f, 6.89137f, 2.75f, 12.0f, 2.75f)
        curveTo(17.1086f, 2.75f, 21.25f, 6.89137f, 21.25f, 12.0f)
        curveTo(21.25f, 17.1086f, 17.1086f, 21.25f, 12.0f, 21.25f)
        curveTo(6.89137f, 21.25f, 2.75f, 17.1086f, 2.75f, 12.0f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineCheckCircleImageVectorPreview() {
    Icon(
        createMediumThinOutlineCheckCircleImageVector(),
        contentDescription = "CheckCircle"
    )
}
