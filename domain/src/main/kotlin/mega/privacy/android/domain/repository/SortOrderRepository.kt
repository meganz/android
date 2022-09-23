package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.SortOrder

/**
 * Sort Order repository
 *
 */
interface SortOrderRepository {
    /**
     * Get camera sort order
     * @return camera sort order
     */
    suspend fun getCameraSortOrder(): SortOrder
}