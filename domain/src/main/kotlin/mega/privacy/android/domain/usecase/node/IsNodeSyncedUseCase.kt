package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Check if node is synced (root of a Sync)
 */
class IsNodeSyncedUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * @param nodeId Node ID
     * @return True if the node is synced or False otherwise
     */
    suspend operator fun invoke(nodeId: NodeId): Boolean =
        nodeRepository.isNodeSynced(nodeId)
}