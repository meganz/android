package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get child [UnTypedNode]
 */
class GetChildNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Get the child node with the provided name
     *
     * @param parentNodeId
     * @param name
     * @return [UnTypedNode] or null if doesn't exist
     */
    suspend operator fun invoke(parentNodeId: NodeId?, name: String?) =
        nodeRepository.getChildNode(parentNodeId, name)
}
