package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Use Case to move a MegaNode to the rubbish bin, referenced by its handle [NodeId]
 */
fun interface MoveNodeToRubbishByHandle {
    /**
     * Moves a MegaNode referenced by its handle [NodeId] to a the rubbish bin
     * @param nodeToMove the node's handle [NodeId] that we want to move to the rubbish bin
     */
    suspend operator fun invoke(
        nodeToMove: NodeId,
    )
}