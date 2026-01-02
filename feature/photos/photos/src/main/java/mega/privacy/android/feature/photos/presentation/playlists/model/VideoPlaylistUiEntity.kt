package mega.privacy.android.feature.photos.presentation.playlists.model

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity

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
 * @property isSystemVideoPlayer the playlist if is system video player
 */
data class VideoPlaylistUiEntity(
    val id: NodeId,
    val title: String,
    val cover: Long? = null,
    val creationTime: Long = 0,
    val modificationTime: Long = 0,
    val thumbnailList: List<NodeId>? = null,
    val numberOfVideos: Int = 0,
    val totalDuration: String = "",
    val videos: List<VideoUiEntity>? = null,
    val isSelected: Boolean = false,
    val isSystemVideoPlayer: Boolean = false
) {
    val type: PlaylistType = if (isSystemVideoPlayer) {
        PlaylistType.Favourite
    } else {
        PlaylistType.User
    }
}
