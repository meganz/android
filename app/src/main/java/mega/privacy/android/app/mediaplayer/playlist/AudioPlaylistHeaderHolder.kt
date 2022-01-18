package mega.privacy.android.app.mediaplayer.playlist

import mega.privacy.android.app.databinding.ItemAudioPlaylistHeaderBinding

/**
 * ViewHolder for Previous, Playing, Next headers, for audio.
 * @param binding ItemAudioPlaylistHeaderBinding
 */
class AudioPlaylistHeaderHolder(private val binding: ItemAudioPlaylistHeaderBinding) :
    PlaylistViewHolder(binding) {
    override fun bind(
        paused: Boolean,
        item: PlaylistItem,
        itemOperation: PlaylistItemOperation,
        holder: PlaylistViewHolder,
        position: Int
    ) {
        binding.highlight = item.type == PlaylistItem.TYPE_NEXT_HEADER
        binding.name = item.nodeName
    }
}
