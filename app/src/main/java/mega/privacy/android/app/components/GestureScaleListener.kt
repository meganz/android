package mega.privacy.android.app.components

import android.view.ScaleGestureDetector
import mega.privacy.android.app.lollipop.ManagerActivityLollipop

/**
 * Listener to detect scale end event.
 * Used to grid view zoom in/out.
 */
class GestureScaleListener(
    private val managerActivity: ManagerActivityLollipop
) : ScaleGestureDetector.SimpleOnScaleGestureListener() {

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        //Scale factor is larger than 1 means zoom in.
        if(detector.scaleFactor > 1) {
            managerActivity.zoomIn()
        } else {
            managerActivity.zoomOut()
        }
        super.onScaleEnd(detector)
    }
}