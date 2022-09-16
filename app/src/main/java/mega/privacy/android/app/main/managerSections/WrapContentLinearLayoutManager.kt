package mega.privacy.android.app.main.managerSections

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber

/**
 * The wrap class to solve the exception (java.lang.IndexOutOfBoundsException: Inconsistency detected.)
 */
class WrapContentLinearLayoutManager(context: Context) : LinearLayoutManager(context) {

    /**
     * Lay out all relevant child views from the given adapter.
     * @param recycler Recycler to use for fetching potentially cached views for a
     *                 position
     * @param state    Transient state of RecyclerView
     */
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Timber.d("meet a IndexOutOfBounds in RecyclerView")
        }
    }
}