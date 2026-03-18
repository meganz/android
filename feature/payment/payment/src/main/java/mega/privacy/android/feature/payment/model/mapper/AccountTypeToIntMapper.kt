package mega.privacy.android.feature.payment.model.mapper

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.feature.payment.model.AccountTypeInt

/**
 * Maps [AccountType] to its corresponding integer representation used by [AccountTypeInt].
 */
fun AccountType.toAccountTypeInt(): Int {
    return when (this) {
        AccountType.PRO_LITE -> AccountTypeInt.PRO_LITE
        AccountType.PRO_I -> AccountTypeInt.PRO_I
        AccountType.PRO_II -> AccountTypeInt.PRO_II
        AccountType.PRO_III -> AccountTypeInt.PRO_III
        AccountType.STARTER -> AccountTypeInt.STARTER
        AccountType.BASIC -> AccountTypeInt.BASIC
        AccountType.ESSENTIAL -> AccountTypeInt.ESSENTIAL
        else -> AccountTypeInt.FREE
    }
}
