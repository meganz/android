package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case for getting the access level of the node
 */
class GetNodeAccessUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Get the access level of the node
     * @param nodeId [NodeId]
     * @return the [AccessPermission] enum value for this node
     */
    suspend operator fun invoke(nodeId: NodeId) = nodeRepository.getNodeAccessPermission(nodeId)
}