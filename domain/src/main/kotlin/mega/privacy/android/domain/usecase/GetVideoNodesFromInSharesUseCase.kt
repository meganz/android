package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting video nodes from InShares
 */
class GetVideoNodesFromInSharesUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Getting video nodes from InShares
     *
     * @param order [SortOrder]
     * @return video nodes
     */
    suspend operator fun invoke(order: SortOrder) =
        mediaPlayerRepository.getVideoNodesFromInShares(order).map { addNodeType(it) }
}