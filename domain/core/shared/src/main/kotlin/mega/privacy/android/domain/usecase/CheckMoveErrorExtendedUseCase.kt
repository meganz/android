package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Checks if node can be moved to target node
 */
class CheckNodeCanBeMovedToTargetNode @Inject constructor(
    private val repository: NodeRepository,
) {
    /**
     * Invoke.
     *
     * @param nodeId
     * @param targetNodeId
     * */
    suspend operator fun invoke(nodeId: NodeId, targetNodeId: NodeId): Boolean =
        repository.checkNodeCanBeMovedToTargetNode(nodeId = nodeId, targetNodeId = targetNodeId)
}