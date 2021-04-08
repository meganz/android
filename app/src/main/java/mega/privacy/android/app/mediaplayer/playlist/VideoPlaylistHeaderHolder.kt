package mega.privacy.android.app.mediaplayer.playlist

import mega.privacy.android.app.databinding.ItemVideoPlaylistHeaderBinding

/**
 * ViewHolder for Previous, Playing, Next headers, for video. Because video playlist is
 * always in dark theme, so we need separate layout file and view holder class.
 */
class VideoPlaylistHeaderHolder(private val binding: ItemVideoPlaylistHeaderBinding) :
    PlaylistViewHolder(binding) {
    override fun bind(item: PlaylistItem, itemOperation: PlaylistItemOperation) {
        binding.highlight = item.type == PlaylistItem.TYPE_NEXT_HEADER
        binding.name = item.nodeName
    }
}
