package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to copy a TypedNode and move it to a new Node while updating its name
 */
class CopyTypedNodeUseCase @Inject constructor(private val nodeRepository: NodeRepository) {

    /**
     * Copy a MegaNode and move it to a new MegaNode while updating its name
     *
     * @param nodeToCopy the TypedNode to copy
     * @param newNodeParent the Node that [nodeToCopy] will be moved to
     * @param newNodeName the new name for [nodeToCopy] once it is moved to [newNodeParent]
     *
     * @return the handle of the new Node that was copied
     */
    suspend operator fun invoke(
        nodeToCopy: TypedNode,
        newNodeParent: NodeId,
        newNodeName: String? = null,
    ) = nodeRepository.copyNode(nodeToCopy, newNodeParent, newNodeName)
}
