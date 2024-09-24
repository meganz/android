package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.recentactions.NodeInfoForRecentActions


/**
 * Recent actions repository
 */
interface RecentActionsRepository {

    /**
     * Gets the recent actions.
     *
     * The recommended values for days and maxNodes parameters are to consider
     * interactions during the last 30 days and maximum 500 nodes. So they are set by default.
     *
     * @param excludeSensitives exclude sensitive nodes
     *
     * @return a list of recent actions.
     */
    suspend fun getRecentActions(
        excludeSensitives: Boolean,
    ): List<RecentActionBucketUnTyped>

    /**
     * Gets the node info required for recent action only
     *
     * @param nodeId [NodeId]
     * @return [NodeInfoForRecentActions]
     */
    suspend fun getNodeInfo(nodeId: NodeId): NodeInfoForRecentActions?
}
