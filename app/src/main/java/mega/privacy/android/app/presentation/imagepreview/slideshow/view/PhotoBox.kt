package mega.privacy.android.app.presentation.imagepreview.slideshow.view

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.magnifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

@Composable
fun PhotoBox(
    modifier: Modifier = Modifier,
    state: PhotoState = rememberPhotoState(),
    enableZoom: Boolean = true,
    enableTap: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    propagateMinConstraints: Boolean = false,
    isMagnifierMode: Boolean = false,
    onTap: ((Offset) -> Unit) = {},
    onDragMagnifier: (Boolean) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var offsetMagnifier by remember { mutableStateOf(Offset.Unspecified) }
    var isDraggingMagnifier by remember { mutableStateOf(false) }

    Box(
        modifier = if (!isMagnifierMode) {
            modifier
                .onSizeChanged { state.layoutSize = it.toSize() }
                .pointerDragInputs(
                    enabled = enableZoom && state.isScaled,
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
                    enabled = enableTap,
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
                    if (enableZoom) {
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
                    }
                }
        } else {
            modifier
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            onDragMagnifier(false)
                            isDraggingMagnifier = false
                        },
                        onDragStart = {
                            onDragMagnifier(true)
                            isDraggingMagnifier = true
                        },
                        onDrag = { change, _ ->
                            offsetMagnifier = change.position
                        }
                    )
                }
                .then(
                    if (offsetMagnifier == Offset.Unspecified || !isDraggingMagnifier) {
                        Modifier
                    } else {
                        Modifier.magnifier(
                            sourceCenter = { offsetMagnifier },
                            magnifierCenter = { offsetMagnifier },
                            zoom = 3.0f,
                            size = DpSize(164.dp, 164.dp),
                            clip = true,
                        )
                    }
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

