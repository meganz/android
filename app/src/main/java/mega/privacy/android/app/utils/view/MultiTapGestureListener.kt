package mega.privacy.android.app.utils.view

import android.view.MotionEvent
import com.facebook.samples.zoomable.DoubleTapGestureListener
import com.facebook.samples.zoomable.ZoomableDraweeView

/**
 * Custom tap gesture listener for double tap to zoom / unzoom, double-tap-and-drag to zoom,
 * single taps and combined zoom.
 *
 * @property onSingleTapCallback    Callback to be called when single tap is triggered.
 * @property onZoomCallback         Callback to be called when any zoom action is triggered.
 */
class MultiTapGestureListener constructor(
    zoomableDraweeView: ZoomableDraweeView,
    private val onSingleTapCallback: () -> Unit,
    private val onZoomCallback: () -> Unit
) : DoubleTapGestureListener(zoomableDraweeView) {

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        onSingleTapCallback.invoke()
        return super.onSingleTapConfirmed(e)
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        onZoomCallback.invoke()
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        onZoomCallback.invoke()
        return super.onDoubleTapEvent(e)
    }
}
