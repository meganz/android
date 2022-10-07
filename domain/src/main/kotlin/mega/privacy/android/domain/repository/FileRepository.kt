package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder

/**
 * File repository
 *
 */
interface FileRepository {
    /**
     * Get a list of all outgoing shares
     *
     * @param order sort order, if null the default order is applied
     * @return List of MegaNode of all active and pending outbound shared by current user
     */
    suspend fun getOutgoingSharesNode(order: SortOrder): List<ShareData>


    /**
     * check whether the node is in rubbish bin or not
     *
     * @return Boolean
     */
    suspend fun isNodeInRubbish(handle: Long): Boolean
}