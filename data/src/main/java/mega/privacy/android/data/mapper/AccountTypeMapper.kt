package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountType
import nz.mega.sdk.MegaAccountDetails

/**
 * Map [Int] to [AccountType]
 */
typealias AccountTypeMapper = (@JvmSuppressWildcards Int) -> @JvmSuppressWildcards AccountType?

/**
 * Map [Int] to [AccountType]. Return value can be subclass of [AccountType]
 */
internal fun toAccountType(type: Int): AccountType = when (type) {
    MegaAccountDetails.ACCOUNT_TYPE_FREE -> AccountType.FREE
    MegaAccountDetails.ACCOUNT_TYPE_PROI -> AccountType.PRO_I
    MegaAccountDetails.ACCOUNT_TYPE_PROII -> AccountType.PRO_II
    MegaAccountDetails.ACCOUNT_TYPE_PROIII -> AccountType.PRO_III
    MegaAccountDetails.ACCOUNT_TYPE_LITE -> AccountType.PRO_LITE
    MegaAccountDetails.ACCOUNT_TYPE_PRO_FLEXI -> AccountType.PRO_FLEXI
    MegaAccountDetails.ACCOUNT_TYPE_BUSINESS -> AccountType.BUSINESS
    else -> AccountType.UNKNOWN
}