package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting video nodes from public links
 */
class GetVideoNodesFromPublicLinksUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Get video nodes from public links
     *
     * @param order [SortOrder]
     * @return video nodes
     */
    suspend operator fun invoke(order: SortOrder) =
        mediaPlayerRepository.getVideoNodesFromPublicLinks(order).map { addNodeType(it) }
}