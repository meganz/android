package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.node.NodeId

/**
 * The entity for the playlist in videos section
 *
 * @property id the playlist id
 * @property title the playlist's title
 * @property cover the playlist's cover
 * @property creationTime the playlist's creation time
 * @property modificationTime the playlist's modification time
 * @property thumbnailList The node ID of the video item that is associated with the thumbnail.
 * @property numberOfVideos the number of videos in the playlist
 * @property totalDuration the total duration of videos in the playlist
 * @property videos the videos in the playlist
 * @property isSelected the playlist if is selected
 */
data class VideoPlaylistUIEntity(
    val id: NodeId,
    val title: String,
    val cover: Long?,
    val creationTime: Long,
    val modificationTime: Long,
    val thumbnailList: List<NodeId>?,
    val numberOfVideos: Int,
    val totalDuration: String,
    val videos: List<VideoUIEntity>?,
    val isSelected: Boolean = false,
)
