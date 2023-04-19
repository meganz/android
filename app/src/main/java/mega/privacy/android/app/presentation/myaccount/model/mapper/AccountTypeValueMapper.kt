package mega.privacy.android.app.myAccount

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.myaccount.model.AccountTypeAttributes
import mega.privacy.android.domain.entity.AccountType

/**
 * Convert [AccountType] into [AccountTypeAttributes]
 */
fun AccountType?.toAccountAttributes(): AccountTypeAttributes {
    return when (this) {
        AccountType.FREE -> AccountTypeAttributes(
            background = R.color.green_400_green_300,
            icon = R.drawable.ic_free_account,
            description = R.string.free_account
        )

        AccountType.PRO_LITE -> AccountTypeAttributes(
            background = R.color.orange_600_orange_300,
            icon = R.drawable.ic_lite_account,
            description = R.string.prolite_account
        )

        AccountType.PRO_I -> AccountTypeAttributes(
            background = R.color.red_300_red_200,
            icon = R.drawable.ic_pro_i_account,
            description = R.string.pro1_account
        )

        AccountType.PRO_II -> AccountTypeAttributes(
            background = R.color.red_300_red_200,
            icon = R.drawable.ic_pro_ii_account,
            description = R.string.pro2_account
        )

        AccountType.PRO_III -> AccountTypeAttributes(
            background = R.color.red_300_red_200,
            icon = R.drawable.ic_pro_iii_account,
            description = R.string.pro3_account
        )

        AccountType.PRO_FLEXI -> AccountTypeAttributes(
            background = R.color.red_300_red_200,
            icon = R.drawable.ic_pro_flexi_account,
            description = R.string.pro_flexi_account
        )

        AccountType.BUSINESS -> AccountTypeAttributes(
            background = R.color.blue_400_blue_300,
            icon = R.drawable.ic_business_account,
            description = R.string.business_label
        )

        else -> AccountTypeAttributes(
            background = R.color.white_black,
            icon = R.drawable.ic_business_account,
            description = R.string.recovering_info
        )
    }
}