package mega.privacy.android.app.utils.billing

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_III_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_III_YEAR
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_II_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_II_YEAR
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_I_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_I_YEAR
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_LITE_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_LITE_YEAR

object PaymentUtils {
    /**
     * Get renewal type of a certain sku item.
     *
     * @param sku The id of the sku item.
     * @return The renewal type of the sku item, Monthly or Yearly.
     */
    @JvmStatic
    fun getSubscriptionRenewalType(sku: String?, context: Context): String? {
        return when (sku) {
            SKU_PRO_LITE_MONTH, SKU_PRO_I_MONTH, SKU_PRO_II_MONTH, SKU_PRO_III_MONTH ->
                context.getString(R.string.subscription_type_monthly)

            SKU_PRO_LITE_YEAR, SKU_PRO_I_YEAR, SKU_PRO_II_YEAR, SKU_PRO_III_YEAR ->
                context.getString(R.string.subscription_type_yearly)

            else -> ""
        }
    }

    /**
     * Get type name of a certain sku item.
     *
     * @param sku The id of the sku item.
     * @return The type name of the sku.
     */
    @JvmStatic
    fun getSubscriptionType(sku: String?, context: Context): String? {
        return when (sku) {
            SKU_PRO_LITE_MONTH, SKU_PRO_LITE_YEAR -> context.getString(R.string.prolite_account)
            SKU_PRO_I_MONTH, SKU_PRO_I_YEAR -> context.getString(R.string.pro1_account)
            SKU_PRO_II_MONTH, SKU_PRO_II_YEAR -> context.getString(R.string.pro2_account)
            SKU_PRO_III_MONTH, SKU_PRO_III_YEAR -> context.getString(R.string.pro3_account)
            else -> ""
        }
    }
}