package test.mega.privacy.android.app

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId

internal val TEST_USER_ACCOUNT = UserAccount(
    userId = UserId(1),
    email = "email@email.com",
    fullName = "name",
    isBusinessAccount = false,
    isMasterBusinessAccount = false,
    accountTypeIdentifier = AccountType.FREE,
    accountTypeString = "Free"
)

