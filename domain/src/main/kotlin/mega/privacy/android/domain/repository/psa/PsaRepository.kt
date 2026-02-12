package mega.privacy.android.domain.repository.psa

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.psa.Psa

/**
 * Psa repository
 */
interface PsaRepository {
    /**
     * Fetch psa
     *
     * @param refreshCache
     * @return latest psa if present
     */
    suspend fun fetchPsa(refreshCache: Boolean): Psa?

    /**
     * Get last psa fetched time
     *
     * @return last time psa was fetched
     */
    suspend fun getLastPsaFetchedTime(): Long?

    /**
     * Set last fetched time
     *
     * @param time
     */
    suspend fun setLastFetchedTime(time: Long?)

    /**
     * Clear cache
     *
     */
    suspend fun clearCache()

    /**
     * Dismiss psa
     *
     * @param psaId
     */
    suspend fun dismissPsa(psaId: Int)

    /**
     * Monitor displayed psa
     *
     * @return flow of displayed psa id or null if none are currently displayed
     */
    fun monitorDisplayedPsa(): Flow<Int?>

    /**
     * Set displayed psa
     *
     * @param psaId pass null to clear
     */
    suspend fun setDisplayedPsa(psaId: Int?)

}
