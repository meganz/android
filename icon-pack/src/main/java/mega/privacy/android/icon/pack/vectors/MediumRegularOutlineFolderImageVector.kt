//
// Generated automatically by IndividualFileGenerator.
// Do not modify this file manually.
//
// Icon: folder
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

public fun createMediumRegularOutlineFolderImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_folder_medium_regular_outline",
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
        moveTo(4.0f, 2.0f)
        curveTo(2.34315f, 2.0f, 1.0f, 3.34315f, 1.0f, 5.0f)
        lineTo(1.0f, 19.0f)
        curveTo(1.0f, 20.6569f, 2.34315f, 22.0f, 4.0f, 22.0f)
        lineTo(20.0f, 22.0f)
        curveTo(21.6569f, 22.0f, 23.0f, 20.6569f, 23.0f, 19.0f)
        lineTo(23.0f, 7.0f)
        curveTo(23.0f, 5.34315f, 21.6569f, 4.0f, 20.0f, 4.0f)
        lineTo(13.8284f, 4.0f)
        curveTo(13.298f, 4.0f, 12.7893f, 3.78929f, 12.4142f, 3.41421f)
        lineTo(11.5858f, 2.58579f)
        curveTo(11.2107f, 2.21071f, 10.702f, 2.0f, 10.1716f, 2.0f)
        lineTo(4.0f, 2.0f)
        close()
        // Subpath 2 (hole)
        moveTo(4.0f, 6.0f)
        curveTo(3.44772f, 6.0f, 3.0f, 6.44772f, 3.0f, 7.0f)
        lineTo(3.0f, 19.0f)
        curveTo(3.0f, 19.5523f, 3.44772f, 20.0f, 4.0f, 20.0f)
        lineTo(20.0f, 20.0f)
        curveTo(20.5523f, 20.0f, 21.0f, 19.5523f, 21.0f, 19.0f)
        lineTo(21.0f, 7.0f)
        curveTo(21.0f, 6.44772f, 20.5523f, 6.0f, 20.0f, 6.0f)
        lineTo(4.0f, 6.0f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumRegularOutlineFolderImageVectorPreview() {
    Icon(
        createMediumRegularOutlineFolderImageVector(),
        contentDescription = "Folder"
    )
}
