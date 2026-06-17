package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case that checks whether a node is accessible by probing its download URL.
 *
 * Throws [mega.privacy.android.domain.exception.BlockedMegaException] if the node is taken down.
 */
class CheckNodeAccessibilityUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * @param nodeId [NodeId] of the node to check
     * @throws [mega.privacy.android.domain.exception.BlockedMegaException] if the node is taken down (API_EBLOCKED)
     * @throws [mega.privacy.android.domain.exception.MegaException] for other non-OK API errors
     */
    suspend operator fun invoke(nodeId: NodeId) = nodeRepository.checkNodeAccessibility(nodeId)
}
