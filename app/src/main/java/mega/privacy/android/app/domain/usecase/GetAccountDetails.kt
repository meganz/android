package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.UserAccount

/**
 * Get account details
 *
 */
interface GetAccountDetails {
    /**
     * Invoke
     *
     * @param forceRefresh
     * @return
     */
    operator fun invoke(forceRefresh: Boolean): UserAccount
}