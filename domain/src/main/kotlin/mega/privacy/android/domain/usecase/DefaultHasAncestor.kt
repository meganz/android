package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Default has ancestor
 *
 * @property fileSystemRepository
 */
class DefaultHasAncestor @Inject constructor(
    private val nodeRepository: NodeRepository,
) : HasAncestor {

    override suspend fun invoke(targetNodeId: NodeId, ancestorId: NodeId): Boolean {
        val node = nodeRepository.getNodeById(targetNodeId) ?: return false
        val currentId = node.id
        val parentId = node.parentId
        return currentId == ancestorId || invoke(parentId, ancestorId)
    }
}