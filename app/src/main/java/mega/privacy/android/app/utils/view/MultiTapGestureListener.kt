package mega.privacy.android.app.utils.view

import android.view.MotionEvent
import com.facebook.samples.zoomable.DoubleTapGestureListener
import com.facebook.samples.zoomable.ZoomableDraweeView

class MultiTapGestureListener constructor(
    zoomableDraweeView: ZoomableDraweeView,
    private val onSingleTapCallback: () -> Unit,
    private val onZoomCallback: () -> Unit
) : DoubleTapGestureListener(zoomableDraweeView) {

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        onSingleTapCallback.invoke()
        return super.onSingleTapConfirmed(e)
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        onZoomCallback.invoke()
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        onZoomCallback.invoke()
        return super.onDoubleTapEvent(e)
    }
}
