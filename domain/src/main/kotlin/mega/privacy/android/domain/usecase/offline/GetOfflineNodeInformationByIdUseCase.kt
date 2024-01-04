package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get OfflineNodeInformation by Node Id
 *
 */
class GetOfflineNodeInformationByIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * invoke
     * @param nodeId [NodeId]
     */
    suspend operator fun invoke(nodeId: NodeId) =
        nodeRepository.getOfflineNodeInformation(nodeId)
}