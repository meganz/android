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
    name = "ic_play_medium_thin_outline",
    defaultWidth = 24.0.dp,
    defaultHeight = 24.0.dp,
    viewportWidth = 24.0f,
    viewportHeight = 24.0f
).apply {
    path(
        fill = SolidColor(Color(0xFF04101E)),
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
        moveTo(6.0f, 5.486f)
        curveTo(6.0f, 3.71f, 7.962f, 2.634f, 9.46f, 3.589f)
        lineTo(19.671f, 10.103f)
        curveTo(21.058f, 10.988f, 21.058f, 13.012f, 19.671f, 13.897f)
        lineTo(9.46f, 20.411f)
        curveTo(7.962f, 21.366f, 6.0f, 20.29f, 6.0f, 18.514f)
        lineTo(6.0f, 5.486f)
        close()
        // Subpath 2 (hole)
        moveTo(8.653f, 4.854f)
        curveTo(8.154f, 4.535f, 7.5f, 4.894f, 7.5f, 5.486f)
        lineTo(7.5f, 18.514f)
        curveTo(7.5f, 19.106f, 8.154f, 19.465f, 8.653f, 19.146f)
        lineTo(18.864f, 12.632f)
        curveTo(19.326f, 12.337f, 19.326f, 11.663f, 18.864f, 11.368f)
        lineTo(8.653f, 4.854f)
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
