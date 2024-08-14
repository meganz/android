package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountType
import nz.mega.sdk.MegaAccountDetails
import javax.inject.Inject

/**
 * Account Type Mapper
 */
internal class AccountTypeMapper @Inject constructor() {
    /**
     * Invoke
     * @param type [Int]
     */
    operator fun invoke(type: Int) = when (type) {
        MegaAccountDetails.ACCOUNT_TYPE_FREE -> AccountType.FREE
        MegaAccountDetails.ACCOUNT_TYPE_PROI -> AccountType.PRO_I
        MegaAccountDetails.ACCOUNT_TYPE_PROII -> AccountType.PRO_II
        MegaAccountDetails.ACCOUNT_TYPE_PROIII -> AccountType.PRO_III
        MegaAccountDetails.ACCOUNT_TYPE_LITE -> AccountType.PRO_LITE
        MegaAccountDetails.ACCOUNT_TYPE_PRO_FLEXI -> AccountType.PRO_FLEXI
        MegaAccountDetails.ACCOUNT_TYPE_BUSINESS -> AccountType.BUSINESS
        MegaAccountDetails.ACCOUNT_TYPE_STARTER -> AccountType.STARTER
        MegaAccountDetails.ACCOUNT_TYPE_BASIC -> AccountType.BASIC
        MegaAccountDetails.ACCOUNT_TYPE_ESSENTIAL -> AccountType.ESSENTIAL
        MegaAccountDetails.ACCOUNT_TYPE_FEATURE -> AccountType.FREE
        else -> AccountType.UNKNOWN
    }
}