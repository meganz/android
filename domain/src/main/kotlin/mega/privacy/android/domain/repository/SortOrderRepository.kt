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

    /**
     * Get cloud sort order
     * @return cloud sort order
     */
    suspend fun getCloudSortOrder(): SortOrder

    /**
     * Get links cloud sort order
     * @return links cloud sort order
     */
    suspend fun getLinksSortOrder(): SortOrder

    /**
     * Get others sort order
     * @return others sort order
     */
    suspend fun getOthersSortOrder(): SortOrder

    /**
     * Get offline sort order
     * @return offline sort order
     */
    suspend fun getOfflineSortOrder(): SortOrder

    /**
     * Set offline sort order
     * @param order
     */
    suspend fun setOfflineSortOrder(order: SortOrder)
}