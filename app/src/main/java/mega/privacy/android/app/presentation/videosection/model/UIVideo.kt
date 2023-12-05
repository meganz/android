package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.node.NodeId
import java.io.File

/**
 * The entity for the video is displayed in videos section
 *
 * @property id NodeId
 * @property name the video's name
 * @property size the video's size
 * @property duration the video's duration
 * @property thumbnail the video's thumbnail
 * @property isFavourite the video if is Favourite
 * @property nodeAvailableOffline the video if is available for offline
 * @property isSelected the video if is selected
 */
data class UIVideo(
    val id: NodeId,
    val name: String,
    val size: Long,
    val duration: String?,
    val thumbnail: File? = null,
    val isFavourite: Boolean = false,
    val nodeAvailableOffline: Boolean = false,
    val isSelected: Boolean = false
)
