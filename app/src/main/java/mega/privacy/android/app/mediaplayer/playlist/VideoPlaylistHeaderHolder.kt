package mega.privacy.android.app.mediaplayer.playlist

import mega.privacy.android.app.databinding.ItemVideoPlaylistHeaderBinding

/**
 * ViewHolder for Previous, Playing, Next headers, for video. Because video playlist is
 * always in dark theme, so we need separate layout file and view holder class.
 * @param binding ItemVideoPlaylistHeaderBinding
 */
class VideoPlaylistHeaderHolder(private val binding: ItemVideoPlaylistHeaderBinding) :
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
