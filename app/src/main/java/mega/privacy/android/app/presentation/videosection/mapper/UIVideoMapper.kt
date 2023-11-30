package mega.privacy.android.app.presentation.videosection.mapper

import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.node.VideoNode
import java.io.File
import javax.inject.Inject

/**
 * The mapper class to convert the VideoNode to UIVideo
 */
class UIVideoMapper @Inject constructor() {
    /**
     * Convert to VideoNode to UIVideo
     */
    operator fun invoke(
        videoNode: VideoNode,
    ) = UIVideo(
        id = videoNode.id,
        name = videoNode.name,
        size = videoNode.size,
        duration = TimeUtils.getVideoDuration(videoNode.duration),
        thumbnail = videoNode.thumbnailFilePath?.let { File(it) },
        isFavourite = videoNode.isFavourite,
        nodeAvailableOffline = videoNode.isAvailableOffline
    )
}
