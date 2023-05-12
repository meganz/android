package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.login.EphemeralCredentials

/**
 * Ephemeral credentials gateway
 *
 */
interface EphemeralCredentialsGateway {
    /**
     * Save
     *
     * @param ephemeral
     */
    suspend fun save(ephemeral: EphemeralCredentials)

    /**
     * Clear
     *
     */
    suspend fun clear()

    /**
     * Monitor ephemeral credentials
     *
     */
    fun monitorEphemeralCredentials(): Flow<EphemeralCredentials?>
}