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
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.Skus

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

    /**
     * Get product id for payment
     *
     * @param isMonthly true if monthly subscription, false if yearly
     * @param upgradeType the account type to upgrade to
     * @return the product id for billing
     */
    @JvmStatic
    fun getProductId(isMonthly: Boolean, upgradeType: AccountType): String {
        val skus = getSkus(convertAccountTypeToInt(upgradeType))
        return if (isMonthly) skus.first else skus.second
    }

    private fun convertAccountTypeToInt(accountType: AccountType): Int {
        return when (accountType) {
            AccountType.PRO_LITE -> Constants.PRO_LITE
            AccountType.PRO_I -> Constants.PRO_I
            AccountType.PRO_II -> Constants.PRO_II
            AccountType.PRO_III -> Constants.PRO_III
            else -> Constants.INVALID_VALUE
        }
    }

    private fun getSkus(upgradeType: Int) = when (upgradeType) {
        Constants.PRO_I -> Skus.SKU_PRO_I_MONTH to Skus.SKU_PRO_I_YEAR
        Constants.PRO_II -> Skus.SKU_PRO_II_MONTH to Skus.SKU_PRO_II_YEAR
        Constants.PRO_III -> Skus.SKU_PRO_III_MONTH to Skus.SKU_PRO_III_YEAR
        Constants.PRO_LITE -> Skus.SKU_PRO_LITE_MONTH to Skus.SKU_PRO_LITE_YEAR
        else -> "" to ""
    }
}