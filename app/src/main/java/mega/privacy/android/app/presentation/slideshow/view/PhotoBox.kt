package mega.privacy.android.app.presentation.slideshow.view

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

@Composable
fun PhotoBox(
    modifier: Modifier = Modifier,
    state: PhotoState = rememberPhotoState(),
    enabled: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    propagateMinConstraints: Boolean = false,
    onTap: ((Offset) -> Unit),
    content: @Composable BoxScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .onSizeChanged { state.layoutSize = it.toSize() }
            .pointerDragInputs(
                enabled = enabled && state.isScaled,
                onDrag = { dragAmount ->
                    state.currentOffset += dragAmount
                },
                onDragStopped = { velocity ->
                    coroutineScope.launch {
                        state.performFling(Offset(velocity.x, velocity.y))
                    }
                },
            )
            .pointerTapInputs(
                enabled = enabled,
                onDoubleTap = {
                    if (state.isScaled) {
                        coroutineScope.launch {
                            state.animateToInitialState()
                        }
                    } else {
                        coroutineScope.launch {
                            state.animateScale(state.maximumScale)
                        }
                    }
                },
                onTap = onTap
            )
            .clipToBounds()
            .graphicsLayer {
                scaleX = state.currentScale
                scaleY = state.currentScale
                translationX = state.currentOffset.x
                translationY = state.currentOffset.y
            }
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGestureStart = { },
                    onGesture = { _, pan, zoom, _ ->
                        if (zoom != state.currentScale) {
                            state.currentScale *= zoom
                            state.currentOffset += pan
                            return@detectTransformGestures true
                        }
                        return@detectTransformGestures false
                    },
                    onGestureEnd = { },
                    enableOneFingerZoom = false,
                )
            },
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints,
        content = content,
    )
}

private fun Modifier.pointerDragInputs(
    enabled: Boolean,
    onDrag: (dragAmount: Offset) -> Unit,
    onDragStopped: (velocity: Velocity) -> Unit,
): Modifier {
    val velocityTracker = VelocityTracker()
    return pointerInput(enabled) {
        if (enabled) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    velocityTracker.addPointerInputChange(change)
                    onDrag(dragAmount)
                },
                onDragEnd = {
                    val velocity = velocityTracker.calculateVelocity()
                    onDragStopped(velocity)
                },
                onDragCancel = {
                    val velocity = velocityTracker.calculateVelocity()
                    onDragStopped(velocity)
                },
            )
        }
    }
}

private fun Modifier.pointerTapInputs(
    enabled: Boolean,
    onDoubleTap: (position: Offset) -> Unit,
    onTap: ((Offset) -> Unit),
): Modifier {
    if (enabled.not()) {
        return this
    }
    return pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = onDoubleTap,
            onTap = onTap
        )
    }
}

