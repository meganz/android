//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: pause_circle
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

public fun createMediumThinSolidPauseCircleImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_pause-circle_medium_thin_solid",
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
        moveTo(22.75f, 12.0f)
        curveTo(22.75f, 17.9371f, 17.9371f, 22.75f, 12.0f, 22.75f)
        curveTo(6.06294f, 22.75f, 1.25f, 17.9371f, 1.25f, 12.0f)
        curveTo(1.25f, 6.06294f, 6.06294f, 1.25f, 12.0f, 1.25f)
        curveTo(17.9371f, 1.25f, 22.75f, 6.06294f, 22.75f, 12.0f)
        close()
        // Subpath 2 (hole)
        moveTo(9.25f, 8.25f)
        curveTo(9.66421f, 8.25f, 10.0f, 8.58579f, 10.0f, 9.0f)
        lineTo(10.0f, 15.0f)
        curveTo(10.0f, 15.4142f, 9.66421f, 15.75f, 9.25f, 15.75f)
        curveTo(8.83579f, 15.75f, 8.5f, 15.4142f, 8.5f, 15.0f)
        lineTo(8.5f, 9.0f)
        curveTo(8.5f, 8.58579f, 8.83579f, 8.25f, 9.25f, 8.25f)
        close()
        // Subpath 3 (hole)
        moveTo(14.75f, 8.25f)
        curveTo(15.1642f, 8.25f, 15.5f, 8.58579f, 15.5f, 9.0f)
        lineTo(15.5f, 15.0f)
        curveTo(15.5f, 15.4142f, 15.1642f, 15.75f, 14.75f, 15.75f)
        curveTo(14.3358f, 15.75f, 14.0f, 15.4142f, 14.0f, 15.0f)
        lineTo(14.0f, 9.0f)
        curveTo(14.0f, 8.58579f, 14.3358f, 8.25f, 14.75f, 8.25f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinSolidPauseCircleImageVectorPreview() {
    Icon(
        createMediumThinSolidPauseCircleImageVector(),
        contentDescription = "PauseCircle"
    )
}
