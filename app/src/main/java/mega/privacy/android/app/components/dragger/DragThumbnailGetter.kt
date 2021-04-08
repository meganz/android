package mega.privacy.android.app.components.dragger

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Interface to get dragging thumbnail info from RecyclerView, should be implemented by adapter.
 */
interface DragThumbnailGetter {
    /**
     * Get adapter position of one node.
     *
     * @param handle the handle of node
     * @return the adapter position of this node
     */
    fun getNodePosition(handle: Long): Int

    /**
     * Get thumbnail view of the given view holder.
     *
     * @param viewHolder the view holder
     * @return the thumbnail view
     */
    fun getThumbnail(viewHolder: RecyclerView.ViewHolder): View?
}
