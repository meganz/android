package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Use Case to move a MegaNode to a new MegaNode, both referenced by their handles [NodeId]
 */
fun interface CopyNodeByHandleChangingName {
    /**
     * Move a MegaNode to a new MegaNode, both referenced by their handles [NodeId]
     * @param nodeToCopy the [NodeId] handle to copy
     * @param newNodeParent the [NodeId] handle that [nodeToCopy] will be moved to
     * @param newNodeName the new name for [nodeToCopy] once it is moved to [newNodeParent]
     *
     * @return the [NodeId] handle of the created MegaNode
     */
    suspend operator fun invoke(
        nodeToCopy: NodeId,
        newNodeParent: NodeId,
        newNodeName: String,
    ): NodeId
}