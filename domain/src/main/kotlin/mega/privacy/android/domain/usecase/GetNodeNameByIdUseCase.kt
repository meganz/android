package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * The use case for getting a node name by id
 */
class GetNodeNameByIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Get node name by id
     *
     * @param id [NodeId] of the node
     * @return the name of the node with the given [NodeId] or null if the node was not found
     */
    suspend operator fun invoke(id: NodeId): String? =
        nodeRepository.getNodeNameById(id)
}
