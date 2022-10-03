package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder

/**
 * Use case interface for setting camera sort order
 */
fun interface SetCameraSortOrder {

    /**
     * Set camera sort order
     * @param order
     */
    suspend operator fun invoke(order: SortOrder)
}