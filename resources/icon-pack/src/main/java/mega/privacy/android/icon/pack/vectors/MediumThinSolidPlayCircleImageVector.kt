//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: play_circle
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

public fun createMediumThinSolidPlayCircleImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_play-circle_medium_thin_solid",
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
        moveTo(10.2704f, 7.99524f)
        curveTo(9.93762f, 7.78133f, 9.5f, 8.02025f, 9.5f, 8.41583f)
        lineTo(9.5f, 15.5842f)
        curveTo(9.5f, 15.9798f, 9.93762f, 16.2187f, 10.2704f, 16.0048f)
        lineTo(15.8457f, 12.4206f)
        curveTo(16.1519f, 12.2238f, 16.1519f, 11.7762f, 15.8457f, 11.5794f)
        lineTo(10.2704f, 7.99524f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinSolidPlayCircleImageVectorPreview() {
    Icon(
        createMediumThinSolidPlayCircleImageVector(),
        contentDescription = "PlayCircle"
    )
}
