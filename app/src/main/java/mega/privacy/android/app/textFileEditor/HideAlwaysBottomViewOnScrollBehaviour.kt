package mega.privacy.android.app.textFileEditor

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import mega.privacy.android.app.textFileEditor.TextFileEditorActivity.Companion.TIME_SHOWING_PAGINATION_BUTTONS

class HideAlwaysBottomViewOnScrollBehaviour<V : View>(context: Context, attrs: AttributeSet) :
    HideBottomViewOnScrollBehavior<V>(context, attrs) {

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
        if (dyConsumed != 0) {
            slideDown(child)
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        type: Int
    ) {
        child.postDelayed({ slideUp(child) }, TIME_SHOWING_PAGINATION_BUTTONS)
    }
}