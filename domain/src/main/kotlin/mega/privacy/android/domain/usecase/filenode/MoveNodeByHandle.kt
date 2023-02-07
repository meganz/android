package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Use Case to move a MegaNode to a new MegaNode, both referenced by their handles [NodeId]
 */
fun interface MoveNodeByHandle {
    /**
     * Move a MegaNode to a new MegaNode, both referenced by their handles [NodeId]
     */
    suspend operator fun invoke(
        nodeToCopy: NodeId,
        newNodeParent: NodeId,
    )
}