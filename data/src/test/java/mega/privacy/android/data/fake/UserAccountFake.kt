package mega.privacy.android.data.fake

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId

/**
 * Fake data class of [UserAccount] for testing purposes
 * @see UserAccount
 */
fun userAccount(
    userId: UserId? = null,
    email: String = "",
    fullName: String? = null,
    isBusinessAccount: Boolean = false,
    isMasterBusinessAccount: Boolean = false,
    accountTypeIdentifier: AccountType? = null,
    accountTypeString: String = "",
    isAchievementsEnabled: Boolean = false,
): UserAccount = UserAccount(
    userId,
    email,
    fullName,
    isBusinessAccount,
    isMasterBusinessAccount,
    accountTypeIdentifier,
    accountTypeString,
    isAchievementsEnabled
)