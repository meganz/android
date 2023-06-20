package mega.privacy.android.data.mapper.account

import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.account.AccountBlockedType
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Mapper for converting data into [AccountBlockedDetail].
 */
class AccountBlockedDetailMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param type Blocked account type.
     * @param text Message.
     */
    operator fun invoke(type: Long, text: String) = AccountBlockedDetail(
        type = type.toAccountBlockedType(),
        text = text
    )

    /**
     * Pending to create SDK bindings and replace long values with SDK enum.
     */
    private fun Long.toAccountBlockedType() = when (this) {
        MegaApiJava.ACCOUNT_BLOCKED_TOS_COPYRIGHT.toLong() -> AccountBlockedType.TOS_COPYRIGHT
        MegaApiJava.ACCOUNT_BLOCKED_TOS_NON_COPYRIGHT.toLong() -> AccountBlockedType.TOS_NON_COPYRIGHT
        MegaApiJava.ACCOUNT_BLOCKED_SUBUSER_DISABLED.toLong() -> AccountBlockedType.SUBUSER_DISABLED
        MegaApiJava.ACCOUNT_BLOCKED_SUBUSER_REMOVED.toLong() -> AccountBlockedType.SUBUSER_REMOVED
        MegaApiJava.ACCOUNT_BLOCKED_VERIFICATION_SMS.toLong() -> AccountBlockedType.VERIFICATION_SMS
        MegaApiJava.ACCOUNT_BLOCKED_VERIFICATION_EMAIL.toLong() -> AccountBlockedType.VERIFICATION_EMAIL
        else -> AccountBlockedType.NOT_BLOCKED
    }
}