package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ConnectivityState

/**
 * Network repository
 *
 */
interface NetworkRepository {
    /**
     * Get current connectivity state
     *
     * @return the current connectivity state
     */
    fun getCurrentConnectivityState(): ConnectivityState

    /**
     * Monitor connectivity changes
     *
     * @return a flow that emits a value anytime the connectivity state changes
     */
    fun monitorConnectivityChanges(): Flow<ConnectivityState>

    /**
     * Set use https
     *
     * @param enabled
     */
    fun setUseHttps(enabled: Boolean)

    /**
     * Is Metered Connection
     *
     * @return [Boolean]
     */
    fun isMeteredConnection(): Boolean?

    /**
     * Is Currently on WIFI
     *
     * @return [Boolean]
     */
    fun isOnWifi(): Boolean
}
