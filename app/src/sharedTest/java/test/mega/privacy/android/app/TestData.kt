package test.mega.privacy.android.app

import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.SupportTicket
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId

internal val TEST_USER_ACCOUNT = UserAccount(
    userId = UserId(1),
    email = "email@email.com",
    isBusinessAccount = false,
    isMasterBusinessAccount = false,
    accountTypeIdentifier = Constants.FREE,
    accountTypeString = "Free"
)

