package mega.privacy.android.app.components

import android.view.ScaleGestureDetector
import mega.privacy.android.app.lollipop.ManagerActivityLollipop

class GridScaleGestureDetector(
    managerActivity: ManagerActivityLollipop
) : ScaleGestureDetector(managerActivity, ScaleListener(managerActivity)) {

    class ScaleListener(private val managerActivity: ManagerActivityLollipop) : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if(detector.scaleFactor > 1) {
                managerActivity.zoomIn()
            } else {
                managerActivity.zoomOut()
            }
            super.onScaleEnd(detector)
        }
    }
}