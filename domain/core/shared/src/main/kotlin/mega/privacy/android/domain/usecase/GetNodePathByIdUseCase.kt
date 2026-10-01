package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

class GetNodePathByIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Get nodes by handles
     *
     * @param id [NodeId] of the node
     * @return The [String] with the path of the given [NodeId]
     */
    suspend operator fun invoke(id: NodeId): String = nodeRepository.getNodePathById(id)
}