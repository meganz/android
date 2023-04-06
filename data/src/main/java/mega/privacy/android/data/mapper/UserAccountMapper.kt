package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId
import javax.inject.Inject

class UserAccountMapper @Inject constructor() {
    operator fun invoke(
        userId: UserId?,
        email: String,
        fullName: String?,
        isBusinessAccount: Boolean,
        isMasterBusinessAccount: Boolean,
        accountTypeIdentifier: AccountType?,
        accountTypeString: String,
        isAchievementsEnabled: Boolean
    ): UserAccount {
        return UserAccount(
            userId = userId,
            email = email,
            fullName = fullName,
            isBusinessAccount = isBusinessAccount,
            isMasterBusinessAccount = isMasterBusinessAccount,
            accountTypeIdentifier = accountTypeIdentifier,
            accountTypeString = accountTypeString,
            isAchievementsEnabled = isAchievementsEnabled
        )
    }
}