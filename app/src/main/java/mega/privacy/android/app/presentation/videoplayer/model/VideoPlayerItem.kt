package mega.privacy.android.app.presentation.videoplayer.model

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.annotation.DrawableRes
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import java.io.File


/**
 * The entity for the video player feature.
 * This entity serves as a unified structure for representing video data from various sources,
 * standardizing diverse data types for consistent handling in the video player.
 *
 * @property icon the icon of play list item
 * @property nodeHandle node handle
 * @property nodeName node name
 * @property thumbnail thumbnail file path, null if not available
 * @property type item type
 * @property size size of the node
 * @property duration the duration of media item
 * @property isSelected whether the item is selected
 */
data class VideoPlayerItem(
    @DrawableRes val icon: Int = iconPackR.drawable.ic_video_medium_solid,
    val nodeHandle: Long,
    val nodeName: String,
    val thumbnail: File? = null,
    val type: MediaQueueItemType,
    val size: Long,
    val duration: String,
    val isSelected: Boolean = false,
)
