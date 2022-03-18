package mega.privacy.android.app.fragments.managerFragments.cu

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.RelativeLayout

/**
 * Custom a mid layer view for handling touch event.
 * If we want to handle the event by Photos (ViewPager2),set parent.requestDisallowInterceptTouchEvent(true) - parent will intercept;
 * If we want to handle the event by TimelineFragment,set parent.requestDisallowInterceptTouchEvent(false) - parent do not intercept.
 */
class TimelineContainer : RelativeLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * A touchSlop from system ViewConfiguration
     */
    private var touchSlop = 0

    /**
     * this will record x distance
     */
    private var initialX = 0f

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        handleInterceptTouchEvent(e)
        return super.onInterceptTouchEvent(e)
    }

    /**
     * Follow our logic to handle when parent(viewpager2) should intercept the event.
     * In our case, if user has 1 finger on screen and dx are longer than touchSlop*2, that means user wants to switch tab.
     * if user has 2 finger on screen, that means user wants to zoom in/out
     */
    private fun handleInterceptTouchEvent(e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = e.x
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.x - initialX

                if (e.pointerCount > 1) {
                    parent.requestDisallowInterceptTouchEvent(true)
                } else {
                    /**
                     * 1 finger case
                     *
                     * If user switches tab by viewpager, we assuming finger just moves a short distance (dx<touchSlop * 2), then viewpager should not disallow intercept event
                     * If user want to zoom, the finger moves a long distance (dx>touchSlop * 2),then viewpager should disallow intercept event
                     */
                    if (dx > touchSlop * 2) {
                        parent.requestDisallowInterceptTouchEvent(true)
                    } else {
                        parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
        }
    }
}