package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.MegaAchievement
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
     * Refreshes DNS servers and retries pending connections.
     *
     * @param disconnect True if should disconnect megaChatApi, false otherwise.
     */
    suspend fun retryPendingConnections(disconnect: Boolean)

    /**
     * Checks whether the user's Business Account is currently active or not
     *
     * @return True if the user's Business Account is currently active, or
     * false if inactive or if the user is not under a Business Account
     */
    suspend fun isBusinessAccountActive(): Boolean

    /**
     * Get the List of SubscriptionOptions
     *
     * @return List of SubscriptionOptions
     */
    suspend fun getSubscriptionOptions(): List<SubscriptionOption>

    /**
     * Returns if accounts achievements enabled
     */
    suspend fun isAccountAchievementsEnabled(): Boolean

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
    suspend fun getMyCredentials(): String?

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
     * Check Achievements are enabled or not
     */
    suspend fun areAchievementsEnabled(): Boolean

}
