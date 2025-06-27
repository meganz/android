package mega.privacy.android.app.camera.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
internal fun FocusRing(
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(2.0f) }
    var alpha by remember { mutableFloatStateOf(0.0f) }

    LaunchedEffect(Unit) {
        alpha = 1.0f
        scale = 1.0f
    }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(FOCUS_ANIMATION_DURATION),
        label = "focus_scale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(FOCUS_ANIMATION_DURATION),
        label = "focus_alpha"
    )

    Box(
        modifier = modifier
            .size(FOCUS_RING_SIZE.dp)
            .graphicsLayer(
                scaleX = animatedScale,
                scaleY = animatedScale,
                alpha = animatedAlpha
            )
            .background(
                color = Color.Transparent,
                shape = CircleShape
            )
            .border(
                width = 1.5.dp,
                color = Color.White,
                shape = CircleShape
            )
    )
}

internal const val FOCUS_RING_SIZE = 70
private const val FOCUS_ANIMATION_DURATION = 250
