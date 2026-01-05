package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeInfo
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * The use case for getting a node info by id
 */
class GetNodeInfoByIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Get node name by id
     *
     * @param id [NodeId] of the node
     * @return the node info with the given [NodeId] or null if the node was not found
     */
    suspend operator fun invoke(id: NodeId): NodeInfo? =
        nodeRepository.getNodeInfoByIdUseCase(id)
}
