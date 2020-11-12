package mega.privacy.android.app.audioplayer

interface PlaylistItemOperation {
    fun onItemClick(item: PlaylistItem)

    fun openItemOptionPanel(item: PlaylistItem)
}
