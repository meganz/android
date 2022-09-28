package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder

/**
 * Use case interface for getting camera sort order
 */
fun interface GetCameraSortOrder {

    /**
     * Get camera sort order
     * @return camera sort order
     */
    suspend operator fun invoke(): SortOrder
}