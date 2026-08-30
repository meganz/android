package mega.privacy.android.app.presentation.videoplayer

import kotlin.math.abs

/**
 * Geometry of the zoomable video: the FIT-displayed video rect and the screen it is
 * centered in, both in pixels.
 */
internal data class VideoZoomViewport(
    val videoWidth: Int,
    val videoHeight: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    // Minimum overflow before an axis counts as pannable. Deliberately has no default: a
    // call site that forgets it would silently fall back to plain geometry, which is the
    // 1px-overflow pan that claims the brightness/volume swipe.
    val panThresholdPx: Float,
) {
    /** The zoom level at which the FIT-displayed video exactly fills the screen. */
    val fillZoom: Float
        get() = if (videoWidth == 0 || videoHeight == 0) {
            VideoPlayerZoomState.MIN_ZOOM
        } else {
            maxOf(
                screenWidth.toFloat() / videoWidth,
                screenHeight.toFloat() / videoHeight,
            )
        }
}

/** The two zoom levels the pinch gesture snaps onto. */
internal enum class ZoomBoundary { Fit, Fill }

/**
 * Pure zoom/pan state for the video player pinch-to-zoom gestures, free of Android
 * dependencies so the clamping and fill-zoom calculations are unit-testable.
 *
 * Zooming pivots on the screen center; [translationX]/[translationY] are applied after
 * scaling. Panning is clamped per axis: an axis whose content does not overflow the
 * screen stays centered, and on an overflowing axis the video edges never move inside
 * the screen edges — so between fit and fill only the overflowing axis pans.
 *
 * While pinching, the zoom snaps onto the fit and fill levels whenever it comes within
 * [ZOOM_SNAP_RATIO] of them, so the user can lock onto either boundary easily.
 */
internal class VideoPlayerZoomState {

    var zoomLevel = MIN_ZOOM
        private set

    // Unsnapped zoom accumulated across the pinch events of the current gesture. Snapping
    // is applied on top of this raw value: if each event were re-anchored to the snapped
    // zoomLevel instead, a continuous pinch could never travel out of a snap zone.
    private var rawZoomLevel = MIN_ZOOM

    var translationX = 0f
        private set

    var translationY = 0f
        private set

    /**
     * Applies one pinch scale event, zooming continuously between min and max zoom with
     * snapping at the fit and fill levels.
     *
     * @return the boundary the zoom just arrived at (or the fill boundary a fast event
     * jumped across) with this event, or null when the zoom stayed on, or away from, a
     * boundary — i.e. non-null exactly when boundary haptic feedback should fire.
     */
    fun onPinchScale(scaleFactor: Float, viewport: VideoZoomViewport): ZoomBoundary? {
        val previousZoom = zoomLevel
        val previousBoundary = currentBoundary(viewport)
        // The fill level of an extreme-aspect-ratio video (e.g. panoramic) can exceed
        // MAX_ZOOM; raise the pinch ceiling to it so fill stays reachable and a pinch
        // after zoomToFill is not abruptly collapsed to MAX_ZOOM.
        val maxZoom = maxOf(MAX_ZOOM, viewport.fillZoom)
        rawZoomLevel = (rawZoomLevel * scaleFactor).coerceIn(MIN_ZOOM, maxZoom)
        zoomLevel = snapToBoundaries(rawZoomLevel, viewport)
        if (zoomLevel <= MIN_ZOOM + ZOOM_LEVEL_EPSILON) {
            translationX = 0f
            translationY = 0f
        } else {
            clampTranslation(viewport)
        }
        val boundary = currentBoundary(viewport)
        return when {
            boundary != null && boundary != previousBoundary -> boundary
            crossedFill(previousZoom, zoomLevel, viewport) -> ZoomBoundary.Fill
            else -> null
        }
    }

    // A fast pinch event can jump from one side of the fill level to the other without
    // landing inside the snap zone; the crossing still deserves boundary feedback.
    private fun crossedFill(from: Float, to: Float, viewport: VideoZoomViewport): Boolean {
        val fill = viewport.fillZoom
        return (from < fill && to > fill) || (from > fill && to < fill)
    }

    /** Ends the pinch gesture, anchoring the next gesture at the displayed (snapped) zoom. */
    fun onPinchEnd() {
        rawZoomLevel = zoomLevel
    }

    /** The boundary the zoom currently sits on, or null when in between or beyond fill. */
    fun currentBoundary(viewport: VideoZoomViewport): ZoomBoundary? = when {
        zoomLevel <= MIN_ZOOM + ZOOM_LEVEL_EPSILON -> ZoomBoundary.Fit
        abs(zoomLevel - viewport.fillZoom) <= ZOOM_LEVEL_EPSILON -> ZoomBoundary.Fill
        else -> null
    }

    /** Pans the zoomed video; clamped to zero on an axis it does not overflow. */
    fun onPan(deltaX: Float, deltaY: Float, viewport: VideoZoomViewport) {
        translationX += deltaX
        translationY += deltaY
        clampTranslation(viewport)
    }

    /** Zooms to the fill-screen level with the video centered. */
    fun zoomToFill(viewport: VideoZoomViewport) {
        zoomLevel = viewport.fillZoom
        rawZoomLevel = viewport.fillZoom
        translationX = 0f
        translationY = 0f
    }

    /** Returns to the FIT display. */
    fun reset() {
        zoomLevel = MIN_ZOOM
        rawZoomLevel = MIN_ZOOM
        translationX = 0f
        translationY = 0f
    }

    /** True when the video is displayed at (or beyond) the fill-screen level by zooming. */
    fun isZoomedToFill(viewport: VideoZoomViewport): Boolean =
        zoomLevel > MIN_ZOOM + ZOOM_LEVEL_EPSILON &&
                zoomLevel >= viewport.fillZoom - ZOOM_LEVEL_EPSILON

    /** True when the zoomed video overflows the fill-screen level, i.e. both axes may pan. */
    fun isZoomedBeyondFill(viewport: VideoZoomViewport): Boolean =
        zoomLevel > viewport.fillZoom + ZOOM_LEVEL_EPSILON

    /** True when the zoomed video overflows the screen horizontally, so horizontal panning applies. */
    fun canPanHorizontally(viewport: VideoZoomViewport): Boolean =
        viewport.videoWidth * zoomLevel > viewport.screenWidth + viewport.panThresholdPx

    /** True when the zoomed video overflows the screen vertically, so vertical panning applies. */
    fun canPanVertically(viewport: VideoZoomViewport): Boolean =
        viewport.videoHeight * zoomLevel > viewport.screenHeight + viewport.panThresholdPx

    private fun snapToBoundaries(zoom: Float, viewport: VideoZoomViewport): Float {
        val fill = viewport.fillZoom
        val snapsToFit = abs(zoom - MIN_ZOOM) <= MIN_ZOOM * ZOOM_SNAP_RATIO
        val snapsToFill = abs(zoom - fill) <= fill * ZOOM_SNAP_RATIO
        return when {
            snapsToFit && snapsToFill ->
                if (abs(zoom - MIN_ZOOM) <= abs(zoom - fill)) MIN_ZOOM else fill

            snapsToFit -> MIN_ZOOM
            snapsToFill -> fill
            else -> zoom
        }
    }

    private fun clampTranslation(viewport: VideoZoomViewport) {
        // Geometry unknown (video surface not laid out yet) — leave the pan unclamped
        // rather than wrongly snapping it to zero.
        if (viewport.videoWidth == 0 || viewport.videoHeight == 0) return
        val maxTranslationX =
            ((viewport.videoWidth * zoomLevel - viewport.screenWidth) / 2).coerceAtLeast(0f)
        val maxTranslationY =
            ((viewport.videoHeight * zoomLevel - viewport.screenHeight) / 2).coerceAtLeast(0f)
        // Assign 0f directly on a non-overflowing axis: coerceIn(-0f, 0f) yields -0.0f,
        // which is not bit-equal to 0.0f.
        translationX = if (maxTranslationX == 0f) {
            0f
        } else {
            translationX.coerceIn(-maxTranslationX, maxTranslationX)
        }
        translationY = if (maxTranslationY == 0f) {
            0f
        } else {
            translationY.coerceIn(-maxTranslationY, maxTranslationY)
        }
    }

    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 5f

        // Tolerance for float comparisons against the fit/fill zoom levels.
        private const val ZOOM_LEVEL_EPSILON = 0.01f

        // Snap zone around the fit and fill levels, relative to the boundary value: the
        // pinch locks onto a boundary while within ±5% of it.
        private const val ZOOM_SNAP_RATIO = 0.05f

        // Lower bound for [VideoZoomViewport.panThresholdPx]: 1px absorbs FIT layout rounding
        // so panning is not enabled at the fit level. It is not a gesture threshold — see
        // VideoPlayerController.MIN_PAN_OVERFLOW_DP for the value production passes.
        const val PAN_OVERFLOW_SLACK_PX = 1f
    }
}
