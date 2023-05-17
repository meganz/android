package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Account preferences Gateway
 */
interface AccountPreferencesGateway {
    /**
     * Set show 2FA dialog
     * Is set to true when 2FA dialog need to be shown
     * @param show2FA
     */
    suspend fun setDisplay2FADialog(show2FA: Boolean)

    /**
     * Checks is 2FA dialog should be shown to user
     * @return true if alert should be shown
     */
    fun monitorShow2FADialog(): Flow<Boolean>

    /**
     * Set last target path of copy
     */
    suspend fun setLatestTargetPathCopyPreference(path: Long)

    /**
     * Get last target path of copy
     */
    fun getLatestTargetPathCopyPreference(): Flow<Long?>

    /**
     * Set timestamp of last target path of copy
     */
    suspend fun setLatestTargetTimestampCopyPreference(timestamp: Long)

    /**
     * Get timestamp of last target path of copy
     */
    fun getLatestTargetTimestampCopyPreference(): Flow<Long?>

    /**
     * Set last target path of move
     */
    suspend fun setLatestTargetPathMovePreference(path: Long)

    /**
     * Get last target path of move
     */
    fun getLatestTargetPathMovePreference(): Flow<Long?>

    /**
     * Set timestamp of last target path of move
     */
    suspend fun setLatestTargetTimestampMovePreference(timestamp: Long)

    /**
     * Get timestamp of last target path of move
     */
    fun getLatestTargetTimestampMovePreference(): Flow<Long?>

    /**
     * Clears account preferences.
     */
    suspend fun clearPreferences()
}