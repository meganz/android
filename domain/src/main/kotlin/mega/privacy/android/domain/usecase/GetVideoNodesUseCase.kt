package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting audio nodes
 */
class GetVideoNodesUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Getting video nodes
     *
     * @param order [SortOrder]
     * @return video nodes
     */
    suspend operator fun invoke(order: SortOrder) =
        mediaPlayerRepository.getVideoNodes(order).map { addNodeType(it) }
}