package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Stop sharing a node
 */
fun interface StopSharingNode {
    /**
     * Stop sharing a node
     * @param nodeId the [NodeId] of the node we want to stop sharing
     */
    suspend operator fun invoke(nodeId: NodeId)
}