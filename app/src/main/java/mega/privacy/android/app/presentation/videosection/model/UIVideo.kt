package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.node.NodeId

/**
 * The entity for the video is displayed in videos section
 *
 * @property id NodeId
 * @property name the video's name
 * @property size the video's size
 * @property duration the video's duration
 * @property thumbnail the video's thumbnail
 */
data class UIVideo(
    val id: NodeId,
    val name: String,
    val size: Long,
    val duration: Int? = 0,
    val thumbnail: String? = null,
)
