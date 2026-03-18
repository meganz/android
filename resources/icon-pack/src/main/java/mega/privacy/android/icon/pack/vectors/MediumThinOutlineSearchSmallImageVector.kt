//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: search_small
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

public fun createMediumThinOutlineSearchSmallImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_search-small_medium_thin_outline",
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
        moveTo(10.0f, 3.75f)
        curveTo(6.54822f, 3.75f, 3.75f, 6.54822f, 3.75f, 10.0f)
        curveTo(3.75f, 13.4518f, 6.54822f, 16.25f, 10.0f, 16.25f)
        curveTo(13.4518f, 16.25f, 16.25f, 13.4518f, 16.25f, 10.0f)
        curveTo(16.25f, 6.54822f, 13.4518f, 3.75f, 10.0f, 3.75f)
        close()
        // Subpath 2 (hole)
        moveTo(2.25f, 10.0f)
        curveTo(2.25f, 5.71979f, 5.71979f, 2.25f, 10.0f, 2.25f)
        curveTo(14.2802f, 2.25f, 17.75f, 5.71979f, 17.75f, 10.0f)
        curveTo(17.75f, 11.87f, 17.0877f, 13.5853f, 15.9849f, 14.9241f)
        lineTo(21.5303f, 20.4697f)
        curveTo(21.8232f, 20.7626f, 21.8232f, 21.2374f, 21.5303f, 21.5303f)
        curveTo(21.2374f, 21.8232f, 20.7626f, 21.8232f, 20.4697f, 21.5303f)
        lineTo(14.9242f, 15.9848f)
        curveTo(13.5854f, 17.0877f, 11.87f, 17.75f, 10.0f, 17.75f)
        curveTo(5.71979f, 17.75f, 2.25f, 14.2802f, 2.25f, 10.0f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineSearchSmallImageVectorPreview() {
    Icon(
        createMediumThinOutlineSearchSmallImageVector(),
        contentDescription = "SearchSmall"
    )
}
