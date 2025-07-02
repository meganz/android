//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: check_circle
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

public fun createMediumRegularSolidCheckCircleImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_check-circle_medium_regular_solid",
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
        moveTo(23.0f, 12.0f)
        curveTo(23.0f, 18.0751f, 18.0751f, 23.0f, 12.0f, 23.0f)
        curveTo(5.92487f, 23.0f, 1.0f, 18.0751f, 1.0f, 12.0f)
        curveTo(1.0f, 5.92487f, 5.92487f, 1.0f, 12.0f, 1.0f)
        curveTo(18.0751f, 1.0f, 23.0f, 5.92487f, 23.0f, 12.0f)
        close()
        // Subpath 2 (hole)
        moveTo(16.7071f, 9.70711f)
        curveTo(17.0976f, 9.31658f, 17.0976f, 8.68342f, 16.7071f, 8.29289f)
        curveTo(16.3166f, 7.90237f, 15.6834f, 7.90237f, 15.2929f, 8.29289f)
        lineTo(10.5f, 13.0858f)
        lineTo(8.70711f, 11.2929f)
        curveTo(8.31658f, 10.9024f, 7.68342f, 10.9024f, 7.29289f, 11.2929f)
        curveTo(6.90237f, 11.6834f, 6.90237f, 12.3166f, 7.29289f, 12.7071f)
        lineTo(9.79289f, 15.2071f)
        curveTo(10.1834f, 15.5976f, 10.8166f, 15.5976f, 11.2071f, 15.2071f)
        lineTo(16.7071f, 9.70711f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumRegularSolidCheckCircleImageVectorPreview() {
    Icon(
        createMediumRegularSolidCheckCircleImageVector(),
        contentDescription = "CheckCircle"
    )
}
