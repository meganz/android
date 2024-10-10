package mega.privacy.android.domain.entity.videosection

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import kotlin.time.Duration

/**
 * Entity video playlist
 *
 * @property thumbnailList The node ID of the video item that is associated with the thumbnail.
 * @property numberOfVideos The number of videos in the playlist
 * @property totalDuration The total duration of videos in the playlist
 * @property videos the videos in the playlist
 */
interface VideoPlaylist {
    val thumbnailList: List<NodeId>?
    val numberOfVideos: Int
    val totalDuration: Duration
    val videos: List<TypedVideoNode>?
}
