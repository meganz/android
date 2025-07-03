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

public fun createMediumRegularOutlineSlashCircleImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_slash-circle_medium_regular_outline",
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
        moveTo(4.9681f, 6.38231f)
        curveTo(3.73647f, 7.92199f, 3.0f, 9.87499f, 3.0f, 12.0f)
        curveTo(3.0f, 16.9706f, 7.02944f, 21.0f, 12.0f, 21.0f)
        curveTo(14.125f, 21.0f, 16.078f, 20.2635f, 17.6177f, 19.0319f)
        lineTo(4.9681f, 6.38231f)
        close()
        // Subpath 2 (hole)
        moveTo(6.38231f, 4.9681f)
        lineTo(19.0319f, 17.6177f)
        curveTo(20.2635f, 16.078f, 21.0f, 14.125f, 21.0f, 12.0f)
        curveTo(21.0f, 7.02944f, 16.9706f, 3.0f, 12.0f, 3.0f)
        curveTo(9.87499f, 3.0f, 7.92199f, 3.73647f, 6.38231f, 4.9681f)
        close()
        // Subpath 3 (hole)
        moveTo(1.0f, 12.0f)
        curveTo(1.0f, 5.92487f, 5.92487f, 1.0f, 12.0f, 1.0f)
        curveTo(18.0751f, 1.0f, 23.0f, 5.92487f, 23.0f, 12.0f)
        curveTo(23.0f, 18.0751f, 18.0751f, 23.0f, 12.0f, 23.0f)
        curveTo(5.92487f, 23.0f, 1.0f, 18.0751f, 1.0f, 12.0f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumRegularOutlineSlashCircleImageVectorPreview() {
    Icon(
        createMediumRegularOutlineSlashCircleImageVector(),
        contentDescription = "SlashCircle"
    )
}
