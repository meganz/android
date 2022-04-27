package mega.privacy.android.app.mediaplayer.playlist

import android.view.View

/**
 * Interface for playlist item operation.
 */
interface PlaylistItemOperation {
    /**
     * Called when item is clicked.
     *
     * @param view clicked view
     * @param item clicked item
     * @param holder clicked holder
     * @param position clicked position
     */
    fun onItemClick(view: View, item: PlaylistItem, holder: PlaylistViewHolder, position: Int)
}
