//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: plus
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

public fun createMediumThinOutlinePlusImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_plus_medium_thin_outline",
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
        moveTo(12.75f, 5.0f)
        curveTo(12.75f, 4.58579f, 12.4142f, 4.25f, 12.0f, 4.25f)
        curveTo(11.5858f, 4.25f, 11.25f, 4.58579f, 11.25f, 5.0f)
        lineTo(11.25f, 11.25f)
        lineTo(5.0f, 11.25f)
        curveTo(4.58579f, 11.25f, 4.25f, 11.5858f, 4.25f, 12.0f)
        curveTo(4.25f, 12.4142f, 4.58579f, 12.75f, 5.0f, 12.75f)
        lineTo(11.25f, 12.75f)
        lineTo(11.25f, 19.0f)
        curveTo(11.25f, 19.4142f, 11.5858f, 19.75f, 12.0f, 19.75f)
        curveTo(12.4142f, 19.75f, 12.75f, 19.4142f, 12.75f, 19.0f)
        lineTo(12.75f, 12.75f)
        lineTo(19.0f, 12.75f)
        curveTo(19.4142f, 12.75f, 19.75f, 12.4142f, 19.75f, 12.0f)
        curveTo(19.75f, 11.5858f, 19.4142f, 11.25f, 19.0f, 11.25f)
        lineTo(12.75f, 11.25f)
        lineTo(12.75f, 5.0f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlinePlusImageVectorPreview() {
    Icon(
        createMediumThinOutlinePlusImageVector(),
        contentDescription = "Plus"
    )
}
