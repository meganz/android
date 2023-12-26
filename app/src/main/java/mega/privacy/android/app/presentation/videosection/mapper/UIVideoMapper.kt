package mega.privacy.android.app.presentation.videosection.mapper

import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.node.TypedVideoNode
import java.io.File
import javax.inject.Inject

/**
 * The mapper class to convert the TypedVideoNode to UIVideo
 */
class UIVideoMapper @Inject constructor() {
    /**
     * Convert to VideoNode to UIVideo
     */
    operator fun invoke(
        typedVideoNode: TypedVideoNode,
    ) = UIVideo(
        id = typedVideoNode.id,
        name = typedVideoNode.name,
        size = typedVideoNode.size,
        duration = TimeUtils.getVideoDuration(typedVideoNode.duration),
        thumbnail = typedVideoNode.thumbnailPath?.let { File(it) },
        isFavourite = typedVideoNode.isFavourite,
        nodeAvailableOffline = typedVideoNode.isAvailableOffline
    )
}
