package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Check whether a node is available offline because the user made it, or one of its ancestors,
 * available offline.
 *
 * Folders are also stored offline to mirror the cloud structure whenever a descendant is made
 * available offline. Those folders are not available offline themselves.
 */
class IsNodeUnderOfflineRootUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param nodeId [NodeId] of the node
     */
    suspend operator fun invoke(nodeId: NodeId) = nodeRepository.isNodeUnderOfflineRoot(nodeId)
}
