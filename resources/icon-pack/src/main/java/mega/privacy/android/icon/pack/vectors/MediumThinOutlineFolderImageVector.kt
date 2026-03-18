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

public fun createMediumThinOutlineFolderImageVector(): ImageVector = ImageVector.Builder(
    name = "icon_folder_medium_thin_outline",
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
        moveTo(1.25f, 5.0f)
        curveTo(1.25f, 3.48122f, 2.48122f, 2.25f, 4.0f, 2.25f)
        lineTo(10.1716f, 2.25f)
        curveTo(10.6357f, 2.25f, 11.0808f, 2.43437f, 11.409f, 2.76256f)
        lineTo(12.2374f, 3.59099f)
        curveTo(12.6594f, 4.01295f, 13.2317f, 4.25f, 13.8284f, 4.25f)
        lineTo(20.0f, 4.25f)
        curveTo(21.5188f, 4.25f, 22.75f, 5.48122f, 22.75f, 7.0f)
        lineTo(22.75f, 19.0f)
        curveTo(22.75f, 20.5188f, 21.5188f, 21.75f, 20.0f, 21.75f)
        lineTo(4.0f, 21.75f)
        curveTo(2.48122f, 21.75f, 1.25f, 20.5188f, 1.25f, 19.0f)
        lineTo(1.25f, 5.0f)
        close()
        // Subpath 2 (hole)
        moveTo(4.0f, 5.75f)
        curveTo(3.30964f, 5.75f, 2.75f, 6.30964f, 2.75f, 7.0f)
        lineTo(2.75f, 19.0f)
        curveTo(2.75f, 19.6904f, 3.30964f, 20.25f, 4.0f, 20.25f)
        lineTo(20.0f, 20.25f)
        curveTo(20.6904f, 20.25f, 21.25f, 19.6904f, 21.25f, 19.0f)
        lineTo(21.25f, 7.0f)
        curveTo(21.25f, 6.30964f, 20.6904f, 5.75f, 20.0f, 5.75f)
        lineTo(4.0f, 5.75f)
        close()
    }
}.build()

@Preview
@Composable
private fun MediumThinOutlineFolderImageVectorPreview() {
    Icon(
        createMediumThinOutlineFolderImageVector(),
        contentDescription = "Folder"
    )
}
