package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import java.io.File
import kotlin.time.Duration

/**
 * The entity for the video is displayed in videos section
 *
 * @property id NodeId
 * @property parentId the video's parent id
 * @property name the video's name
 * @property size the video's size
 * @property fileTypeInfo the video's file type info
 * @property durationString the video's duration String
 * @property duration the video's duration
 * @property thumbnail the video's thumbnail
 * @property isFavourite the video if is Favourite
 * @property nodeAvailableOffline the video if is available for offline
 * @property isSharedItems the video if is share
 * @property label the video's label
 * @property elementID the element id if the video is belong to a playlist
 * @property isSelected the video if is selected
 * @property isMarkedSensitive the video if is marked as sensitive
 * @property isSensitiveInherited the video if is sensitive inherited
 * @property watchedDate the video's watched date
 */
data class VideoUIEntity(
    val id: NodeId,
    val parentId: NodeId,
    val name: String,
    val size: Long,
    val fileTypeInfo: FileTypeInfo,
    val durationString: String?,
    val duration: Duration,
    val thumbnail: File? = null,
    val isFavourite: Boolean = false,
    val nodeAvailableOffline: Boolean = false,
    val isSharedItems: Boolean = false,
    val label: Int = 0,
    val elementID: Long? = null,
    val isSelected: Boolean = false,
    val isMarkedSensitive: Boolean = false,
    val isSensitiveInherited: Boolean = false,
    val watchedDate: String,
)
