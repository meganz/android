package test.mega.privacy.android.app

import mega.privacy.android.app.domain.entity.SupportTicket
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.entity.user.UserId
import mega.privacy.android.app.utils.Constants
import java.io.File

internal val TEST_USER_ACCOUNT = UserAccount(
    userId = UserId(1),
    email = "email@email.com",
    isBusinessAccount = false,
    isMasterBusinessAccount = false,
    accountTypeIdentifier = Constants.FREE,
    accountTypeString = "Free"
)

internal val TEST_SUPPORT_TICKET = SupportTicket(
        androidAppVersion =  "appVersion",
        sdkVersion = "sdkVersion",
        device = "device",
        accountEmail = "accountEmail",
        accountType = "accountTypeString",
        currentLanguage = "languageCode",
        description = "description",
        logFileName = "123-fileName.zip",
)