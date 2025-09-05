package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.advertisements.AdDetails

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
    ): List<AdDetails>


    /**
     * Monitor ads closing timestamp
     */
    fun monitorAdsClosingTimestamp(): Flow<Long?>

    /**
     * Set ads closing timestamp
     */
    suspend fun setAdsClosingTimestamp(timestamp: Long)

    /**
     * Query ads
     */
    suspend fun queryAds(linkHandle: Long): Boolean

    /**
     * Set Google consent loaded state
     */
    fun setGoogleConsentLoaded(isLoaded: Boolean)

    /**
     * Monitor Google consent loaded state
     */
    fun monitorGoogleConsentLoaded(): Flow<Boolean>
}