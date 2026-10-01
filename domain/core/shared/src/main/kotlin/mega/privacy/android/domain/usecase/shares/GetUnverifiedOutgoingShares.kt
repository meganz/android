package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder

/**
 * GetUnverifiedOutgoingShares Use case
 */
fun interface GetUnverifiedOutgoingShares {

    /**
     * @return Flow of unverified outgoing shares
     */
    suspend operator fun invoke(order: SortOrder): List<ShareData>
}
