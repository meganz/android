package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Default has ancestor
 *
 * @property nodeRepository
 */
class DefaultHasAncestor @Inject constructor(
    private val nodeRepository: NodeRepository,
) : HasAncestor {

    override suspend fun invoke(targetNodeId: NodeId, ancestorId: NodeId): Boolean {
        return targetNodeId == ancestorId
                || invoke(nodeRepository.getParentNodeId(targetNodeId) ?: return false, ancestorId)
    }
}