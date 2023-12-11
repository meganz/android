package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.repository.VideoSectionRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * The use case for getting all video nodes
 */
class GetAllVideosUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val videoSectionRepository: VideoSectionRepository,
) {
    /**
     * Get the all video nodes
     */
    suspend operator fun invoke(): List<TypedVideoNode> =
        videoSectionRepository.getAllVideos(getCloudSortOrder())
}