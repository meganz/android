package mega.privacy.android.app.data.gateway.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Logging preferences gateway
 *
 */
interface LoggingPreferencesGateway {
    /**
     * Is logging preference enabled
     *
     * @return
     */
    fun isLoggingPreferenceEnabled(): Flow<Boolean>

    /**
     * Set logging enabled preference
     *
     * @param enabled
     */
    suspend fun setLoggingEnabledPreference(enabled: Boolean)

    /**
     * Is chat logging preference enabled
     *
     * @return
     */
    fun isChatLoggingPreferenceEnabled(): Flow<Boolean>

    /**
     * Set chat logging enabled preference
     *
     * @param enabled
     */
    suspend fun setChatLoggingEnabledPreference(enabled: Boolean)
}