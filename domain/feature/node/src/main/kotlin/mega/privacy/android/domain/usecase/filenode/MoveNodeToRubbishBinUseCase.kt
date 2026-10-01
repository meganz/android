package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to move a MegaNode to the rubbish bin, referenced by its handle [NodeId]
 */
class MoveNodeToRubbishBinUseCase @Inject constructor(private val nodeRepository: NodeRepository) {
    /**
     * Moves a MegaNode referenced by its handle [NodeId] to a the rubbish bin
     * @param nodeToMove the node's handle [NodeId] that we want to move to the rubbish bin
     */
    suspend operator fun invoke(
        nodeToMove: NodeId,
    ) {
        nodeRepository.moveNodeToRubbishBinByHandle(nodeToMove)
    }
}