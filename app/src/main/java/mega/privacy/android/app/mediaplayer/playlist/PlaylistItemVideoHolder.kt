package mega.privacy.android.app.mediaplayer.playlist

import mega.privacy.android.app.databinding.ItemPlaylistVideoBinding

/**
 * ViewHolder for playlist item.
 */
class PlaylistItemVideoHolder(private val binding: ItemPlaylistVideoBinding) :
    PlaylistViewHolder(binding) {
    override fun bind(item: PlaylistItem, itemOperation: PlaylistItemOperation) {
        binding.item = item
        binding.highlight = item.type == PlaylistItem.TYPE_NEXT
        binding.itemOperation = itemOperation
    }
}
