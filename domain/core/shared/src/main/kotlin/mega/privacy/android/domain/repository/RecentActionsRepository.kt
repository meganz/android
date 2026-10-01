package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
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
     * @param maxBucketCount maximum number of buckets
     *
     * @return a list of recent actions.
     */
    suspend fun getRecentActions(
        excludeSensitives: Boolean,
        maxBucketCount: Int,
    ): List<RecentActionBucketUnTyped>

    /**
     * Clear the recent actions up to given timestamp
     *
     * @param until Epoch time (in seconds). Recent actions up to this time will be cleared.
     * @return The timestamp up to which recent actions were cleared
     */
    suspend fun clearRecentActions(until: Long): Long

    /**
     * Monitor when recent activity has been cleared
     *
     * @return a flow that emits [Unit] whenever recent actions are cleared
     */
    fun monitorRecentActivityCleared(): Flow<Unit>

    /**
     * Gets the node info required for recent action only
     *
     * @param nodeId [NodeId]
     * @return [NodeInfoForRecentActions]
     */
    suspend fun getNodeInfo(nodeId: NodeId): NodeInfoForRecentActions?

    /**
     * Gets a single recent action bucket by its identifier.
     *
     * This method is optimized to only fetch nodes for the matching bucket,
     * avoiding unnecessary node fetching for all buckets.
     *
     * @param id The unique identifier of the bucket
     * @param excludeSensitives Exclude sensitive nodes
     * @return The matching [RecentActionBucketUnTyped] or null if not found
     */
    suspend fun getRecentActionBucketById(
        id: String,
        excludeSensitives: Boolean,
    ): RecentActionBucketUnTyped?
}
