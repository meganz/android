//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: filter
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

public fun createMediumThinOutlineFilterImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_filter_medium_thin_outline",
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
        moveTo(2.81946f, 5.13002f)
        curveTo(1.85756f, 3.99257f, 2.66606f, 2.25f, 4.15571f, 2.25f)
        lineTo(19.8451f, 2.25f)
        curveTo(21.3348f, 2.25f, 22.1432f, 3.99257f, 21.1813f, 5.13002f)
        lineTo(14.7504f, 12.7346f)
        lineTo(14.7504f, 20.191f)
        curveTo(14.7504f, 21.1202f, 13.7725f, 21.7246f, 12.9414f, 21.309f)
        lineTo(9.94139f, 19.809f)
        curveTo(9.51791f, 19.5973f, 9.2504f, 19.1644f, 9.2504f, 18.691f)
        lineTo(9.2504f, 12.7346f)
        lineTo(2.81946f, 5.13002f)
        close()
        // Subpath 2 (hole)
        moveTo(4.15571f, 3.75f)
        curveTo(3.9429f, 3.75f, 3.8274f, 3.99894f, 3.96482f, 4.16143f)
        lineTo(10.4549f, 11.8359f)
        curveTo(10.6457f, 12.0616f, 10.7504f, 12.3475f, 10.7504f, 12.6431f)
        lineTo(10.7504f, 18.5365f)
        lineTo(13.2504f, 19.7865f)
        lineTo(13.2504f, 12.6431f)
        curveTo(13.2504f, 12.3475f, 13.3551f, 12.0616f, 13.5459f, 11.8359f)
        lineTo(20.036f, 4.16143f)
        curveTo(20.1734f, 3.99894f, 20.0579f, 3.75f, 19.8451f, 3.75f)
        lineTo(4.15571f, 3.75f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineFilterImageVectorPreview() {
    Icon(
        createMediumThinOutlineFilterImageVector(),
        contentDescription = "Filter"
    )
}
