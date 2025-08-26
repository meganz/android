package mega.privacy.android.feature.payment.model

import mega.privacy.android.feature.payment.R

/**
 *  UI enum class for Account Type
 *
 *  @param textValue                  Int     string Int
 *  @param textBuyButtonValue         Int     string Int for Buy button
 */
enum class UIAccountType(
    val textValue: Int,
    val textBuyButtonValue: Int,
) {
    /**
     * FREE
     */
    FREE(
        R.string.general_free_plan_name,
        0,
    ),

    /**
     * PRO_LITE
     */
    PRO_LITE(
        R.string.prolite_account,
        R.string.account_upgrade_account_buy_button_title_pro_lite,
    ),

    /**
     * PRO_I
     */
    PRO_I(
        R.string.pro1_account,
        R.string.account_upgrade_account_buy_button_title_pro_i,
    ),

    /**
     * PRO_II
     */
    PRO_II(
        R.string.pro2_account,
        R.string.account_upgrade_account_buy_button_title_pro_ii,
    ),

    /**
     * PRO_III
     */
    PRO_III(
        R.string.pro3_account,
        R.string.account_upgrade_account_buy_button_title_pro_iii,
    );

    companion object {
        /**
         * The default selected account type
         */
        val DEFAULT = FREE
    }
}