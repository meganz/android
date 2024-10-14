package mega.privacy.android.domain.entity.videosection

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import kotlin.time.Duration

/**
 * The Favourites video playlist
 */
data class FavouritesVideoPlaylist(
    override val thumbnailList: List<NodeId>?,
    override val numberOfVideos: Int,
    override val totalDuration: Duration,
    override val videos: List<TypedVideoNode>?,
) : SystemVideoPlaylist