package mega.privacy.android.feature.mediaplayer.presentation

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Fraction of the container height the user must drag down before the release collapses the
 * player. The value matches the positional threshold of the Material bottom sheet, which is the
 * nearest drag-to-dismiss surface in the app.
 */
internal const val COLLAPSE_DISTANCE_FRACTION = 0.25f

/**
 * Downward speed at release that collapses the player even when the drag did not pass
 * [COLLAPSE_DISTANCE_FRACTION], so that a short flick works.
 */
private val COLLAPSE_VELOCITY_DP_PER_SECOND = 400.dp

/**
 * Moves the content down with the user's finger and collapses it when the drag goes far enough.
 *
 * The content follows the drag only downwards, because the player is already at the top of the
 * screen and must not be draggable above it. On release, [onCollapse] runs when the drag passed
 * [COLLAPSE_DISTANCE_FRACTION] of [containerHeight] or the downward speed passed
 * [COLLAPSE_VELOCITY_DP_PER_SECOND]; otherwise the content animates back to its original position.
 *
 * [onCollapse] must remove the content from the composition, which is why the offset is kept at
 * its released value: the content keeps the position the user dragged it to while the host plays
 * its exit transition.
 *
 * The content draws no background of its own behind the area it leaves, so the screen below it
 * must stay in the composition for the drag to uncover it.
 *
 * The gesture reads vertical drags only, so a horizontal drag on the seek bar is not affected.
 * The seek bar also consumes its own pointer events before this modifier sees them.
 *
 * @param enabled false stops the gesture and resets the offset, for example while the play queue
 * covers the player.
 * @param containerHeight Height of the area the content is dragged in.
 * @param onCollapse Called once the released drag passed a threshold.
 */
@Composable
internal fun Modifier.dragDownToCollapse(
    enabled: Boolean,
    containerHeight: Dp,
    onCollapse: () -> Unit,
): Modifier {
    val density = LocalDensity.current
    val collapseDistancePx = with(density) {
        (containerHeight * COLLAPSE_DISTANCE_FRACTION).toPx()
    }
    val collapseVelocityPxPerSecond = with(density) { COLLAPSE_VELOCITY_DP_PER_SECOND.toPx() }
    var offsetPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(enabled) {
        if (!enabled) offsetPx = 0f
    }

    val draggableState = rememberDraggableState { delta ->
        offsetPx = (offsetPx + delta).coerceAtLeast(0f)
    }

    return this
        .graphicsLayer { translationY = offsetPx }
        .draggable(
            state = draggableState,
            orientation = Orientation.Vertical,
            enabled = enabled,
            onDragStopped = { velocity ->
                if (offsetPx >= collapseDistancePx || velocity >= collapseVelocityPxPerSecond) {
                    onCollapse()
                } else {
                    animate(initialValue = offsetPx, targetValue = 0f) { value, _ ->
                        offsetPx = value
                    }
                }
            },
        )
}
