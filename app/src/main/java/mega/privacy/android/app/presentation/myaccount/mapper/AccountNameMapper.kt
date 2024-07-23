package mega.privacy.android.app.presentation.myaccount.mapper

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.AccountType
import javax.inject.Inject

class AccountNameMapper @Inject constructor() {

    /**
     * Get the account description based on the account type
     * @param accountType Account type
     */
    operator fun invoke(accountType: AccountType?): Int =
        when (accountType) {
            AccountType.FREE -> R.string.free_account
            AccountType.PRO_LITE -> R.string.prolite_account
            AccountType.PRO_I -> R.string.pro1_account
            AccountType.PRO_II -> R.string.pro2_account
            AccountType.PRO_III -> R.string.pro3_account
            AccountType.PRO_FLEXI -> R.string.pro_flexi_account
            AccountType.BUSINESS -> R.string.business_label
            AccountType.STARTER -> mega.privacy.android.shared.resources.R.string.general_low_tier_plan_starter_label
            AccountType.BASIC -> mega.privacy.android.shared.resources.R.string.general_low_tier_plan_basic_label
            AccountType.ESSENTIAL -> mega.privacy.android.shared.resources.R.string.general_low_tier_plan_essential_label
            else -> R.string.recovering_info
        }

}