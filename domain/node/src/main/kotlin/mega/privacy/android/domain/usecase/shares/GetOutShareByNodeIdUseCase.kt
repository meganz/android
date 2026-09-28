package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get Out Share By Node Id Use Case
 *
 */
class GetOutShareByNodeIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(id: NodeId) = nodeRepository.getNodeOutgoingShares(id)
}