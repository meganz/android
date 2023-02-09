package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Use Case to move a MegaNode to a new MegaNode, both referenced by their handles [NodeId]
 */
fun interface MoveNodeByHandle {
    /**
     * Move a MegaNode to a new MegaNode, both referenced by their handles [NodeId]
     * @param nodeToMove the node's handle [NodeId] that we want to move
     * @param newNodeParent the folder node's handle [NodeId] where we want to move the node
     * @return the [NodeId] of the moved node
     */
    suspend operator fun invoke(
        nodeToMove: NodeId,
        newNodeParent: NodeId,
    ): NodeId
}