package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject


/**
 * Check if node is in the rubbish bin
 * In case the node does not exist, returns false
 */
class IsNodeInRubbishBinUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * @param nodeId
     * @return true if the node is in the rubbish bin
     *         In case the node does not exist, return false
     */
    suspend operator fun invoke(nodeId: NodeId): Boolean =
        nodeRepository.isNodeInRubbishBin(nodeId)
}
