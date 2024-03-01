package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.node.NodeId
import java.io.File

/**
 * The entity for the video is displayed in videos section
 *
 * @property id NodeId
 * @property parentId the video's parent id
 * @property name the video's name
 * @property size the video's size
 * @property durationString the video's duration String
 * @property durationInMinutes the video's duration in minutes
 * @property thumbnail the video's thumbnail
 * @property isFavourite the video if is Favourite
 * @property nodeAvailableOffline the video if is available for offline
 * @property isSharedItems the video if is share
 * @property isSelected the video if is selected
 */
data class VideoUIEntity(
    val id: NodeId,
    val parentId: NodeId,
    val name: String,
    val size: Long,
    val durationString: String?,
    val durationInMinutes: Long,
    val thumbnail: File? = null,
    val isFavourite: Boolean = false,
    val nodeAvailableOffline: Boolean = false,
    val isSharedItems: Boolean = false,
    val isSelected: Boolean = false
)
