package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.UserAccount
import javax.inject.Inject

/**
 * Default can delete account
 *
 */
class DefaultCanDeleteAccount @Inject constructor(): CanDeleteAccount {
    override fun invoke(account: UserAccount): Boolean {
        return !account.isBusinessAccount || account.isMasterBusinessAccount
    }
}