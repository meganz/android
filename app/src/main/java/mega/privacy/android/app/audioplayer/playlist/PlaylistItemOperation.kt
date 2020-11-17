package mega.privacy.android.app.audioplayer.playlist

interface PlaylistItemOperation {
    fun onItemClick(item: PlaylistItem)

    fun openItemRemove(item: PlaylistItem)
}
