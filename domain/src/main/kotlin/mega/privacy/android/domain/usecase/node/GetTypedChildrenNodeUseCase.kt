package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Use case to get typed node children for a given node
 */
class GetTypedChildrenNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addNodeTypeUseCase: AddNodeType,
) {

    /**
     * Get children nodes of a parent node
     *
     * @param parentNodeId Parent [NodeId]
     * @param order [SortOrder] for the returned list
     * @return Children [TypedNode] of the parent node
     */
    suspend operator fun invoke(
        parentNodeId: NodeId,
        order: SortOrder,
    ) = nodeRepository.getNodeChildren(parentNodeId, order).map { addNodeTypeUseCase(it) }
}
