//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: minus_circle
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

public fun createMediumThinOutlineMinusCircleImageVector(): ImageVector = ImageVector.Builder(
    name = "ic_minus_circle_medium_thin_outline",
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
        moveTo(8.0f, 11.25f)
        curveTo(7.586f, 11.25f, 7.25f, 11.586f, 7.25f, 12.0f)
        curveTo(7.25f, 12.414f, 7.586f, 12.75f, 8.0f, 12.75f)
        lineTo(16.0f, 12.75f)
        curveTo(16.414f, 12.75f, 16.75f, 12.414f, 16.75f, 12.0f)
        curveTo(16.75f, 11.586f, 16.414f, 11.25f, 16.0f, 11.25f)
        lineTo(8.0f, 11.25f)
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
        moveTo(12.0f, 1.0f)
        curveTo(5.925f, 1.0f, 1.0f, 5.925f, 1.0f, 12.0f)
        curveTo(1.0f, 18.075f, 5.925f, 23.0f, 12.0f, 23.0f)
        curveTo(18.075f, 23.0f, 23.0f, 18.075f, 23.0f, 12.0f)
        curveTo(23.0f, 5.925f, 18.075f, 1.0f, 12.0f, 1.0f)
        close()
        // Subpath 2 (hole)
        moveTo(2.5f, 12.0f)
        curveTo(2.5f, 6.753f, 6.753f, 2.5f, 12.0f, 2.5f)
        curveTo(17.247f, 2.5f, 21.5f, 6.753f, 21.5f, 12.0f)
        curveTo(21.5f, 17.247f, 17.247f, 21.5f, 12.0f, 21.5f)
        curveTo(6.753f, 21.5f, 2.5f, 17.247f, 2.5f, 12.0f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineMinusCircleImageVectorPreview() {
    Icon(
        createMediumThinOutlineMinusCircleImageVector(),
        contentDescription = "MinusCircle"
    )
}
