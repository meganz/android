package mega.privacy.android.app.mediaplayer.playlist

import mega.privacy.android.app.databinding.ItemVideoPlaylistBinding

/**
 * ViewHolder for playlist item, for video. Because video playlist is
 * always in dark theme, so we need separate layout file and view holder class.
 * @param binding ItemVideoPlaylistBinding
 */
class VideoPlaylistItemHolder(private val binding: ItemVideoPlaylistBinding) :
    PlaylistViewHolder(binding) {
    override fun bind(
        paused: Boolean,
        item: PlaylistItem,
        itemOperation: PlaylistItemOperation,
        holder: PlaylistViewHolder,
        position: Int
    ) {
        with(binding) {
            this.item = item
            highlight = item.type == PlaylistItem.TYPE_NEXT
            this.itemOperation = itemOperation
            this.holder = holder
            this.position = position
            name = PlaylistItem.getHeaderName(item.type, paused)
        }
    }
}
