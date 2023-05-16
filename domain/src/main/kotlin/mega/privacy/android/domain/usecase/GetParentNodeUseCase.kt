package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * The use case for getting parent node by nodeId
 */
class GetParentNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Get parent node by handle
     *
     * @param nodeId [NodeId]
     * @return [UnTypedNode]?
     */
    suspend operator fun invoke(nodeId: NodeId) =
        nodeRepository.getParentNode(nodeId)
}
