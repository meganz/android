package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Use Case to delete a MegaNode's history versions, referenced by its handle [NodeId]
 */
fun interface DeleteNodeVersionsByHandle {
    /**
     * Deletes a MegaNode's history versions referenced by its handle [NodeId]
     * Only last version will be keep
     * @param nodeToDeleteVersions [NodeId] handle of the node whose history we want to delete
     */
    suspend operator fun invoke(
        nodeToDeleteVersions: NodeId,
    )
}