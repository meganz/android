package mega.privacy.android.app.utils.view

import android.graphics.Matrix
import android.graphics.PointF
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import com.facebook.samples.zoomable.AbstractAnimatedZoomableController
import com.facebook.samples.zoomable.DefaultZoomableController
import com.facebook.samples.zoomable.ZoomableDraweeView
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Custom tap gesture listener for double tap to zoom / unzoom, double-tap-and-drag to zoom,
 * single taps and combined zoom.
 *
 * @property zoomableDraweeView     Zoomable View
 * @property onSingleTapCallback    Callback to be called when single tap is triggered.
 * @property onTransformChanged     Callback to be called when any zoom action/scroll is triggered.
 */
class MultiTapGestureListener constructor(
    private val zoomableDraweeView: ZoomableDraweeView,
    private val onSingleTapCallback: () -> Unit,
    private val onTransformChanged: (Matrix) -> Unit,
) : SimpleOnGestureListener() {

    private val doubleTapViewPoint = PointF()
    private val doubleTapImagePoint = PointF()
    private var doubleTapScale = 1.0f
    private var doubleTapScroll = false
    private var doubleTapMaxScaleDivider = 4
    private var durationMs = 300L

    /**
     * Handles Single Tap
     *
     * @param event
     */
    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        onSingleTapCallback.invoke()
        return super.onSingleTapConfirmed(event)
    }

    /**
     * Handles Scroll
     *
     * @param e1
     * @param e1
     * @param distanceX
     * @param distanceY
     */
    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
        val zc = this.zoomableDraweeView.zoomableController as AbstractAnimatedZoomableController
        onTransformChanged(zc.transform)
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    /**
     * Handles Double Tap
     *
     * @param event
     */
    override fun onDoubleTapEvent(event: MotionEvent): Boolean {
        val zc = this.zoomableDraweeView.zoomableController as AbstractAnimatedZoomableController
        val vp = PointF(event.x, event.y)
        val ip = zc.mapViewToImage(vp)
        val maxScale: Float
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                this.doubleTapViewPoint.set(vp)
                this.doubleTapImagePoint.set(ip)
                this.doubleTapScale = zc.scaleFactor
                onTransformChanged(zc.transform)
            }

            MotionEvent.ACTION_UP -> {
                if (this.doubleTapScroll) {
                    maxScale = this.calcScale(vp)
                    zc.zoomToPoint(maxScale, this.doubleTapImagePoint, this.doubleTapViewPoint)
                    onTransformChanged(zc.transform)
                } else {
                    maxScale = zc.maxScaleFactor / doubleTapMaxScaleDivider
                    val minScale = zc.minScaleFactor
                    if (zc.scaleFactor < (maxScale + minScale) / 2.0f) {
                        zc.zoomToPoint(
                            maxScale,
                            ip,
                            vp,
                            DefaultZoomableController.LIMIT_ALL,
                            durationMs,
                            null as Runnable?
                        )
                        onTransformChanged(zc.transform)
                    } else {
                        zc.zoomToPoint(
                            minScale,
                            ip,
                            vp,
                            DefaultZoomableController.LIMIT_ALL,
                            durationMs,
                            null as Runnable?
                        )
                        onTransformChanged(zc.transform)
                    }
                }
                this.doubleTapScroll = false
            }

            MotionEvent.ACTION_MOVE -> {
                this.doubleTapScroll = this.doubleTapScroll || this.shouldStartDoubleTapScroll(vp)
                if (this.doubleTapScroll) {
                    maxScale = this.calcScale(vp)
                    zc.zoomToPoint(maxScale, this.doubleTapImagePoint, this.doubleTapViewPoint)
                    onTransformChanged(zc.transform)
                }
            }
        }
        return true
    }

    private fun shouldStartDoubleTapScroll(viewPoint: PointF): Boolean {
        val dist = hypot(
            (viewPoint.x - doubleTapViewPoint.x).toDouble(),
            (viewPoint.y - doubleTapViewPoint.y).toDouble()
        )
        return dist > 20.0
    }

    private fun calcScale(currentViewPoint: PointF): Float {
        val dy = currentViewPoint.y - doubleTapViewPoint.y
        val t = 1.0f + abs(dy) * 0.001f
        return if (dy < 0.0f) doubleTapScale / t else doubleTapScale * t
    }
}
