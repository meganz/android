package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder

/**
 * Use case interface for getting links sort order
 */
fun interface GetLinksSortOrder {

    /**
     * Get links sort order
     * @return links sort order
     */
    suspend operator fun invoke(): SortOrder
}