package mega.privacy.android.app.audioplayer.playlist

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

/**
 * Base class for ViewHolder for PlaylistItem.
 */
abstract class PlaylistViewHolder(binding: ViewDataBinding) :
    RecyclerView.ViewHolder(binding.root) {
    abstract fun bind(item: PlaylistItem, itemOperation: PlaylistItemOperation)
}
