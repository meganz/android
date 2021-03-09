package mega.privacy.android.app.mediaplayer.playlist

import mega.privacy.android.app.databinding.ItemAudioPlaylistBinding

/**
 * ViewHolder for playlist item, for audio.
 */
class AudioPlaylistItemHolder(private val binding: ItemAudioPlaylistBinding) :
    PlaylistViewHolder(binding) {
    override fun bind(item: PlaylistItem, itemOperation: PlaylistItemOperation) {
        binding.item = item
        binding.highlight = item.type == PlaylistItem.TYPE_NEXT
        binding.itemOperation = itemOperation
    }
}
