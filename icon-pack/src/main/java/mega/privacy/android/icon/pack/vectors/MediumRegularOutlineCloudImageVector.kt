//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: cloud
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

public fun createMediumRegularOutlineCloudImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_cloud_medium_regular_outline",
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
        moveTo(11.25f, 5.0f)
        curveTo(8.07436f, 5.0f, 5.5f, 7.57436f, 5.5f, 10.75f)
        curveTo(5.5f, 10.8303f, 5.50164f, 10.9101f, 5.50488f, 10.9894f)
        curveTo(5.52612f, 11.5096f, 5.14472f, 11.9591f, 4.62803f, 12.0227f)
        curveTo(3.14711f, 12.2052f, 2.0f, 13.4694f, 2.0f, 15.0f)
        curveTo(2.0f, 16.6569f, 3.34315f, 18.0f, 5.0f, 18.0f)
        lineTo(17.5f, 18.0f)
        curveTo(19.9853f, 18.0f, 22.0f, 15.9853f, 22.0f, 13.5f)
        curveTo(22.0f, 11.0147f, 19.9853f, 9.0f, 17.5f, 9.0f)
        curveTo(17.4771f, 9.0f, 17.4543f, 9.00017f, 17.4314f, 9.00051f)
        curveTo(17.0315f, 9.00646f, 16.6665f, 8.77355f, 16.5034f, 8.40835f)
        curveTo(15.6057f, 6.3979f, 13.59f, 5.0f, 11.25f, 5.0f)
        close()
        // Subpath 2 (hole)
        moveTo(3.51759f, 10.2237f)
        curveTo(3.78819f, 6.18892f, 7.14669f, 3.0f, 11.25f, 3.0f)
        curveTo(14.1806f, 3.0f, 16.7293f, 4.62648f, 18.0462f, 7.02263f)
        curveTo(21.3805f, 7.30003f, 24.0f, 10.0941f, 24.0f, 13.5f)
        curveTo(24.0f, 17.0899f, 21.0898f, 20.0f, 17.5f, 20.0f)
        lineTo(5.0f, 20.0f)
        curveTo(2.23858f, 20.0f, 0.0f, 17.7614f, 0.0f, 15.0f)
        curveTo(0.0f, 12.7544f, 1.47969f, 10.8556f, 3.51759f, 10.2237f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumRegularOutlineCloudImageVectorPreview() {
    Icon(
        createMediumRegularOutlineCloudImageVector(),
        contentDescription = "Cloud"
    )
}
