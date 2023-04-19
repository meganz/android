package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId
import javax.inject.Inject

/**
 * User Account Info Mapper
 */
class UserAccountMapper @Inject constructor() {
    /**
     * Invoke
     * @return [UserAccount]
     * @param userId
     * @param email
     * @param fullName
     * @param isBusinessAccount
     * @param isMasterBusinessAccount
     * @param accountTypeIdentifier
     * @param accountTypeString
     */
    operator fun invoke(
        userId: UserId?,
        email: String,
        fullName: String?,
        isBusinessAccount: Boolean,
        isMasterBusinessAccount: Boolean,
        accountTypeIdentifier: AccountType?,
        accountTypeString: String,
    ): UserAccount {
        return UserAccount(
            userId = userId,
            email = email,
            fullName = fullName,
            isBusinessAccount = isBusinessAccount,
            isMasterBusinessAccount = isMasterBusinessAccount,
            accountTypeIdentifier = accountTypeIdentifier,
            accountTypeString = accountTypeString,
        )
    }
}