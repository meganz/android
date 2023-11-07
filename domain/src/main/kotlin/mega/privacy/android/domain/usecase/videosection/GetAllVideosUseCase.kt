package mega.privacy.android.domain.usecase.videosection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.node.VideoNode
import javax.inject.Inject

/**
 * The use case for getting all video nodes
 */
class GetAllVideosUseCase @Inject constructor() {
    /**
     * Get the all video nodes
     */
    operator fun invoke(): Flow<List<VideoNode>> = flow {
        emptyList<VideoNode>()
    }
}