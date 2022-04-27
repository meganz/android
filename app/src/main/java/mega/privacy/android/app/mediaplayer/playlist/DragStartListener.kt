package mega.privacy.android.app.mediaplayer.playlist

/**
 * Interface for listening drag start
 */
interface DragStartListener {
    /**
     * The callback when drag is started
     * @param holder the view holder which is dragged
     */
    fun onDragStarted(holder: PlaylistViewHolder)
}