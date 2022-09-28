package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder

/**
 * Use case interface for getting cloud sort order
 */
fun interface GetCloudSortOrder {

    /**
     * Get cloud sort order
     * @return cloud sort order
     */
    suspend operator fun invoke(): SortOrder
}