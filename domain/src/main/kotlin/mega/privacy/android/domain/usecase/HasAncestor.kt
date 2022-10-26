package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Has ancestor
 */
fun interface HasAncestor {
    /**
     * Invoke
     *
     * @param targetNode
     * @param ancestorId
     * @return true if node id appears in the history of the target node else false
     */
    suspend operator fun invoke(targetNode: NodeId, ancestorId: NodeId): Boolean
}