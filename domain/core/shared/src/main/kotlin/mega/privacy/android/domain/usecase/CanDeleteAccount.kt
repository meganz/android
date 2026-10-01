package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.UserAccount

/**
 * Can delete account
 *
 */
interface CanDeleteAccount {
    /**
     * Invoke
     *
     * @param account
     * @return
     */
    operator fun invoke(account: UserAccount): Boolean
}