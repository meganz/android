package mega.privacy.android.app.presentation.myaccount.model.mapper

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.myaccount.model.AccountTypeAttributes
import mega.privacy.android.core.ui.theme.extensions.blue_400_blue_300
import mega.privacy.android.core.ui.theme.extensions.green_400_green_300
import mega.privacy.android.core.ui.theme.extensions.orange_600_orange_300
import mega.privacy.android.core.ui.theme.extensions.red_300_red_200
import mega.privacy.android.core.ui.theme.extensions.white_black
import mega.privacy.android.domain.entity.AccountType

/**
 * Convert [AccountType] into [AccountTypeAttributes]
 */
@Composable
fun AccountType?.toAccountAttributes(): AccountTypeAttributes {
    return when (this) {
        AccountType.FREE -> AccountTypeAttributes(
            background = MaterialTheme.colors.green_400_green_300,
            icon = R.drawable.ic_free_account,
            description = R.string.free_account
        )

        AccountType.PRO_LITE -> AccountTypeAttributes(
            background = MaterialTheme.colors.orange_600_orange_300,
            icon = R.drawable.ic_lite_account,
            description = R.string.prolite_account
        )

        AccountType.PRO_I -> AccountTypeAttributes(
            background = MaterialTheme.colors.red_300_red_200,
            icon = R.drawable.ic_pro_i_account,
            description = R.string.pro1_account
        )

        AccountType.PRO_II -> AccountTypeAttributes(
            background = MaterialTheme.colors.red_300_red_200,
            icon = R.drawable.ic_pro_ii_account,
            description = R.string.pro2_account
        )

        AccountType.PRO_III -> AccountTypeAttributes(
            background = MaterialTheme.colors.red_300_red_200,
            icon = R.drawable.ic_pro_iii_account,
            description = R.string.pro3_account
        )

        AccountType.PRO_FLEXI -> AccountTypeAttributes(
            background = MaterialTheme.colors.red_300_red_200,
            icon = R.drawable.ic_pro_flexi_account,
            description = R.string.pro_flexi_account
        )

        AccountType.BUSINESS -> AccountTypeAttributes(
            background = MaterialTheme.colors.blue_400_blue_300,
            icon = R.drawable.ic_business_account,
            description = R.string.business_label
        )

        else -> AccountTypeAttributes(
            background = MaterialTheme.colors.white_black,
            icon = R.drawable.ic_business_account,
            description = R.string.recovering_info
        )
    }
}