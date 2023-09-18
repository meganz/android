package mega.privacy.android.domain.repository.psa

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

}
