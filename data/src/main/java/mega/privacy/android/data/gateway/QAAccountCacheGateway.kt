package mega.privacy.android.data.gateway

import mega.privacy.android.domain.entity.user.UserCredentials

/**
 * Gateway for managing cached account credentials for QA testing
 */
interface QAAccountCacheGateway {
    /**
     * Save current account credentials to cache
     *
     * @param credentials Account credentials to save
     */
    suspend fun saveAccount(credentials: UserCredentials)

    /**
     * Get all cached accounts
     *
     * @return List of cached account credentials
     */
    suspend fun getAllCachedAccounts(): List<UserCredentials>

    /**
     * Remove a cached account by email
     *
     * @param email Email of the account to remove
     */
    suspend fun removeAccount(email: String?)

    /**
     * Clear all cached accounts
     */
    suspend fun clearAllAccounts()

    /**
     * Update last login time for an account
     *
     * @param email Email of the account
     * @param timestamp Last login timestamp in milliseconds
     */
    suspend fun updateLastLoginTime(email: String?, timestamp: Long)

    /**
     * Get last login time for an account
     *
     * @param email Email of the account
     * @return Last login timestamp in milliseconds, or null if not found
     */
    suspend fun getLastLoginTime(email: String?): Long?

    /**
     * Save remark for an account
     *
     * @param email Email of the account
     * @param remark Remark text to save
     */
    suspend fun saveRemark(email: String?, remark: String?)

    /**
     * Get remark for an account
     *
     * @param email Email of the account
     * @return Remark text, or null if not found
     */
    suspend fun getRemark(email: String?): String?
}
