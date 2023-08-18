package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * GetIncomingSharesParentUserEmailUseCase
 *
 * Retrieves email of the parent user who shared the folder with you
 * Method can be called from any node level
 */
class GetIncomingShareParentUserEmailUseCase @Inject constructor(
    private val nodeRepository: NodeRepository
) {
    /**
     * Invoke
     *
     * @param nodeId node handle from which we need to get the parent user's email
     * @return email if node is found
     * @return null if node not found
     */
    suspend operator fun invoke(nodeId: NodeId) =
        nodeRepository.getIncomingShareParentUserEmail(nodeId)
}