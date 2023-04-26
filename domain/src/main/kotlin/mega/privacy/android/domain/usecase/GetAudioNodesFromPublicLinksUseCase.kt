package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for get audio nodes from public links
 */
class GetAudioNodesFromPublicLinksUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Get audio nodes from public links
     *
     * @param order [SortOrder]
     * @return audio nodes
     */

    suspend operator fun invoke(order: SortOrder): List<TypedNode> =
        mediaPlayerRepository.getAudioNodesFromPublicLinks(order).map { addNodeType(it) }
}