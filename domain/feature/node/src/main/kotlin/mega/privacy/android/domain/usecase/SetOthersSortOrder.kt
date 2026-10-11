package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder

/**
 * Use case interface for setting others sort order
 */
fun interface SetOthersSortOrder {

    /**
     * Set others sort order
     * @param order
     */
    suspend operator fun invoke(order: SortOrder)
}