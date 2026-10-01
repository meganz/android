package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder

/**
 * Use case interface for getting others sort order
 */
fun interface GetOthersSortOrder {

    /**
     * Get others sort order
     * @return others sort order
     */
    suspend operator fun invoke(): SortOrder
}