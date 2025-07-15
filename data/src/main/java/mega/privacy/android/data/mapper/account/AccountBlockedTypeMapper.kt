package mega.privacy.android.data.mapper.account

import mega.privacy.android.domain.entity.account.AccountBlockedType
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

internal class AccountBlockedTypeMapper @Inject constructor() {
    operator fun invoke(sdkType: Long) = when (sdkType.toInt()) {
        MegaApiJava.ACCOUNT_BLOCKED_TOS_COPYRIGHT -> AccountBlockedType.TOS_COPYRIGHT
        MegaApiJava.ACCOUNT_BLOCKED_TOS_NON_COPYRIGHT -> AccountBlockedType.TOS_NON_COPYRIGHT
        MegaApiJava.ACCOUNT_BLOCKED_SUBUSER_DISABLED -> AccountBlockedType.SUBUSER_DISABLED
        MegaApiJava.ACCOUNT_BLOCKED_SUBUSER_REMOVED -> AccountBlockedType.SUBUSER_REMOVED
        MegaApiJava.ACCOUNT_BLOCKED_VERIFICATION_SMS -> AccountBlockedType.VERIFICATION_SMS
        MegaApiJava.ACCOUNT_BLOCKED_VERIFICATION_EMAIL -> AccountBlockedType.VERIFICATION_EMAIL
        else -> AccountBlockedType.NOT_BLOCKED
    }
}