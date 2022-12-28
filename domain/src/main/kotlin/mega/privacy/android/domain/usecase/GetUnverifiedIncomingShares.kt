package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder

/**
 * GetUnverifiedIncomingShares Use case
 */
fun interface GetUnverifiedIncomingShares {

    /**
     * @return Flow of unverified incoming shares
     */
    suspend operator fun invoke(order: SortOrder): List<ShareData>
}