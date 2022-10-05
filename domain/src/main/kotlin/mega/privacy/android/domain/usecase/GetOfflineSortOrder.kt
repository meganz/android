package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder

/**
 * Use case interface for getting offline sort order
 */
fun interface GetOfflineSortOrder {

    /**
     * Get offline sort order
     * @return offline sort order
     */
    suspend operator fun invoke(): SortOrder
}