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

public fun createMediumRegularOutlinePlayImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_play_medium_regular_outline",
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
        moveTo(6.0f, 6.66333f)
        curveTo(6.0f, 4.28983f, 8.62574f, 2.85631f, 10.6223f, 4.1398f)
        lineTo(18.9238f, 9.47646f)
        curveTo(20.7607f, 10.6574f, 20.7607f, 13.3426f, 18.9238f, 14.5235f)
        lineTo(10.6223f, 19.8602f)
        curveTo(8.62574f, 21.1437f, 6.0f, 19.7102f, 6.0f, 17.3367f)
        lineTo(6.0f, 6.66333f)
        close()
        // Subpath 2 (hole)
        moveTo(9.54076f, 5.82215f)
        curveTo(8.87525f, 5.39432f, 8.0f, 5.87217f, 8.0f, 6.66333f)
        lineTo(8.0f, 17.3367f)
        curveTo(8.0f, 18.1278f, 8.87525f, 18.6057f, 9.54076f, 18.1778f)
        lineTo(17.8422f, 12.8412f)
        curveTo(18.4546f, 12.4475f, 18.4546f, 11.5525f, 17.8422f, 11.1588f)
        lineTo(9.54076f, 5.82215f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumRegularOutlinePlayImageVectorPreview() {
    Icon(
        createMediumRegularOutlinePlayImageVector(),
        contentDescription = "Play"
    )
}
