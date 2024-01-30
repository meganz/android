package mega.privacy.android.domain.entity.videosection

import mega.privacy.android.domain.entity.node.NodeId
import kotlin.time.Duration

/**
 * Entity video playlist
 *
 * @property id The playlist ID
 * @property title The playlist title
 * @property cover The playlist cover
 * @property creationTime The playlist creation time
 * @property modificationTime The playlist modification time
 * @property thumbnailList The playlist thumbnail list
 * @property numberOfVideos The number of videos in the playlist
 * @property totalDuration The total duration of videos in the playlist
 */
data class VideoPlaylist(
    val id: NodeId,
    val title: String,
    val cover: Long?,
    val creationTime: Long,
    val modificationTime: Long,
    val thumbnailList: List<String?>?,
    val numberOfVideos: Int,
    val totalDuration: Duration,
)
