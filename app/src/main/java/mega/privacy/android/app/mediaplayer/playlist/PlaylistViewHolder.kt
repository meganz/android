package mega.privacy.android.app.mediaplayer.playlist

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemPlaylistBinding

/**
 * ViewHolder for playlist item.
 * @param binding ItemPlaylistBinding
 */
class PlaylistViewHolder(private val binding: ItemPlaylistBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
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
