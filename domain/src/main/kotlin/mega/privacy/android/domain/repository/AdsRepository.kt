package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.advertisements.AdDetail

/**
 * The repository interface regarding in-app ads
 */
interface AdsRepository {
    /**
     * fetch list of AdDetails containing URL for each slot
     */
    suspend fun fetchAdDetails(
        adSlots: List<String>,
        linkHandle: Long?,
    ): List<AdDetail>
}