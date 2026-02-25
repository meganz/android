package mega.privacy.android.domain.repository.psa

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.psa.Psa

/**
 * Psa repository
 */
interface PsaRepository {

    /**
     * Refresh psa
     *
     */
    suspend fun refreshPsa(): Unit

    /**
     * Monitor psa
     */
    fun monitorPsa(): Flow<Psa?>

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
