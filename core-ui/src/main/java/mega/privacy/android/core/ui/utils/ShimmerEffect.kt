package mega.privacy.android.core.ui.utils

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Shimmer effect for a view
 * @param visible [Boolean] if the shimmer effect is visible, default is true
 * @param shape [Shape] radius of the corners, default is 8.dp
 */
fun Modifier.shimmerEffect(
    visible: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
): Modifier = composed {
    if (!visible) return@composed this

    var size by remember {
        mutableStateOf(IntSize.Zero)
    }
    val transition = rememberInfiniteTransition(label = "InfiniteTransition")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            )
        ),
        label = "Shimmering Animation"
    )
    val colors = listOf(
        MegaTheme.colors.background.surface2,
        MegaTheme.colors.background.surface1,
        MegaTheme.colors.background.surface2,
    )
    val gradientWidth = size.width.toFloat()
    drawWithCache {
        val offsetX = startOffsetX
        val brush = Brush.linearGradient(
            colors = colors,
            start = Offset(offsetX, 0f),
            end = Offset(offsetX + gradientWidth, 0f)
        )
        val outline = shape.createOutline(size.toSize(), layoutDirection, this)
        onDrawWithContent {
            drawOutline(outline, brush)
        }
    }.onGloballyPositioned {
        size = it.size
    }
}
