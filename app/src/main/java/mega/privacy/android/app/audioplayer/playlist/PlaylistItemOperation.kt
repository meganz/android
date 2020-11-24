package mega.privacy.android.app.audioplayer.playlist

/**
 * Interface for playlist item operation.
 */
interface PlaylistItemOperation {
    /**
     * Called when item is clicked.
     *
     * @param item clicked item
     */
    fun onItemClick(item: PlaylistItem)

    /**
     * Called when remove icon is clicked.
     *
     * @param item item to remove
     */
    fun removeItem(item: PlaylistItem)
}
