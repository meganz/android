package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to delete a MegaNode, referenced by its handle [NodeId]
 */
class DeleteNodeByHandleUseCase @Inject constructor(private val nodeRepository: NodeRepository) {
    /**
     * Deletes a MegaNode referenced by its handle [NodeId]
     * @param nodeToDelete the node's handle [NodeId] that we want to delete
     */
    suspend operator fun invoke(
        nodeToDelete: NodeId,
    ) = nodeRepository.deleteNodeByHandle(nodeToDelete)
}