package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder

/**
 * Use case interface for setting offline sort order
 */
fun interface SetOfflineSortOrder {

    /**
     * Set offline sort order
     * @param order
     */
    suspend operator fun invoke(order: SortOrder)
}