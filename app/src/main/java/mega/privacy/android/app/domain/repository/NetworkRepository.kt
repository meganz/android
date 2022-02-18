package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.ConnectivityState

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
    fun monitorConnectivityChanges() : Flow<ConnectivityState>
}