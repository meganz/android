package mega.privacy.android.app.presentation.videoplayer.mapper

import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerItem
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Mapper to convert VideoPlayerEntity
 */
class VideoPlayerItemMapper @Inject constructor() {

    /**
     * Convert to VideoPlayerEntity
     */
    operator fun invoke(
        nodeHandle: Long,
        nodeName: String,
        thumbnail: File?,
        type: MediaQueueItemType,
        size: Long,
        duration: Duration = 0.seconds,
    ) = VideoPlayerItem(
        nodeHandle = nodeHandle,
        nodeName = nodeName,
        thumbnail = thumbnail,
        type = type,
        size = size,
        duration = duration,
    )
}