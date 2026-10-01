package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.UserAccount
import javax.inject.Inject

/**
 * Default can delete account
 *
 */
class DefaultCanDeleteAccount @Inject constructor() : CanDeleteAccount {
    override fun invoke(account: UserAccount) =
        !account.isBusinessAccount || account.isMasterBusinessAccount
}