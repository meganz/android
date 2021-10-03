package mega.privacy.android.app.utils.view.zoomable

import android.view.MotionEvent

class MultiTapGestureListener(
    zoomableDraweeView: ZoomableDraweeView,
    private val singleTapCallback: () -> Unit
) : DoubleTapGestureListener(zoomableDraweeView) {

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        singleTapCallback.invoke()
        return true
    }
}
