package mega.privacy.android.data.mapper.account

import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.account.AccountBlockedType
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
        200L -> AccountBlockedType.TOS_COPYRIGHT
        300L -> AccountBlockedType.TOS_NON_COPYRIGHT
        400L -> AccountBlockedType.SUBUSER_DISABLED
        401L -> AccountBlockedType.SUBUSER_REMOVED
        500L -> AccountBlockedType.VERIFICATION_SMS
        700L -> AccountBlockedType.VERIFICATION_EMAIL
        else -> AccountBlockedType.NOT_BLOCKED
    }
}