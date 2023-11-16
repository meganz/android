package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow

/**
 * App info preferences gateway
 *
 */
interface AppInfoPreferencesGateway {
    /**
     * Set last version code
     *
     * @param versionCode
     */
    suspend fun setLastVersionCode(versionCode: Int)

    /**
     * Monitor last version code
     *
     */
    fun monitorLastVersionCode(): Flow<Int>

    /**
     * Set is first launch
     *
     * @param isFirstLaunch
     */
    suspend fun setIsFirstLaunch(isFirstLaunch: Boolean)

    /**
     * Monitor is first launch
     *
     * @return value  of first launch boolean or null if not set
     */
    fun monitorIsFirstLaunch(): Flow<Boolean?>
}