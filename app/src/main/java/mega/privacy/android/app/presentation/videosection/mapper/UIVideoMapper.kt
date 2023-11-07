package mega.privacy.android.app.presentation.videosection.mapper

import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.domain.entity.node.VideoNode
import javax.inject.Inject

/**
 * The mapper class to convert the VideoNode to UIVideo
 */
class UIVideoMapper @Inject constructor() {
    /**
     * Convert to VideoNode to UIVideo
     */
    operator fun invoke(
        videoNode: VideoNode
    ) = UIVideo(
        id = videoNode.fileNode.id,
        name = videoNode.fileNode.name,
        size = videoNode.fileNode.size,
        duration = videoNode.duration,
        thumbnail = videoNode.thumbnailFilePath
    )
}
