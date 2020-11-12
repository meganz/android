package mega.privacy.android.app.audioplayer

import androidx.recyclerview.widget.DiffUtil

class PlaylistItemDiffCallback : DiffUtil.ItemCallback<PlaylistItem>() {
    override fun areItemsTheSame(oldItem: PlaylistItem, newItem: PlaylistItem) =
        oldItem.nodeHandle == newItem.nodeHandle

    override fun areContentsTheSame(oldItem: PlaylistItem, newItem: PlaylistItem) =
        oldItem == newItem
}
