package mega.privacy.android.feature.videoeditor.components

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Pure geometry helpers for the free-form crop overlay. Separated from the
 * Composable so they can be unit-tested without a Compose runtime.
 */

internal enum class CropHandle { TL, TR, BL, BR }

internal data class VideoBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

/**
 * Smallest video scale that keeps the crop frame inside the inset region on
 * BOTH axes when the video is centered at canvas-center. With an asymmetric
 * bottom inset > top inset, the binding vertical constraint is the bottom
 * one, so we use `bottomInsetPx` for both sides of the vertical fit.
 */
internal fun fitScale(
    canvas: Size,
    bounds: VideoBounds,
    sideInsetPx: Float,
    bottomInsetPx: Float,
): Float {
    if (bounds.width <= 0f || bounds.height <= 0f) return 1f
    val widthFitScale = (canvas.width - 2f * sideInsetPx) / bounds.width
    val heightFitScale = (canvas.height - 2f * bottomInsetPx) / bounds.height
    return minOf(widthFitScale, heightFitScale).coerceIn(0.3f, 1f)
}

/** Compute the source-video's letterboxed rect inside `canvas` at the source size. */
internal fun videoBounds(canvas: Size, sourceWidth: Int, sourceHeight: Int): VideoBounds {
    if (canvas.width <= 0f || canvas.height <= 0f || sourceWidth <= 0 || sourceHeight <= 0) {
        return VideoBounds(0f, 0f, 0f, 0f)
    }
    val sourceRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
    val canvasRatio = canvas.width / canvas.height
    return if (sourceRatio > canvasRatio) {
        // Source is wider than the canvas → letterbox (bars top & bottom).
        val fittedHeight = canvas.width / sourceRatio
        VideoBounds(
            left = 0f,
            top = (canvas.height - fittedHeight) / 2f,
            width = canvas.width,
            height = fittedHeight,
        )
    } else {
        // Source is taller than the canvas → pillarbox (bars left & right).
        val fittedWidth = canvas.height * sourceRatio
        VideoBounds(
            left = (canvas.width - fittedWidth) / 2f,
            top = 0f,
            width = fittedWidth,
            height = canvas.height,
        )
    }
}

/** Map a crop rect (normalised source coords) to screen-space, given current pan/scale. */
internal fun cropToScreen(rect: RectF, bounds: VideoBounds, pan: Offset, scale: Float): RectF {
    val left = bounds.left + pan.x + (rect.left - 0.5f) * bounds.width * scale + 0.5f * bounds.width
    val right = bounds.left + pan.x + (rect.right - 0.5f) * bounds.width * scale + 0.5f * bounds.width
    val top = bounds.top + pan.y + (rect.top - 0.5f) * bounds.height * scale + 0.5f * bounds.height
    val bottom = bounds.top + pan.y + (rect.bottom - 0.5f) * bounds.height * scale + 0.5f * bounds.height
    return RectF(left, top, right, bottom)
}

/** Inverse of [cropToScreen] for a single screen point; returns normalised source coords. */
internal fun screenToSrc(
    point: Offset,
    bounds: VideoBounds,
    pan: Offset,
    scale: Float,
): Pair<Float, Float> {
    val sourceX = (point.x - bounds.left - pan.x - 0.5f * bounds.width) / (bounds.width * scale) + 0.5f
    val sourceY = (point.y - bounds.top - pan.y - 0.5f * bounds.height) / (bounds.height * scale) + 0.5f
    return sourceX to sourceY
}

internal fun rectContains(rect: RectF, point: Offset): Boolean =
    point.x >= rect.left && point.x <= rect.right && point.y >= rect.top && point.y <= rect.bottom

/** Pick the nearest corner handle within `radius`, or null if none in range. */
internal fun nearestHandle(position: Offset, cropScreen: RectF, radius: Float): CropHandle? {
    var nearest: CropHandle? = null
    var nearestDistance = radius
    for (handle in CropHandle.entries) {
        val handlePoint = when (handle) {
            CropHandle.TL -> Offset(cropScreen.left, cropScreen.top)
            CropHandle.TR -> Offset(cropScreen.right, cropScreen.top)
            CropHandle.BL -> Offset(cropScreen.left, cropScreen.bottom)
            CropHandle.BR -> Offset(cropScreen.right, cropScreen.bottom)
        }
        val distance = hypot(position.x - handlePoint.x, position.y - handlePoint.y)
        if (distance < nearestDistance) {
            nearestDistance = distance
            nearest = handle
        }
    }
    return nearest
}

/**
 * Resize a crop rect under a fixed aspect lock. Projects the dragged point
 * ([dragX], [dragY]) onto the constant-aspect line anchored at the opposite
 * corner, then clamps size to `[minSide, …]` and position to the supplied
 * source-coord bounds (default `[0, 1]`).
 *
 * The bounds are caller-computed: pass the source coords of the on-screen inset
 * region (via [screenToSrc] on the inset corners) so the projection can't push
 * the dragged corner past the inset after the user has zoomed in.
 */
internal fun resizeWithAspectLock(
    handle: CropHandle,
    dragX: Float,
    dragY: Float,
    currentRect: RectF,
    aspect: Float,
    minSide: Float,
    minX: Float = 0f,
    maxX: Float = 1f,
    minY: Float = 0f,
    maxY: Float = 1f,
): RectF {
    val rect = RectF(currentRect)
    val anchorX: Float = when (handle) {
        CropHandle.TL, CropHandle.BL -> rect.right
        CropHandle.TR, CropHandle.BR -> rect.left
    }
    val anchorY: Float = when (handle) {
        CropHandle.TL, CropHandle.TR -> rect.bottom
        CropHandle.BL, CropHandle.BR -> rect.top
    }
    val draggedWidth = abs(dragX - anchorX)
    val draggedHeight = abs(dragY - anchorY)
    val projectedHeight = (aspect * draggedWidth + draggedHeight) / (aspect * aspect + 1f)
    val maxHeightByX = when (handle) {
        CropHandle.TL, CropHandle.BL -> (anchorX - minX) / aspect
        CropHandle.TR, CropHandle.BR -> (maxX - anchorX) / aspect
    }
    val maxHeightByY = when (handle) {
        CropHandle.TL, CropHandle.TR -> anchorY - minY
        CropHandle.BL, CropHandle.BR -> maxY - anchorY
    }
    val height = projectedHeight.coerceIn(minSide, max(minSide, min(maxHeightByX, maxHeightByY)))
    val width = height * aspect
    when (handle) {
        CropHandle.TL -> {
            rect.left = anchorX - width
            rect.top = anchorY - height
        }

        CropHandle.TR -> {
            rect.right = anchorX + width
            rect.top = anchorY - height
        }

        CropHandle.BL -> {
            rect.left = anchorX - width
            rect.bottom = anchorY + height
        }

        CropHandle.BR -> {
            rect.right = anchorX + width
            rect.bottom = anchorY + height
        }
    }
    return rect
}

/** Resize a crop rect freely (no aspect lock), moving the dragged corner. */
internal fun resizeFree(
    handle: CropHandle,
    dragX: Float,
    dragY: Float,
    currentRect: RectF,
    minSide: Float,
): RectF {
    val rect = RectF(currentRect)
    when (handle) {
        CropHandle.TL -> {
            rect.left = dragX.coerceAtMost(rect.right - minSide)
            rect.top = dragY.coerceAtMost(rect.bottom - minSide)
        }

        CropHandle.TR -> {
            rect.right = dragX.coerceAtLeast(rect.left + minSide)
            rect.top = dragY.coerceAtMost(rect.bottom - minSide)
        }

        CropHandle.BL -> {
            rect.left = dragX.coerceAtMost(rect.right - minSide)
            rect.bottom = dragY.coerceAtLeast(rect.top + minSide)
        }

        CropHandle.BR -> {
            rect.right = dragX.coerceAtLeast(rect.left + minSide)
            rect.bottom = dragY.coerceAtLeast(rect.top + minSide)
        }
    }
    return rect
}

internal data class VideoTransformResult(
    val pan: Offset,
    val scale: Float,
    val cropRect: RectF,
)

/**
 * Apply a pan+pinch delta to the video transform while keeping the crop rect
 * inside `[0,1]`. Centroid-zoom and pan are clamped together so the cropRect
 * stays valid; this is the function called every frame of a pinch gesture.
 */
internal fun applyVideoTransform(
    cropRect: RectF,
    videoPan: Offset,
    videoScale: Float,
    panDelta: Offset,
    zoom: Float,
    centroid: Offset,
    bounds: VideoBounds,
    minSide: Float,
    minScale: Float = 1f,
): VideoTransformResult {
    var panX = videoPan.x
    var panY = videoPan.y
    var scale = videoScale
    var newCrop = RectF(cropRect)

    // ─── Pan (clamped so cropRect stays inside [0, 1]) ───
    val intendedDeltaX = -panDelta.x / (bounds.width * scale)
    val intendedDeltaY = -panDelta.y / (bounds.height * scale)
    val allowedDeltaX = intendedDeltaX.coerceIn(-newCrop.left, 1f - newCrop.right)
    val allowedDeltaY = intendedDeltaY.coerceIn(-newCrop.top, 1f - newCrop.bottom)

    panX += -allowedDeltaX * bounds.width * scale
    panY += -allowedDeltaY * bounds.height * scale
    newCrop.offset(allowedDeltaX, allowedDeltaY)

    // ─── Zoom around centroid ───
    if (zoom != 1f && zoom > 0f) {
        val targetScale = (scale * zoom).coerceIn(minScale, 5f)
        val actualZoom = targetScale / scale
        if (actualZoom != 1f) {
            val centroidSourceX =
                (centroid.x - bounds.left - panX - 0.5f * bounds.width) / (bounds.width * scale) + 0.5f
            val centroidSourceY =
                (centroid.y - bounds.top - panY - 0.5f * bounds.height) / (bounds.height * scale) + 0.5f

            scale = targetScale
            panX = centroid.x - bounds.left - (centroidSourceX - 0.5f) * bounds.width * scale - 0.5f * bounds.width
            panY = centroid.y - bounds.top - (centroidSourceY - 0.5f) * bounds.height * scale - 0.5f * bounds.height

            newCrop = RectF(
                (newCrop.left - centroidSourceX) / actualZoom + centroidSourceX,
                (newCrop.top - centroidSourceY) / actualZoom + centroidSourceY,
                (newCrop.right - centroidSourceX) / actualZoom + centroidSourceX,
                (newCrop.bottom - centroidSourceY) / actualZoom + centroidSourceY,
            )

            // After zoom-out, cropRect can drift past [0,1]; clamp position and
            // compensate pan so the on-screen frame stays fixed.
            if (newCrop.left < 0f) {
                val shift = -newCrop.left
                newCrop.right = (newCrop.right + shift).coerceAtMost(1f)
                newCrop.left = 0f
                panX -= shift * bounds.width * scale
            }
            if (newCrop.right > 1f) {
                val shift = newCrop.right - 1f
                newCrop.left = (newCrop.left - shift).coerceAtLeast(0f)
                newCrop.right = 1f
                panX += shift * bounds.width * scale
            }
            if (newCrop.top < 0f) {
                val shift = -newCrop.top
                newCrop.bottom = (newCrop.bottom + shift).coerceAtMost(1f)
                newCrop.top = 0f
                panY -= shift * bounds.height * scale
            }
            if (newCrop.bottom > 1f) {
                val shift = newCrop.bottom - 1f
                newCrop.top = (newCrop.top - shift).coerceAtLeast(0f)
                newCrop.bottom = 1f
                panY += shift * bounds.height * scale
            }
        }
    }

    // ─── Size guard (min only) ───
    if (newCrop.width() < minSide) {
        val center = (newCrop.left + newCrop.right) / 2f
        newCrop.left = (center - minSide / 2f).coerceAtLeast(0f)
        newCrop.right = (center + minSide / 2f).coerceAtMost(1f)
    }
    if (newCrop.height() < minSide) {
        val center = (newCrop.top + newCrop.bottom) / 2f
        newCrop.top = (center - minSide / 2f).coerceAtLeast(0f)
        newCrop.bottom = (center + minSide / 2f).coerceAtMost(1f)
    }

    return VideoTransformResult(Offset(panX, panY), scale, newCrop)
}
