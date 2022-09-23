package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.ShareData

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
    suspend fun getOutgoingSharesNode(order: Int): List<ShareData>
}