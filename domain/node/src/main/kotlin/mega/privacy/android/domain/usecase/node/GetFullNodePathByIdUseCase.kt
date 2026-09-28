package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case for getting full node path by id.
 */
class GetFullNodePathByIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke.
     *
     * @param id [NodeId] of the node
     * @return The [String] with the full path of the given [NodeId]
     */
    suspend operator fun invoke(id: NodeId) = nodeRepository.getFullNodePathById(id)
}