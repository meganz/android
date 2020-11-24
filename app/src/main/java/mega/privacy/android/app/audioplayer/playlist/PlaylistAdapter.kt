package mega.privacy.android.app.audioplayer.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemPlaylistBinding
import mega.privacy.android.app.databinding.ItemPlaylistHeaderBinding

/**
 * RecyclerView adapter for playlist screen.
 */
class PlaylistAdapter(private val itemOperation: PlaylistItemOperation) :
    ListAdapter<PlaylistItem, PlaylistViewHolder>(PlaylistItemDiffCallback()) {
    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        return when (viewType) {
            PlaylistItem.TYPE_PREVIOUS_HEADER,
            PlaylistItem.TYPE_PLAYING_HEADER,
            PlaylistItem.TYPE_NEXT_HEADER -> {
                PlaylistHeaderHolder(
                    ItemPlaylistHeaderBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )
            }
            else -> {
                PlaylistItemHolder(
                    ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position), itemOperation)
    }
}
