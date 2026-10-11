package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to change [Node]'s favorite property
 * @property nodeRepository [NodeRepository]
 */
class UpdateNodeFavoriteUseCase @Inject constructor(private val nodeRepository: NodeRepository) {
    /**
     * Invoke
     * @param nodeId [NodeId]
     * @param isFavorite
     */
    suspend operator fun invoke(nodeId: NodeId, isFavorite: Boolean) =
        nodeRepository.updateFavoriteNode(nodeId = nodeId, isFavorite = isFavorite)
}