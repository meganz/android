package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.UserAccount

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