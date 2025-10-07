package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.resetpassword.ResetPasswordLinkInfo
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.exception.MegaException
import java.io.File

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
    suspend fun getSpecificAccountDetail(
        storage: Boolean,
        transfer: Boolean,
        pro: Boolean,
    ): AccountDetail

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
     * Check if the MegaApi object is logged in
     *
     * NOTE: Executing a fastLogin while fetching the isLoggedIn can return 0.
     *
     * @return 0 if not logged in, Otherwise a number > 0
     */
    suspend fun isMegaApiLoggedIn(): Boolean

    /**
     * Check if is ephemeral plus plus account
     */
    suspend fun isEphemeralPlusPlus(): Boolean

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
     * Queries a reset password link.
     *
     * @param link Reset password link
     * @return Email associated with the link
     */
    suspend fun queryResetPasswordLink(link: String): ResetPasswordLinkInfo

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
    fun getAccountType(): AccountType

    /**
     * Refreshes DNS servers and retries pending connections.
     */
    suspend fun retryPendingConnections()

    /**
     * Get logged in user id
     *
     * @return a user handle Long value or null
     */
    suspend fun getLoggedInUserId(): UserId?

    /**
     * Get the alias of the given user if any
     *
     * @param handle User identifier.
     * @return User alias.
     */
    suspend fun getUserAlias(handle: Long): String?

    /**
     * Returns the known alias given to the user
     *
     * Returns NULL if data is not cached yet or it's not possible to get
     *
     * You take the ownership of returned value
     *
     * @param userHandle Handle of the user whose alias is requested.
     * @return The alias from user
     */
    suspend fun getUserAliasFromCache(userHandle: Long): String?

    /**
     * Checks whether MEGA Achievements are enabled for the open account
     * @return True if enabled, false otherwise.
     */
    suspend fun isAchievementsEnabled(): Boolean

    /**
     * Rename Recovery Key file
     *
     * @param relativePath    Relative path of the file
     * @param newName         New name for the file
     * @return                True if success or false otherwise
     */
    suspend fun renameRecoveryKeyFile(relativePath: String, newName: String): Boolean

    /**
     * Get recovery key file
     */
    suspend fun getRecoveryKeyFile(): File?

    /**
     * Get a boolean value that represent whether the user account is new or not
     *
     * @return if the account is new or not
     */
    suspend fun isAccountNew(): Boolean

    /**
     * Get a boolean value that represent if the cookie banner is enabled or not
     *
     * @return if the cookie banner is enabled or not
     */
    suspend fun isCookieBannerEnabled(): Boolean

    /**
     * Fetch miscellaneous flags when not logged in
     * The associated request type with this request is MegaRequest::TYPE_GET_MISC_FLAGS.
     * */
    suspend fun getMiscFlags()

    /**
     * Get cookie settings
     *
     * @return Set of CookieType
     */
    suspend fun getCookieSettings(): Set<CookieType>

    /**
     * Set cookie settings
     *
     * @param enabledCookieSettings Set of CookieType
     */
    suspend fun setCookieSettings(enabledCookieSettings: Set<CookieType>)

    /**
     * Monitor cookie settings saved
     *
     * @return Flow of Set of CookieType
     */
    fun monitorCookieSettingsSaved(): Flow<Set<CookieType>>

    /**
     * Broadcast cookie settings
     *
     * @param enabledCookieSettings Set of CookieType
     */
    suspend fun broadcastCookieSettings(enabledCookieSettings: Set<CookieType>)

    /**
     * Should show copyright
     */
    suspend fun shouldShowCopyright(): Boolean

    /**
     * Kills all other active Sessions except the current Session
     */
    suspend fun killOtherSessions()

    /**
     * Confirms an Account cancellation
     *
     * @param cancellationLink The Account cancellation link
     * @param accountPassword The password of the Account to be cancelled
     */
    suspend fun confirmCancelAccount(cancellationLink: String, accountPassword: String)

    /**
     * Confirms the User's change of Email
     *
     * @param changeEmailLink The Change Email link
     * @param accountPassword The password of the Account whose email to be changed
     *
     * @return The new Email Address
     */
    suspend fun confirmChangeEmail(changeEmailLink: String, accountPassword: String): String

    /**
     * Get the user's data
     */
    suspend fun getUserData()

    /**
     * Broadcasts update user's data.
     */
    suspend fun broadcastUpdateUserData()

    /**
     * Monitors update user's data broadcast.
     */
    fun monitorUpdateUserData(): Flow<Unit>

    /**
     * Get last registered email
     * @return [Boolean]
     */
    suspend fun getLastRegisteredEmail(): String?

    /**
     * Save last registered email
     * @param email [String]
     */
    suspend fun saveLastRegisteredEmail(email: String)

    /**
     * Clear last registered email
     */
    suspend fun clearLastRegisteredEmail()

    /**
     * Retrieves information on the Account Cancellation Link
     *
     * @param accountCancellationLink The Account Cancellation Link to be queried
     * @return the Account Cancellation Link if there are no issues found during the querying process
     */
    suspend fun queryCancelLink(accountCancellationLink: String): String

    /**
     * Retrieves information on the Change Email Link
     *
     * @param changeEmailLink The Change Email Link to be queried
     * @return The Change Email Link if there are no issues found during the querying process
     */
    suspend fun queryChangeEmailLink(changeEmailLink: String): String

    /**
     * Cancel a registration process
     *
     * @return The corresponding email
     */
    suspend fun cancelCreateAccount(): String

    /**
     * Get storage size occupied by the user in bytes
     */
    suspend fun getUsedStorage(): Long

    /**
     * Total storage size in bytes
     *
     * @return
     */
    suspend fun getMaxStorage(): Long

    /**
     * Monitor credentials
     *
     * @return Flow of UserCredentials
     */
    fun monitorCredentials(): Flow<UserCredentials?>

    /**
     * Set credentials
     *
     * @param credentials
     */
    suspend fun setCredentials(credentials: UserCredentials)

    /**
     * Clear credentials
     *
     */
    suspend fun clearCredentials()

    /**
     * Get the [User] of the currently open account.
     *
     * @return [User] The current user.
     */
    suspend fun getCurrentUser(): User?

    /**
     * Get the account's storage state
     *
     * @return [Int] The storage state
     */
    suspend fun getStorageState(): StorageState

    /**
     * Set timestamp when user closed Almost Full Storage Quota warning banner
     *
     * @param timestamp
     */
    suspend fun setAlmostFullStorageBannerClosingTimestamp(timestamp: Long)

    /**
     * Get the timestamp, when the almost full storage quota banner was closed
     *
     * @return Flow of timestamp
     */
    fun monitorAlmostFullStorageBannerClosingTimestamp(): Flow<Long?>

    /**
     * Create new user account
     *
     * @param email User's email to register
     * @param password Account password
     * @param firstName User's first name
     * @param lastName User's last name
     *
     * @return EphemeralCredentials if successful
     */
    suspend fun createAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): EphemeralCredentials

    /**
     * Get Invalid Handle
     */
    fun getInvalidHandle(): Long

    /**
     * Get Invalid Affiliate Type
     */
    fun getInvalidAffiliateType(): Int


    /**
     * Monitor misc loaded
     */
    fun monitorMiscLoaded(): Flow<Boolean>

    /**
     * Broadcast misc loaded
     */
    suspend fun broadcastMiscLoaded()

    /**
     * Broadcast misc un loaded
     */
    suspend fun broadcastMiscUnLoaded()

    /**
     *  Resend the verification email for Weak Account Protection
     */
    suspend fun resendVerificationEmail()

    /**
     * Resume create account
     */
    suspend fun resumeCreateAccount(session: String)

    /**
     * Check if the recovery key is valid
     *
     * @param link The recovery key link
     * @param recoveryKey The recovery key
     */
    suspend fun checkRecoveryKey(link: String, recoveryKey: String)

    /**
     * Set the state indicating if the user session has been logged out from another location.
     */
    suspend fun setLoggedOutFromAnotherLocation(isLoggedOut: Boolean)

    /**
     * Monitor the state indicating if the user session has been logged out from another location.
     *
     * @return Flow of Boolean
     */
    fun monitorLoggedOutFromAnotherLocation(): Flow<Boolean>

    /**
     * Set the state indicating if the current account is an unverified business account.
     */
    suspend fun setIsUnverifiedBusinessAccount(isUnverified: Boolean)

    /**
     * Monitor the state indicating if the current account is an unverified business account.
     *
     * @return Flow of Boolean
     */
    fun monitorIsUnverifiedBusinessAccount(): Flow<Boolean>
}