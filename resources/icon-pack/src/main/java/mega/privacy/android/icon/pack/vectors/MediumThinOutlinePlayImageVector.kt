//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: play
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

public fun createMediumThinOutlinePlayImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_play_medium_thin_outline",
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
        moveTo(6.0f, 5.48613f)
        curveTo(6.0f, 3.70954f, 7.96232f, 2.63376f, 9.46009f, 3.58923f)
        lineTo(19.671f, 10.1031f)
        curveTo(21.0576f, 10.9876f, 21.0576f, 13.0124f, 19.671f, 13.8969f)
        lineTo(9.46008f, 20.4108f)
        curveTo(7.96231f, 21.3662f, 6.0f, 20.2905f, 6.0f, 18.5139f)
        lineTo(6.0f, 5.48613f)
        close()
        // Subpath 2 (hole)
        moveTo(8.65336f, 4.85383f)
        curveTo(8.15411f, 4.53534f, 7.5f, 4.89393f, 7.5f, 5.48613f)
        lineTo(7.5f, 18.5139f)
        curveTo(7.5f, 19.1061f, 8.15411f, 19.4647f, 8.65336f, 19.1462f)
        lineTo(18.8643f, 12.6323f)
        curveTo(19.3265f, 12.3375f, 19.3265f, 11.6625f, 18.8643f, 11.3677f)
        lineTo(8.65336f, 4.85383f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlinePlayImageVectorPreview() {
    Icon(
        createMediumThinOutlinePlayImageVector(),
        contentDescription = "Play"
    )
}
