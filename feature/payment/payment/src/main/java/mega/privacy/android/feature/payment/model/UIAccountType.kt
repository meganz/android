package mega.privacy.android.feature.payment.model

import mega.privacy.android.shared.resources.R as sharedR

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
        sharedR.string.general_free_plan_name,
        0,
    ),

    /**
     * PRO_LITE
     */
    PRO_LITE(
        sharedR.string.prolite_account,
        sharedR.string.account_upgrade_account_buy_button_title_pro_lite,
    ),

    /**
     * PRO_I
     */
    PRO_I(
        sharedR.string.pro1_account,
        sharedR.string.account_upgrade_account_buy_button_title_pro_i,
    ),

    /**
     * PRO_II
     */
    PRO_II(
        sharedR.string.pro2_account,
        sharedR.string.account_upgrade_account_buy_button_title_pro_ii,
    ),

    /**
     * PRO_III
     */
    PRO_III(
        sharedR.string.pro3_account,
        sharedR.string.account_upgrade_account_buy_button_title_pro_iii,
    ),

    /**
     * ESSENTIAL
     */
    ESSENTIAL(
        sharedR.string.essential_account,
        sharedR.string.account_upgrade_account_buy_button_title_essential,
    ),

    /**
     * STARTER
     */
    STARTER(
        sharedR.string.starter_account,
        sharedR.string.account_upgrade_account_buy_button_title_starter,
    ),

    /**
     * BASIC
     */
    BASIC(
        sharedR.string.basic_account,
        sharedR.string.account_upgrade_account_buy_button_title_basic,
    );

    companion object {
        /**
         * The default selected account type
         */
        val DEFAULT = FREE
    }
}