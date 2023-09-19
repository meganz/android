package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow

/**
 * UI preferences gateway
 *
 */
interface UIPreferencesGateway {
    /**
     * Monitor preferred start screen
     *
     * @return preferred start screen
     */
    fun monitorPreferredStartScreen(): Flow<Int?>

    /**
     * Set preferred start screen
     *
     * @param value
     */
    suspend fun setPreferredStartScreen(value: Int)

    /**
     * Monitor the View type
     *
     * @return a [Flow] to observe the View type
     */
    fun monitorViewType(): Flow<Int?>

    /**
     * Set the new View type
     *
     * @param value An [Integer] representing a new View type
     */
    suspend fun setViewType(value: Int)

    /**
     * Monitor hide recent activity
     *
     * @return true if recent activity should be hidden, else false
     */
    fun monitorHideRecentActivity(): Flow<Boolean?>

    /**
     * Monitor media discovery view
     *
     * @return media discovery state
     */
    fun monitorMediaDiscoveryView(): Flow<Int?>

    /**
     * Set hide recent activity
     *
     * @param value
     */
    suspend fun setHideRecentActivity(value: Boolean)

    /**
     * Set media discovery view
     *
     * @param value
     */
    suspend fun setMediaDiscoveryView(value: Int)

    /**
     * Monitor subfolder media discovery setting
     *
     * @return subfolder media discovery option enabled status as a flow
     */
    fun monitorSubfolderMediaDiscoveryEnabled(): Flow<Boolean?>

    /**
     * set subfolder media discovery  enabled
     *
     * @param enabled
     */
    suspend fun setSubfolderMediaDiscoveryEnabled(enabled: Boolean)


    /**
     * Set offline warning message visibility
     * @param isVisible the visibility of the view to set
     */
    suspend fun setOfflineWarningMessageVisibility(isVisible: Boolean)

    /**
     * Monitor the offline warning message visibility
     *
     * @return the view visibility state
     */
    fun monitorOfflineWarningMessageVisibility(): Flow<Boolean?>

}