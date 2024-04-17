package mega.privacy.android.app.mediaplayer.mapper

import androidx.annotation.DrawableRes
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.node.NodeId
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Mapper to convert MediaQueueItemUiEntity
 */
class MediaQueueItemUiEntityMapper @Inject constructor(
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
) {

    /**
     * Convert MediaQueueItemUiEntity
     */
    operator fun invoke(
        @DrawableRes icon: Int,
        thumbnailFile: File?,
        id: NodeId,
        name: String,
        type: MediaQueueItemType,
        duration: Duration,
    ) = MediaQueueItemUiEntity(
        icon = icon,
        id = id,
        nodeName = name,
        thumbnail = thumbnailFile,
        type = type,
        duration = durationInSecondsTextMapper(duration)
    )
}