package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Has ancestor
 */
fun interface HasAncestor {
    /**
     * Invoke
     *
     * @param targetNodeId
     * @param ancestorId
     * @return true if node id appears in the history of the target node else false
     */
    suspend operator fun invoke(targetNodeId: NodeId, ancestorId: NodeId): Boolean
}