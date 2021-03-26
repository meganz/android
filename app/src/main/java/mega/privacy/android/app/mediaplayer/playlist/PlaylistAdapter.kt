package mega.privacy.android.app.mediaplayer.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemAudioPlaylistBinding
import mega.privacy.android.app.databinding.ItemAudioPlaylistHeaderBinding
import mega.privacy.android.app.databinding.ItemVideoPlaylistBinding
import mega.privacy.android.app.databinding.ItemVideoPlaylistHeaderBinding

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
                    AudioPlaylistHeaderHolder(
                        ItemAudioPlaylistHeaderBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                    )
                } else {
                    VideoPlaylistHeaderHolder(
                        ItemVideoPlaylistHeaderBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                    )
                }
            }
            else -> {
                if (isAudioPlayer) {
                    AudioPlaylistItemHolder(
                        ItemAudioPlaylistBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                    )
                } else {
                    VideoPlaylistItemHolder(
                        ItemVideoPlaylistBinding.inflate(
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
