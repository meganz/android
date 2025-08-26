package mega.privacy.android.feature.payment.util

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.Skus

object PaymentUtils {
    /**
     * Get product id for payment
     *
     * @param isMonthly true if monthly subscription, false if yearly
     * @param upgradeType the account type to upgrade to
     * @return the product id for billing
     */
    fun getProductId(isMonthly: Boolean, upgradeType: AccountType): String {
        val skus = getSkus(upgradeType)
        return if (isMonthly) skus.first else skus.second
    }

    private fun getSkus(accountType: AccountType) = when (accountType) {
        AccountType.PRO_I -> Skus.SKU_PRO_I_MONTH to Skus.SKU_PRO_I_YEAR
        AccountType.PRO_II -> Skus.SKU_PRO_II_MONTH to Skus.SKU_PRO_II_YEAR
        AccountType.PRO_III -> Skus.SKU_PRO_III_MONTH to Skus.SKU_PRO_III_YEAR
        AccountType.PRO_LITE -> Skus.SKU_PRO_LITE_MONTH to Skus.SKU_PRO_LITE_YEAR
        else -> "" to ""
    }
}