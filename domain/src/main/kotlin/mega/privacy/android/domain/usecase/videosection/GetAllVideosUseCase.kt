package mega.privacy.android.domain.usecase.videosection

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.VideoNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * The use case for getting all video nodes
 */
class GetAllVideosUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Get the all video nodes
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(order: SortOrder): Flow<List<VideoNode>> = flow {
        emit(videoSectionRepository.getAllVideos(order))
        emitAll(
            nodeRepository.monitorNodeUpdates()
                .mapLatest { videoSectionRepository.getAllVideos(order) }
        )
    }.mapLatest { it }
}