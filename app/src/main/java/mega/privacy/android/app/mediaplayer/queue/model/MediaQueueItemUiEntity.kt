package mega.privacy.android.app.mediaplayer.queue.model

import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.node.NodeId
import java.io.File

/**
 * The UI entity for media queue item.
 *
 * @property icon the default drawable resource of media queue item
 * @property id the NodeId of media queue item
 * @property nodeName the node name of media queue item
 * @property thumbnail the thumbnail file of media queue item
 * @property type the type of media queue item
 * @property duration the duration of media queue item
 * @property isSelected whether the item is selected
 */
data class MediaQueueItemUiEntity(
    @DrawableRes val icon: Int,
    val id: NodeId,
    val nodeName: String,
    val thumbnail: File?,
    val type: MediaQueueItemType,
    val duration: String,
    val isSelected: Boolean = false,
)
