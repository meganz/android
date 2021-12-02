package mega.privacy.android.app.fragments.homepage.photos

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import mega.privacy.android.app.components.GestureScaleListener

/**
 * Handle with scale gesture event to zoom in/out grid list.
 */
class ScaleGestureHandler(
    context: Context,
    callback: GestureScaleListener.GestureScaleCallback?
) : View.OnTouchListener {

    /**
     * Scale gesture detector.
     */
    val scaleDetector = ScaleGestureDetector(context, GestureScaleListener(callback))

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return when (event.pointerCount) {
            2 -> scaleDetector.onTouchEvent(event)
            else -> false
        }
    }
}