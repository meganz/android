package mega.privacy.android.shared.original.core.ui.theme.shape

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Creates a [RoundedCornerShapeWithOffset] with the specified corner sizes and offset.
 * This can be used to apply status bar insets padding to bottom sheet layout
 *
 * @param offset The offset to apply to the shape.
 * @param topStart The size of the top start corner.
 * @param topEnd The size of the top end corner.
 * @param bottomEnd The size of the bottom end corner.
 * @param bottomStart The size of the bottom start corner.
 */
fun RoundedCornerShapeWithOffset(
    offset: Offset,
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
) = RoundedCornerShapeWithOffset(
    offset = offset,
    topStart = CornerSize(topStart),
    topEnd = CornerSize(topEnd),
    bottomEnd = CornerSize(bottomEnd),
    bottomStart = CornerSize(bottomStart)
)

/**
 * A shape that represents a rounded rectangle with an offset.
 *
 * @param offset The offset to apply to the shape.
 * @param topStart The size of the top start corner.
 * @param topEnd The size of the top end corner.
 * @param bottomEnd The size of the bottom end corner.
 * @param bottomStart The size of the bottom start corner.
 */
class RoundedCornerShapeWithOffset(
    val offset: Offset,
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
) : CornerBasedShape(
    topStart,
    topEnd,
    bottomEnd,
    bottomStart
) {
    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ) = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart
    )

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline = if (topStart + topEnd + bottomEnd + bottomStart == 0.0f) {
        Outline.Rectangle(size.toRect())
    } else {
        Outline.Rounded(
            RoundRect(
                rect = Rect(offset, size),
                topLeft = CornerRadius(if (layoutDirection == LayoutDirection.Ltr) topStart else topEnd),
                topRight = CornerRadius(if (layoutDirection == LayoutDirection.Ltr) topEnd else topStart),
                bottomRight = CornerRadius(if (layoutDirection == LayoutDirection.Ltr) bottomEnd else bottomStart),
                bottomLeft = CornerRadius(if (layoutDirection == LayoutDirection.Ltr) bottomStart else bottomEnd)
            )
        )
    }
}