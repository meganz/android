package mega.privacy.android.app.presentation.videosection.mapper

import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.node.VideoNode
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import java.io.File
import javax.inject.Inject

/**
 * The mapper class to convert the VideoNode to UIVideo
 */
class UIVideoMapper @Inject constructor(
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val addNodeType: AddNodeType
) {
    /**
     * Convert to VideoNode to UIVideo
     */
    suspend operator fun invoke(
        videoNode: VideoNode,
    ) = UIVideo(
        id = videoNode.fileNode.id,
        name = videoNode.fileNode.name,
        size = videoNode.fileNode.size,
        duration = TimeUtils.getVideoDuration(videoNode.duration),
        thumbnail = videoNode.thumbnailFilePath?.let { File(it) },
        isFavourite = videoNode.fileNode.isFavourite,
        nodeAvailableOffline = isAvailableOfflineUseCase(addNodeType(videoNode.fileNode))
    )
}
