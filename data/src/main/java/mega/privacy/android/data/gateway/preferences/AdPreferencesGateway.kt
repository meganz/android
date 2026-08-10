package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Gateway for ad-related preferences persistence.
 */
interface AdPreferencesGateway {

    /**
     * Atomically increment the rewarded ad attempt count by 1.
     */
    suspend fun incrementRewardedAdAttemptCount()

    /**
     * Reset the rewarded ad attempt count back to 0.
     */
    suspend fun resetRewardedAdAttemptCount()

    /**
     * Monitor the rewarded ad attempt count.
     */
    fun monitorRewardedAdAttemptCount(): Flow<Int>
}
