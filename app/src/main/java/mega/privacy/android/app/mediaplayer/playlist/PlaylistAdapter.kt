package mega.privacy.android.app.mediaplayer.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemPlaylistBinding
import mega.privacy.android.app.databinding.ItemPlaylistHeaderBinding
import mega.privacy.android.app.databinding.ItemPlaylistHeaderVideoBinding
import mega.privacy.android.app.databinding.ItemPlaylistVideoBinding

/**
 * RecyclerView adapter for playlist screen.
 */
class PlaylistAdapter(
    private val itemOperation: PlaylistItemOperation,
    private val isAudioPlayer: Boolean
) :
    ListAdapter<PlaylistItem, PlaylistViewHolder>(PlaylistItemDiffCallback()) {
    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        return when (viewType) {
            PlaylistItem.TYPE_PREVIOUS_HEADER,
            PlaylistItem.TYPE_PLAYING_HEADER,
            PlaylistItem.TYPE_NEXT_HEADER -> {
                if (isAudioPlayer) {
                    PlaylistHeaderHolder(
                        ItemPlaylistHeaderBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                    )
                } else {
                    PlaylistHeaderVideoHolder(
                        ItemPlaylistHeaderVideoBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                    )
                }
            }
            else -> {
                if (isAudioPlayer) {
                    PlaylistItemHolder(
                        ItemPlaylistBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                    )
                } else {
                    PlaylistItemVideoHolder(
                        ItemPlaylistVideoBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                    )
                }
            }
        }
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position), itemOperation)
    }
}
