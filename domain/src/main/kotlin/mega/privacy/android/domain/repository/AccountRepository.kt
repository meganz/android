package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.exception.MegaException

/**
 * Account repository
 */
interface AccountRepository {
    /**
     * Get user account
     *
     * @return the user account for the current user
     */
    suspend fun getUserAccount(): UserAccount

    /**
     * Storage capacity used is blank
     *
     */
    fun storageCapacityUsedIsBlank(): Boolean

    /**
     * Request account
     * Sends a request to update account data asynchronously
     */
    suspend fun requestAccount()

    /**
     * Set that the user has logged in
     */
    suspend fun setUserHasLoggedIn()

    /**
     * Is multi factor auth available
     *
     * @return true if multi-factor auth is available for the current user, else false
     */
    fun isMultiFactorAuthAvailable(): Boolean

    /**
     * Is multi factor auth enabled
     *
     * @return true if multi-factor auth is enabled for the current user, else false
     */
    @Throws(MegaException::class)
    suspend fun isMultiFactorAuthEnabled(): Boolean

    /**
     * Request delete account link
     *
     * Sends a delete account link to the user's email address
     *
     */
    suspend fun requestDeleteAccountLink()

    /**
     * Monitor user updates
     *
     * @return a flow of all global user updates
     */
    fun monitorUserUpdates(): Flow<UserUpdate>

    /**
     * Gets the number of unread user alerts for the logged in user.
     *
     * @return Number of unread user alerts.
     */
    suspend fun getNumUnreadUserAlerts(): Int

    /**
     * Gets user account credentials.
     *
     * @return User credentials if exists, null otherwise.
     */
    suspend fun getSession(): String?

    /**
     * Refreshes DNS servers and retries chat pending connections.
     *
     * @param disconnect True if should disconnect megaChatApi, false otherwise.
     */
    suspend fun retryChatPendingConnections(disconnect: Boolean)

    /**
     * Get the List of SubscriptionOptions
     *
     * @return List of SubscriptionOptions
     */
    suspend fun getSubscriptionOptions(): List<SubscriptionOption>

    /**
     * Returns if accounts achievements enabled
     */
    suspend fun areAccountAchievementsEnabled(): Boolean

    /**
     * Get account achievements
     *
     * @return MegaAchievement
     */
    suspend fun getAccountAchievements(
        achievementType: AchievementType,
        awardIndex: Long,
    ): MegaAchievement

    /**
     * Get account details time stamp
     *
     * @return the latest account detail time stamp
     */
    suspend fun getAccountDetailsTimeStampInSeconds(): String?

    /**
     * Get extended account details time stamp
     *
     * @return the latest account detail time stamp
     */
    suspend fun getExtendedAccountDetailsTimeStampInSeconds(): String?

    /**
     * Get specific account detail
     *
     * @param storage
     * @param transfer
     * @param pro
     */
    suspend fun getSpecificAccountDetail(storage: Boolean, transfer: Boolean, pro: Boolean)

    /**
     * Get extended account details
     *
     * @param sessions
     * @param purchases
     * @param transactions
     */
    suspend fun getExtendedAccountDetails(
        sessions: Boolean,
        purchases: Boolean,
        transactions: Boolean,
    )

    /**
     * Gets the credentials of the currently open account.
     *
     * @return Fingerprint of the signing key of the current account.
     */
    suspend fun getMyCredentials(): AccountCredentials.MyAccountCredentials?

    /**
     * Reset account details time stamp
     *
     */
    suspend fun resetAccountDetailsTimeStamp()

    /**
     * Reset extended account details timestamp
     *
     */
    suspend fun resetExtendedAccountDetailsTimestamp()

    /**
     * Create a contact link
     *
     * @param renew – True to invalidate the previous contact link (if any).
     * @return string of contact link.
     */
    suspend fun createContactLink(renew: Boolean): String

    /**
     * Delete a contact link
     *
     * @param handle   Handle of the contact link to delete
     *                 If the parameter is INVALID_HANDLE, the active contact link is deleted
     */
    suspend fun deleteContactLink(handle: Long)

    /**
     * Get an overview of all the existing achievements and rewards for current account
     */
    suspend fun getAccountAchievementsOverview(): AchievementsOverview

    /**
     * Registered email of current account
     * @return email address or null
     */
    suspend fun getAccountEmail(forceRefresh: Boolean = true): String?

    /**
     * Monitor account detail
     *
     */
    fun monitorAccountDetail(): Flow<AccountDetail>

    /**
     * Checks if User is Logged In
     */
    suspend fun isUserLoggedIn(): Boolean

    /**
     * Saves the UserCredentials of the current logged in account and clears ephemeral.
     *
     * @return [AccountSession]
     */
    suspend fun saveAccountCredentials(): AccountSession

    /**
     * Gets the credentials of the current logged in account.
     *
     * @return [UserCredentials]
     */
    suspend fun getAccountCredentials(): UserCredentials?

    /**
     * Change email
     *
     * @param email the new user email
     * @return new user email from sdk
     */
    suspend fun changeEmail(email: String): String

    /**
     * Queries a signup link.
     *
     * @param signupLink Signup link.
     * @return The email related to the link.
     */
    suspend fun querySignupLink(signupLink: String): String

    /**
     * Stops using the current authentication token, it's needed to explicitly call
     * set accountAuth as NULL as parameter. Otherwise, the value set would continue
     * being used despite this MegaApi object is logged in or logged out.s account
     */
    suspend fun resetAccountAuth()

    /**
     * Clears the account data and preferences.
     */
    suspend fun clearAccountPreferences()

    /**
     * Clears shared preferences.
     */
    suspend fun clearSharedPreferences()

    /**
     * Clears app data and cache.
     */
    suspend fun clearAppDataAndCache()

    /**
     * Cancels all notifications.
     */
    suspend fun cancelAllNotifications()

    /**
     * Check whether password is user's current password
     * @param password as password to check
     * @return true if password is the same as current password
     */
    suspend fun isCurrentPassword(password: String): Boolean

    /**
     * Change the given user's password
     * @param newPassword as user's chosen new password
     * @return true if successful
     */
    suspend fun changePassword(newPassword: String): Boolean

    /**
     * Reset the user's password from a link
     * @param link as reset link
     * @param newPassword as user's chosen new password
     * @param masterKey as user's account master key
     * @return true if successful, else false
     */
    suspend fun resetPasswordFromLink(
        link: String?,
        newPassword: String,
        masterKey: String?,
    ): Boolean

    /**
     * Check the given password's strength
     * @param password as password to test
     * @return password strength level from 0 - 4
     */
    suspend fun getPasswordStrength(password: String): PasswordStrength

    /**
     * Resets account info.
     */
    suspend fun resetAccountInfo()

    /**
     * update 2FA dialog preference
     */
    suspend fun update2FADialogPreference(show2FA: Boolean)

    /**
     * get 2FA dialog preference
     */
    suspend fun get2FADialogPreference(): Boolean

    /**
     * Checks if user already enabled 2FA
     */
    suspend fun is2FAEnabled(): Boolean

    /**
     * Set last target path of copy
     */
    suspend fun setLatestTargetPathCopyPreference(path: Long)

    /**
     * Get last target path of copy
     */
    suspend fun getLatestTargetPathCopyPreference(): Long?

    /**
     * Set last target path of move
     */
    suspend fun setLatestTargetPathMovePreference(path: Long)

    /**
     * Get last target path of move
     */
    suspend fun getLatestTargetPathMovePreference(): Long?

    /**
     *  Notify the user has successfully skipped the password check
     */
    suspend fun skipPasswordReminderDialog()

    /**
     * Notify the user wants to totally disable the password check
     */
    suspend fun blockPasswordReminderDialog()

    /**
     * Notify the user has successfully checked his password
     */
    suspend fun notifyPasswordChecked()

    /**
     * Update cryptographic security
     */
    suspend fun upgradeSecurity()

    /**
     * Sets the secure share flag to true or false
     *
     * @param enable : Boolean
     */
    suspend fun setSecureFlag(enable: Boolean)

    /**
     * Monitor update upgrade security events set in app
     *
     * @return
     */
    fun monitorSecurityUpgrade(): Flow<Boolean>

    /**
     * Set upgrade security in app
     *
     * @param isSecurityUpgrade
     */
    suspend fun setUpgradeSecurity(isSecurityUpgrade: Boolean)

    /**
     * Monitor my account update
     */
    fun monitorMyAccountUpdate(): Flow<MyAccountUpdate>

    /**
     * Broadcast my account update
     */
    suspend fun broadcastMyAccountUpdate(data: MyAccountUpdate)

    /**
     * Monitor ephemeral credentials
     *
     * @return the Flow of Nullable EphemeralCredentials
     */
    fun monitorEphemeralCredentials(): Flow<EphemeralCredentials?>

    /**
     * Save ephemeral
     *
     * @param ephemeral
     */
    suspend fun saveEphemeral(ephemeral: EphemeralCredentials)

    /**
     * Clear ephemeral
     *
     */
    suspend fun clearEphemeral()

    /**
     * Broadcast refresh session
     *
     */
    suspend fun broadcastRefreshSession()

    /**
     * Monitor refresh session
     *
     * @return
     */
    fun monitorRefreshSession(): Flow<Unit>

    /**
     * Get user account type
     */
    suspend fun getAccountType(): AccountType

    /**
     * Reconnect and retry all transfers.
     */
    suspend fun reconnect()

    /**
     * Refreshes DNS servers and retries pending connections.
     */
    suspend fun retryPendingConnections()

    /**
     * Broadcasts blocked account.
     *
     * @param type Blocked account type.
     * @param text Message.
     */
    suspend fun broadcastAccountBlocked(type: Long, text: String)

    /**
     * Monitors blocked account.
     *
     * @return Flow of [AccountBlockedDetail]
     */
    fun monitorAccountBlocked(): Flow<AccountBlockedDetail>
}
