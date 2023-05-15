package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to move a MegaNode to a new MegaNode, both referenced by their [NodeId]
 */
class MoveNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Move a MegaNode to a new MegaNode, both referenced by their [NodeId]
     * @param nodeToMove the node's [NodeId] that we want to move
     * @param newNodeParent the folder node's [NodeId] where we want to move the node
     * @return the [NodeId] of the moved node
     */
    suspend operator fun invoke(
        nodeToMove: NodeId,
        newNodeParent: NodeId,
    ): NodeId = nodeRepository.moveNode(nodeToMove, newNodeParent, null)
}