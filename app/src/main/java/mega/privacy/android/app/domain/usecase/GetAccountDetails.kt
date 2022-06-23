package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.UserAccount

/**
 * Get account details
 *
 */
fun interface GetAccountDetails {
    /**
     * Invoke
     *
     * @param forceRefresh
     * @return
     */
    suspend operator fun invoke(forceRefresh: Boolean): UserAccount
}