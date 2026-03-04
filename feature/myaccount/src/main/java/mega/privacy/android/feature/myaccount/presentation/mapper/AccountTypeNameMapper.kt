package mega.privacy.android.feature.myaccount.presentation.mapper

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.shared.resources.R
import mega.privacy.android.shared.resources.SharedStringResourceProvider
import javax.inject.Inject

/**
 * Mapper to get account type name resource ID.
 * Implements [SharedStringResourceProvider] for use by other modules (e.g. payment).
 */
class AccountTypeNameMapper @Inject constructor() : SharedStringResourceProvider<AccountType?> {

    /**
     * Get the account description based on the account type
     * @param input Account type
     * @return string resource ID for the account type name
     */
    @StringRes
    override operator fun invoke(input: AccountType?): Int =
        when (input) {
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
