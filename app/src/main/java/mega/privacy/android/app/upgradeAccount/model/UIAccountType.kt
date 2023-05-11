package mega.privacy.android.app.upgradeAccount.model

import androidx.compose.ui.graphics.Color
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.theme.green_200
import mega.privacy.android.core.ui.theme.green_500
import mega.privacy.android.core.ui.theme.orange_300
import mega.privacy.android.core.ui.theme.orange_500
import mega.privacy.android.core.ui.theme.red_300
import mega.privacy.android.core.ui.theme.red_600

/**
 *  UI enum class for Account Type
 *
 *  @param iconValue                  Int     icon Int
 *  @param textValue                  Int     string Int
 *  @param colorValue                 Color   color value for Light Theme
 *  @param colorValueDark             Color   color value for Dark Theme
 *  @param textBuyAccountTypeValue    String   String for Buy button
 */
enum class UIAccountType(
    val iconValue: Int,
    val textValue: Int,
    val colorValue: Color,
    val colorValueDark: Color,
    val textBuyAccountTypeValue: String,
) {
    /**
     * FREE
     */
    FREE(
        R.drawable.ic_free_crest,
        R.string.free_account,
        green_500,
        green_200,
        "",
    ),

    /**
     * PRO_LITE
     */
    PRO_LITE(
        R.drawable.ic_lite_crest,
        R.string.prolite_account,
        orange_500,
        orange_300,
        "Buy Pro Lite",
    ),

    /**
     * PRO_I
     */
    PRO_I(
        R.drawable.ic_pro_1_crest,
        R.string.pro1_account,
        red_600,
        red_300,
        "Buy Pro I",
    ),

    /**
     * PRO_II
     */
    PRO_II(
        R.drawable.ic_pro_2_crest,
        R.string.pro2_account,
        red_600,
        red_300,
        "Buy Pro II",
    ),

    /**
     * PRO_III
     */
    PRO_III(
        R.drawable.ic_pro_3_crest,
        R.string.pro3_account,
        red_600,
        red_300,
        "Buy Pro III",
    );

    companion object {
        /**
         * The default selected account type
         */
        val DEFAULT = FREE
    }
}