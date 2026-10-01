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
     *
     * @param searchQuery
     * @param tag
     * @param description
     */
    suspend operator fun invoke(
        searchQuery: String = "",
        tag: String? = null,
        description: String? = null,
    ): List<TypedVideoNode> =
        videoSectionRepository.getAllVideos(
            searchQuery = searchQuery,
            tag = tag,
            description = description,
            order = getCloudSortOrder()
        )
}