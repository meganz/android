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
        binding.item = item
        binding.highlight = item.type == PlaylistItem.TYPE_NEXT
        binding.paused = paused
        binding.itemOperation = itemOperation
        binding.holder = holder
        binding.position = position
    }
}
