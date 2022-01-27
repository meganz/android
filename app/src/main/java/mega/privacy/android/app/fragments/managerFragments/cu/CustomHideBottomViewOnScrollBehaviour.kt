package mega.privacy.android.app.fragments.managerFragments.cu

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior

/**
 * CoordinatorLayout behaviour to hide some view when a list is scrolled
 * and show it only when the list is in its initial position without scrolling.
 */
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

        // ListView goes up.
        if(dyConsumed > 0) {
            slideDown(child)
        } else {
            slideUp(child)
        }
    }
}