package mega.privacy.android.app.mediaplayer.playlist

import mega.privacy.android.app.databinding.ItemAudioPlaylistBinding

/**
 * ViewHolder for playlist item, for audio.
 * @param binding ItemAudioPlaylistBinding
 */
class AudioPlaylistItemHolder(private val binding: ItemAudioPlaylistBinding) :
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
            this.paused = paused
            this.itemOperation = itemOperation
            this.holder = holder
            this.position = position
            name = PlaylistItem.getHeaderName(item.type, paused)
        }
    }
}
