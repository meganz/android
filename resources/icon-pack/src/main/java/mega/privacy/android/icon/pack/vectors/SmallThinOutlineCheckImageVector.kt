//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: check
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

public fun createSmallThinOutlineCheckImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_check_small_thin_outline",
    defaultWidth = 16.0.dp,
    defaultHeight = 16.0.dp,
    viewportWidth = 16.0f,
    viewportHeight = 16.0f
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
        moveTo(12.8536f, 4.64645f)
        curveTo(13.0488f, 4.84171f, 13.0488f, 5.15829f, 12.8536f, 5.35355f)
        lineTo(6.35355f, 11.8536f)
        curveTo(6.15829f, 12.0488f, 5.84171f, 12.0488f, 5.64645f, 11.8536f)
        lineTo(3.14645f, 9.35355f)
        curveTo(2.95118f, 9.15829f, 2.95118f, 8.84171f, 3.14645f, 8.64645f)
        curveTo(3.34171f, 8.45118f, 3.65829f, 8.45118f, 3.85355f, 8.64645f)
        lineTo(6.0f, 10.7929f)
        lineTo(12.1464f, 4.64645f)
        curveTo(12.3417f, 4.45118f, 12.6583f, 4.45118f, 12.8536f, 4.64645f)
        close()
    }
}.build()

@Preview
@Composable
private fun SmallThinOutlineCheckImageVectorPreview() {
    Icon(
        createSmallThinOutlineCheckImageVector(),
        contentDescription = "Check"
    )
}
