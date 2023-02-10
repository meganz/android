package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Use Case to delete a MegaNode, referenced by its handle [NodeId]
 */
fun interface DeleteNodeByHandle {
    /**
     * Deletes a MegaNode referenced by its handle [NodeId]
     * @param nodeToDelete the node's handle [NodeId] that we want to delete
     */
    suspend operator fun invoke(
        nodeToDelete: NodeId,
    )
}