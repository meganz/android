package mega.privacy.android.app.fragments.homepage

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/*
* when exit search mode from list view of homepage file categories, we can observe
* item divider and item content messed up for a while: the item divider is drawn in
* the right place immediately, but the item will have a fall down animation.
* disable animator for a while (default 100ms) could avoid this issue.
*/
fun disableRecyclerViewAnimator(rv: RecyclerView, duration: Long = 100) {
    val oldAnimator = rv.itemAnimator ?: DefaultItemAnimator()
    rv.itemAnimator = null
    rv.postDelayed({ rv.itemAnimator = oldAnimator }, duration)
}
