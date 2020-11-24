package mega.privacy.android.app.audioplayer.playlist

import mega.privacy.android.app.databinding.ItemPlaylistBinding

/**
 * ViewHolder for playlist item.
 */
class PlaylistItemHolder(private val binding: ItemPlaylistBinding) :
    PlaylistViewHolder(binding) {
    override fun bind(item: PlaylistItem, itemOperation: PlaylistItemOperation) {
        binding.item = item
        binding.highlight = item.type == PlaylistItem.TYPE_NEXT
        binding.itemOperation = itemOperation
    }
}
