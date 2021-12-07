package mega.privacy.android.app.components

import android.view.ScaleGestureDetector
import mega.privacy.android.app.lollipop.ManagerActivityLollipop

/**
 * Listener to detect scale end event.
 * Used to grid view zoom in/out.
 */
class GestureScaleListener(
    private val callback: GestureScaleCallback?
) : ScaleGestureDetector.SimpleOnScaleGestureListener() {

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        //Scale factor is larger than 1 means zoom in.
        if(detector.scaleFactor > 1) {
            callback?.zoomIn()
        } else {
            callback?.zoomOut()
        }
        super.onScaleEnd(detector)
    }

    interface GestureScaleCallback {
        fun zoomIn()
        fun zoomOut()
    }

}