//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: slash_circle
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

public fun createMediumThinOutlineSlashCircleImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_slash-circle_medium_thin_outline",
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
        moveTo(4.95034f, 6.011f)
        curveTo(3.57804f, 7.62475f, 2.75f, 9.71567f, 2.75f, 12.0f)
        curveTo(2.75f, 17.1086f, 6.89137f, 21.25f, 12.0f, 21.25f)
        curveTo(14.2843f, 21.25f, 16.3753f, 20.422f, 17.989f, 19.0497f)
        lineTo(4.95034f, 6.011f)
        close()
        // Subpath 2 (hole)
        moveTo(6.011f, 4.95034f)
        lineTo(19.0497f, 17.989f)
        curveTo(20.422f, 16.3753f, 21.25f, 14.2843f, 21.25f, 12.0f)
        curveTo(21.25f, 6.89137f, 17.1086f, 2.75f, 12.0f, 2.75f)
        curveTo(9.71567f, 2.75f, 7.62475f, 3.57804f, 6.011f, 4.95034f)
        close()
        // Subpath 3 (hole)
        moveTo(1.25f, 12.0f)
        curveTo(1.25f, 6.06294f, 6.06294f, 1.25f, 12.0f, 1.25f)
        curveTo(17.9371f, 1.25f, 22.75f, 6.06294f, 22.75f, 12.0f)
        curveTo(22.75f, 17.9371f, 17.9371f, 22.75f, 12.0f, 22.75f)
        curveTo(6.06294f, 22.75f, 1.25f, 17.9371f, 1.25f, 12.0f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineSlashCircleImageVectorPreview() {
    Icon(
        createMediumThinOutlineSlashCircleImageVector(),
        contentDescription = "SlashCircle"
    )
}
