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

public fun createMediumRegularSolidPlayImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_play_medium_regular_solid",
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
        moveTo(6.0f, 6.66332f)
        curveTo(6.0f, 4.28983f, 8.62574f, 2.8563f, 10.6223f, 4.13979f)
        lineTo(18.9238f, 9.47646f)
        curveTo(20.7607f, 10.6574f, 20.7607f, 13.3426f, 18.9238f, 14.5235f)
        lineTo(10.6223f, 19.8602f)
        curveTo(8.62574f, 21.1437f, 6.0f, 19.7102f, 6.0f, 17.3367f)
        lineTo(6.0f, 6.66332f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumRegularSolidPlayImageVectorPreview() {
    Icon(
        createMediumRegularSolidPlayImageVector(),
        contentDescription = "Play"
    )
}
