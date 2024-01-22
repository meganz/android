package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.node.NodeId
import java.io.File

/**
 * The entity for the playlist in videos section
 *
 * @property id the playlist id
 * @property title the playlist's title
 * @property cover the playlist's cover
 * @property creationTime the playlist's creation time
 * @property modificationTime the playlist's modification time
 * @property thumbnailList the playlist's thumbnail list
 * @property numberOfVideos the number of videos in the playlist
 * @property totalDuration the total duration of videos in the playlist
 * @property isSelected the playlist if is selected
 */
data class UIVideoPlaylist(
    val id: NodeId,
    val title: String,
    val cover: Long?,
    val creationTime: Long,
    val modificationTime: Long,
    val thumbnailList: List<File>?,
    val numberOfVideos: Int,
    val totalDuration: String,
    val isSelected: Boolean = false,
)
