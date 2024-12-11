package mega.privacy.android.app.presentation.account.model

import mega.privacy.android.shared.resources.R as sharedR
import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * Enum class that denotes different statuses when the user's account is deactivated
 *
 * @property title The title of the dialog
 * @property body The body of the dialog
 */
enum class AccountDeactivatedStatus(
    @StringRes val title: Int,
    @StringRes val body: Int,
) {

    /**
     * Enum entry that indicates the user's account is deactivated master business account
     */
    MASTER_BUSINESS_ACCOUNT_DEACTIVATED(
        title = R.string.account_business_account_deactivated_dialog_title,
        body = R.string.account_business_account_deactivated_dialog_admin_body
    ),

    /**
     * Enum entry that indicates the user's account is deactivated business account
     */
    BUSINESS_ACCOUNT_DEACTIVATED(
        title = R.string.account_business_account_deactivated_dialog_title,
        body = R.string.account_business_account_deactivated_dialog_sub_user_body
    ),

    /**
     * Enum entry that indicates the user's account is deactivated pro flexi account
     */
    PRO_FLEXI_ACCOUNT_DEACTIVATED(
        title = sharedR.string.account_pro_flexi_account_deactivated_dialog_title,
        body = sharedR.string.account_pro_flexi_account_deactivated_dialog_body
    )
}