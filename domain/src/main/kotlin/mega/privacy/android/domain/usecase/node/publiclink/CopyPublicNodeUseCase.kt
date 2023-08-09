package mega.privacy.android.domain.usecase.node.publiclink

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to copy a public node and move it to a new Node while updating its name
 */
class CopyPublicNodeUseCase @Inject constructor(private val nodeRepository: NodeRepository) {
    /**
     * Copy a MegaNode and move it to a new MegaNode while updating its name
     *
     * @param publicNodeToCopy the Node to copy
     * @param newNodeParent the Node that [publicNodeToCopy] will be moved to
     * @param newNodeName the new name for [publicNodeToCopy] once it is moved to [newNodeParent]
     *
     * @return the handle of the new Node that was copied
     */
    suspend operator fun invoke(
        publicNodeToCopy: Node,
        newNodeParent: NodeId,
        newNodeName: String?,
    ) = nodeRepository.copyPublicNode(publicNodeToCopy, newNodeParent, newNodeName)
}