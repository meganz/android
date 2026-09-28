package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * Get the access level of the node, used mainly for incoming shares
 */
fun interface GetNodeAccessPermission {
    /**
     * Get the access level of the node
     * @param nodeId [NodeId]
     * @return the [AccessPermission] enum value for this node
     */
    suspend operator fun invoke(nodeId: NodeId): AccessPermission?
}