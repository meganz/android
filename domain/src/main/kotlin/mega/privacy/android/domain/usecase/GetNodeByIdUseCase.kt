package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * The use case for getting a [TypedNode] by its nodeId
 */
class GetNodeByIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Get nodes by handles
     *
     * @param id [NodeId] of the node
     * @return the [TypedNode] with the given [NodeId] or null if the node was not found
     */
    suspend operator fun invoke(id: NodeId): TypedNode? =
        nodeRepository.getNodeById(id)?.let {
            addNodeType(it as UnTypedNode)
        }
}