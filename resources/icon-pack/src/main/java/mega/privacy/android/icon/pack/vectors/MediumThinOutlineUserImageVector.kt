//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: user
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

public fun createMediumThinOutlineUserImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_user_medium_thin_outline",
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
        moveTo(12.0f, 3.25f)
        curveTo(9.37665f, 3.25f, 7.25f, 5.37665f, 7.25f, 8.0f)
        curveTo(7.25f, 10.6234f, 9.37665f, 12.75f, 12.0f, 12.75f)
        curveTo(14.6234f, 12.75f, 16.75f, 10.6234f, 16.75f, 8.0f)
        curveTo(16.75f, 5.37665f, 14.6234f, 3.25f, 12.0f, 3.25f)
        close()
        // Subpath 2 (hole)
        moveTo(8.75f, 8.0f)
        curveTo(8.75f, 6.20507f, 10.2051f, 4.75f, 12.0f, 4.75f)
        curveTo(13.7949f, 4.75f, 15.25f, 6.20507f, 15.25f, 8.0f)
        curveTo(15.25f, 9.79493f, 13.7949f, 11.25f, 12.0f, 11.25f)
        curveTo(10.2051f, 11.25f, 8.75f, 9.79493f, 8.75f, 8.0f)
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
        pathFillType = PathFillType.NonZero,
    ) {
        moveTo(4.97846f, 21.1869f)
        curveTo(5.78333f, 18.0598f, 8.62292f, 15.75f, 12.0001f, 15.75f)
        curveTo(15.3773f, 15.75f, 18.2169f, 18.0598f, 19.0217f, 21.1869f)
        curveTo(19.125f, 21.5881f, 19.5339f, 21.8296f, 19.935f, 21.7263f)
        curveTo(20.3361f, 21.6231f, 20.5776f, 21.2142f, 20.4744f, 20.8131f)
        curveTo(19.5031f, 17.0393f, 16.0783f, 14.25f, 12.0001f, 14.25f)
        curveTo(7.92192f, 14.25f, 4.49711f, 17.0393f, 3.52581f, 20.8131f)
        curveTo(3.42256f, 21.2142f, 3.66405f, 21.6231f, 4.06519f, 21.7263f)
        curveTo(4.46633f, 21.8296f, 4.87522f, 21.5881f, 4.97846f, 21.1869f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineUserImageVectorPreview() {
    Icon(
        createMediumThinOutlineUserImageVector(),
        contentDescription = "User"
    )
}
