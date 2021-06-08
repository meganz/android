package mega.privacy.android.app.fragments.managerFragments.cu

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION

class CustomHideBottomViewOnScrollBehaviour<V : View> : HideBottomViewOnScrollBehavior<V>() {

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if (target !is RecyclerView) {
            return
        }

        if(target.canScrollVertically(SCROLLING_UP_DIRECTION)) {
            slideDown(child)
        } else {
            slideUp(child)
        }
    }
}