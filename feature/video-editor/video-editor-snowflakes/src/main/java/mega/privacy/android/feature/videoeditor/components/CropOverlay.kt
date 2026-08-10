package mega.privacy.android.feature.videoeditor.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens
import kotlin.math.hypot

/**
 * Free-form crop overlay: a scrim + draggable corners + pan/pinch on the video.
 *
 * Gesture flow:
 * - Touch starts on a corner → drag resizes (constrained by aspect lock if set).
 * - Touch starts inside the crop frame → pan+pinch the underlying video.
 * - Touch outside → no-op.
 *
 * The overlay fills the canvas-sized parent. cropRect / videoPan / videoScale
 * are hoisted; this composable just renders and reports gestures. `presetKey` is
 * an opaque key (the host's selected preset) that, when it changes, snaps the
 * video transform back to "fit the inset".
 */
@Composable
fun FreeFormCropOverlay(
    cropRect: RectF,
    sourceWidth: Int,
    sourceHeight: Int,
    videoPan: Offset,
    videoScale: Float,
    aspectLock: Float?,
    presetKey: Any?,
    onCornerResize: (RectF) -> Unit,
    onPanPinch: (pan: Offset, scale: Float, cropRect: RectF) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cropRectState = rememberUpdatedState(cropRect)
    val videoPanState = rememberUpdatedState(videoPan)
    val videoScaleState = rememberUpdatedState(videoScale)
    val onResizeState = rememberUpdatedState(onCornerResize)
    val onPanPinchState = rememberUpdatedState(onPanPinch)
    val aspectLockState = rememberUpdatedState(aspectLock)
    val canvasSize = remember { mutableStateOf(Size.Zero) }

    val handleRadiusPx = 80f
    val minSide = 0.1f
    val density = LocalDensity.current
    val sideInsetPx = with(density) { 30.dp.toPx() }
    val bottomInsetPx = with(density) { 80.dp.toPx() }

    // Snap the video transform back to "fit the inset" whenever the preset key
    // (or source / canvas size) changes — cropRect itself is left alone, only
    // pan/scale reset, so switching presets after pinching in doesn't carry
    // over the zoom.
    LaunchedEffect(presetKey, canvasSize.value.width, sourceWidth, sourceHeight) {
        if (canvasSize.value.width <= 0f) return@LaunchedEffect
        val vb = videoBounds(canvasSize.value, sourceWidth, sourceHeight)
        if (vb.width <= 0f || vb.height <= 0f) return@LaunchedEffect
        val fitScale = fitScale(canvasSize.value, vb, sideInsetPx, bottomInsetPx)
        onPanPinchState.value(Offset.Zero, fitScale, cropRectState.value)
    }

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize.value = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(sourceWidth, sourceHeight) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val vb = videoBounds(canvasSize.value, sourceWidth, sourceHeight)
                    if (vb.width <= 0f || vb.height <= 0f) return@awaitEachGesture

                    val cropScreen = cropToScreen(
                        cropRectState.value, vb, videoPanState.value, videoScaleState.value,
                    )
                    val handle = nearestHandle(down.position, cropScreen, handleRadiusPx)

                    if (handle != null) {
                        // Corner-resize: clamp dragged screen position to inset.
                        var change = down
                        while (change.pressed) {
                            val maxX = (canvasSize.value.width - sideInsetPx)
                                .coerceAtLeast(sideInsetPx)
                            val maxY = (canvasSize.value.height - bottomInsetPx)
                                .coerceAtLeast(sideInsetPx)
                            val clampedX = change.position.x.coerceIn(sideInsetPx, maxX)
                            val clampedY = change.position.y.coerceIn(sideInsetPx, maxY)
                            val (sx, sy) = screenToSrc(
                                Offset(clampedX, clampedY), vb,
                                videoPanState.value, videoScaleState.value,
                            )
                            val (minXSrcRaw, minYSrcRaw) = screenToSrc(
                                Offset(sideInsetPx, sideInsetPx), vb,
                                videoPanState.value, videoScaleState.value,
                            )
                            val (maxXSrcRaw, maxYSrcRaw) = screenToSrc(
                                Offset(maxX, maxY), vb,
                                videoPanState.value, videoScaleState.value,
                            )
                            val minXSrc = minXSrcRaw.coerceIn(0f, 1f)
                            val maxXSrc = maxXSrcRaw.coerceIn(0f, 1f)
                            val minYSrc = minYSrcRaw.coerceIn(0f, 1f)
                            val maxYSrc = maxYSrcRaw.coerceIn(0f, 1f)
                            val nx = sx.coerceIn(minXSrc, maxXSrc)
                            val ny = sy.coerceIn(minYSrc, maxYSrc)
                            val currentAspectLock = aspectLockState.value
                            val r = if (currentAspectLock != null) {
                                resizeWithAspectLock(
                                    handle, nx, ny, cropRectState.value,
                                    currentAspectLock, minSide,
                                    minXSrc, maxXSrc, minYSrc, maxYSrc,
                                )
                            } else {
                                resizeFree(handle, nx, ny, cropRectState.value, minSide)
                            }
                            onResizeState.value(r)
                            change.consume()
                            val event = awaitPointerEvent()
                            change = event.changes.firstOrNull { it.id == down.id } ?: break
                        }
                        return@awaitEachGesture
                    }

                    // Pan / pinch on the VIDEO, keeping the crop frame fixed.
                    if (!rectContains(cropScreen, down.position)) return@awaitEachGesture

                    var prevCentroid = down.position
                    var prevDistance = 0f
                    var prevPointerCount = 1

                    while (true) {
                        val event = awaitPointerEvent()
                        val active = event.changes.filter { it.pressed }
                        if (active.isEmpty()) break

                        val centroid = active
                            .map { it.position }
                            .reduce { a, b -> Offset(a.x + b.x, a.y + b.y) } / active.size.toFloat()

                        val distance = if (active.size >= 2) {
                            val a = active[0].position
                            val b = active[1].position
                            hypot(b.x - a.x, b.y - a.y)
                        } else 0f

                        val pointerCountChanged = active.size != prevPointerCount
                        val panDelta = if (pointerCountChanged) {
                            Offset.Zero
                        } else {
                            Offset(centroid.x - prevCentroid.x, centroid.y - prevCentroid.y)
                        }
                        val zoom = if (
                            !pointerCountChanged && prevDistance > 0f && distance > 0f
                        ) {
                            (distance / prevDistance).coerceIn(0.5f, 2f)
                        } else 1f

                        if (!pointerCountChanged) {
                            val minScale = fitScale(
                                canvasSize.value, vb, sideInsetPx, bottomInsetPx,
                            )
                            val result = applyVideoTransform(
                                cropRect = cropRectState.value,
                                videoPan = videoPanState.value,
                                videoScale = videoScaleState.value,
                                panDelta = panDelta,
                                zoom = zoom,
                                centroid = centroid,
                                bounds = vb,
                                minSide = minSide,
                                minScale = minScale,
                            )
                            onPanPinchState.value(result.pan, result.scale, result.cropRect)
                        }

                        prevCentroid = centroid
                        prevDistance = distance
                        prevPointerCount = active.size
                        active.forEach { it.consume() }
                    }
                }
            },
    ) {
        val handleColor = DSTokens.colors.brand.default
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize.value = size
            val vb = videoBounds(size, sourceWidth, sourceHeight)
            val cropScreen = cropToScreen(
                cropRectState.value, vb, videoPanState.value, videoScaleState.value,
            )
            drawCropOverlay(cropScreen, size, drawHandles = true, handleColor = handleColor)
        }
    }
}

private fun DrawScope.drawCropOverlay(
    cropScreen: RectF,
    canvasSize: Size,
    drawHandles: Boolean,
    handleColor: Color,
) {
    val scrim = Color.Black.copy(alpha = 0.5f)
    drawRect(scrim, Offset.Zero, Size(canvasSize.width, cropScreen.top.coerceAtLeast(0f)))
    drawRect(
        scrim,
        Offset(0f, cropScreen.bottom.coerceAtMost(canvasSize.height)),
        Size(canvasSize.width, (canvasSize.height - cropScreen.bottom).coerceAtLeast(0f)),
    )
    drawRect(
        scrim,
        Offset(0f, cropScreen.top.coerceAtLeast(0f)),
        Size(
            cropScreen.left.coerceAtLeast(0f),
            (cropScreen.bottom - cropScreen.top).coerceAtLeast(0f),
        ),
    )
    drawRect(
        scrim,
        Offset(cropScreen.right.coerceAtMost(canvasSize.width), cropScreen.top.coerceAtLeast(0f)),
        Size(
            (canvasSize.width - cropScreen.right).coerceAtLeast(0f),
            (cropScreen.bottom - cropScreen.top).coerceAtLeast(0f),
        ),
    )

    drawRect(
        color = Color.White,
        topLeft = Offset(cropScreen.left, cropScreen.top),
        size = Size(cropScreen.right - cropScreen.left, cropScreen.bottom - cropScreen.top),
        style = Stroke(width = 3f),
    )
    val w = cropScreen.right - cropScreen.left
    val h = cropScreen.bottom - cropScreen.top
    for (i in 1..2) {
        drawLine(
            color = Color.White.copy(alpha = 0.45f),
            start = Offset(cropScreen.left + w * i / 3f, cropScreen.top),
            end = Offset(cropScreen.left + w * i / 3f, cropScreen.bottom),
            strokeWidth = 1.5f,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.45f),
            start = Offset(cropScreen.left, cropScreen.top + h * i / 3f),
            end = Offset(cropScreen.right, cropScreen.top + h * i / 3f),
            strokeWidth = 1.5f,
        )
    }
    if (drawHandles) {
        val cornerLen = 36f
        val cornerRadius = 12f
        val stroke = 10f
        val handleStyle = Stroke(
            width = stroke,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val corners = listOf(
            Offset(cropScreen.left, cropScreen.top) to (1f to 1f),
            Offset(cropScreen.right, cropScreen.top) to (-1f to 1f),
            Offset(cropScreen.left, cropScreen.bottom) to (1f to -1f),
            Offset(cropScreen.right, cropScreen.bottom) to (-1f to -1f),
        )
        val path = Path()
        for ((p, dir) in corners) {
            path.reset()
            path.moveTo(p.x + cornerLen * dir.first, p.y)
            path.lineTo(p.x + cornerRadius * dir.first, p.y)
            path.quadraticBezierTo(p.x, p.y, p.x, p.y + cornerRadius * dir.second)
            path.lineTo(p.x, p.y + cornerLen * dir.second)
            drawPath(path, color = handleColor, style = handleStyle)
        }
    }
}
