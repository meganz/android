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
    suspend fun setLatestTargetCopyPreference(path: Long)

    /**
     * Get last target path of copy
     */
    fun getLatestTargetCopyPreference(): Flow<Long?>

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
    suspend fun setLatestTargetMovePreference(path: Long)

    /**
     * Get last target path of move
     */
    fun getLatestTargetMovePreference(): Flow<Long?>

    /**
     * Set timestamp of last target path of move
     */
    suspend fun setLatestTargetTimestampMovePreference(timestamp: Long)

    /**
     * Get timestamp of last target path of move
     */
    fun getLatestTargetTimestampMovePreference(): Flow<Long?>

    /**
     * Get the calendar day the storage almost full warning was last shown on for the given trigger
     *
     * @param trigger identifier of the action that raised the warning
     * @return the epoch day, or null if it has never been shown for that trigger
     */
    suspend fun getStorageQuotaWarningShownDay(trigger: String): Long?

    /**
     * Set the calendar day the storage almost full warning was last shown on for the given trigger
     *
     * @param trigger  identifier of the action that raised the warning
     * @param epochDay the calendar day it was shown on
     */
    suspend fun setStorageQuotaWarningShownDay(trigger: String, epochDay: Long)

    /**
     * Get the calendar day the transfer quota running low warning was last shown on for the given
     * trigger
     *
     * @param trigger identifier of the kind of transfer that raised the warning
     * @return the epoch day, or null if it has never been shown for that trigger
     */
    suspend fun getTransferQuotaWarningShownDay(trigger: String): Long?

    /**
     * Set the calendar day the transfer quota running low warning was last shown on for the given
     * trigger
     *
     * @param trigger  identifier of the kind of transfer that raised the warning
     * @param epochDay the calendar day it was shown on
     */
    suspend fun setTransferQuotaWarningShownDay(trigger: String, epochDay: Long)

    /**
     * Get the time the account details were last fetched
     *
     * @return the timestamp in seconds, or null if they have not been fetched since the last reset
     */
    suspend fun getAccountDetailsTimeStamp(): Long?

    /**
     * Set the time the account details were last fetched
     *
     * @param timestamp the timestamp in seconds
     */
    suspend fun setAccountDetailsTimeStamp(timestamp: Long)

    /**
     * Clear the account details timestamp, so the account details are treated as stale
     */
    suspend fun clearAccountDetailsTimeStamp()

    /**
     * Get the time the extended account details were last fetched
     *
     * @return the timestamp in seconds, or null if they have not been fetched since the last reset
     */
    suspend fun getExtendedAccountDetailsTimeStamp(): Long?

    /**
     * Set the time the extended account details were last fetched
     *
     * @param timestamp the timestamp in seconds
     */
    suspend fun setExtendedAccountDetailsTimeStamp(timestamp: Long)

    /**
     * Clear the extended account details timestamp, so the extended account details are treated
     * as stale
     */
    suspend fun clearExtendedAccountDetailsTimeStamp()

    /**
     * Get whether the copyright agreement should be shown before creating a link
     *
     * @return the stored value, or null if it has not been stored yet
     */
    suspend fun getShowCopyright(): Boolean?

    /**
     * Set whether the copyright agreement should be shown before creating a link
     *
     * @param show true to show it, false otherwise
     */
    suspend fun setShowCopyright(show: Boolean)

    /**
     * Clears account preferences except last registered email
     */
    suspend fun clearPreferences()

    /**
     * Monitor last registered email
     */
    fun monitorLastRegisteredEmail(): Flow<String?>

    /**
     * Set last registered email
     * @param email [String]
     */
    suspend fun setLastRegisteredEmail(email: String)

    /**
     * Clear last registered email
     */
    suspend fun clearLastRegisteredEmail()
}