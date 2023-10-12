package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * NodeAccessPermissionCheckUseCase
 *
 * checks if the node has the required permission to modify the node
 */
class NodeAccessPermissionCheckUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invocation
     *
     * @param nodeId
     * @param level
     */
    suspend operator fun invoke(nodeId: NodeId, level: AccessPermission) =
        nodeRepository.checkIfNodeHasTheRequiredAccessLevelPermission(
            nodeId = nodeId,
            level = level
        )
}