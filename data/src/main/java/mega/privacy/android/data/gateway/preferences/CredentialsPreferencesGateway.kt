package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.user.UserCredentials

/**
 * Credentials Preferences Gateway
 *
 */
internal interface CredentialsPreferencesGateway {
    /**
     * Save
     *
     * @param credentials
     */
    suspend fun save(credentials: UserCredentials)

    /**
     * Save first name
     *
     * @param firstName
     */
    suspend fun saveFirstName(firstName: String)

    /**
     * Save last name
     *
     * @param lastName
     */
    suspend fun saveLastName(lastName: String)

    /**
     * Clear
     *
     */
    suspend fun clear()

    /**
     * Monitor credentials
     *
     */
    fun monitorCredentials(): Flow<UserCredentials?>
}