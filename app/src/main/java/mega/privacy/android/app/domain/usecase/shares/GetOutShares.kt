package mega.privacy.android.app.domain.usecase.shares

import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaShare

/**
 * Get a list with the active and pending outbound sharings for a MegaNode
 */
fun interface GetOutShares {
    /**
     * Get a list with the active and pending outbound sharings for a MegaNode
     * @param nodeId the [NodeId] of the node to get the outbound sharings
     * @return a list of [MegaShare] of the outbound sharings of the node
     */
    suspend operator fun invoke(nodeId: NodeId): List<MegaShare>?
}