package mega.privacy.android.feature.payment.model.mapper

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.Skus
import javax.inject.Inject

/**
 * Mapper to convert AccountType and subscription period to product ID for billing
 */
class AccountTypeToProductIdMapper @Inject constructor() {

    /**
     * Invoke
     * Get product id for payment based on account type and subscription period
     *
     * @param accountType the account type to upgrade to
     * @param isMonthly true if monthly subscription, false if yearly
     * @return the product id for billing, or empty string if account type is not supported
     */
    operator fun invoke(
        accountType: AccountType,
        isMonthly: Boolean,
    ): String = when (accountType) {
        AccountType.PRO_I -> if (isMonthly) Skus.SKU_PRO_I_MONTH else Skus.SKU_PRO_I_YEAR
        AccountType.PRO_II -> if (isMonthly) Skus.SKU_PRO_II_MONTH else Skus.SKU_PRO_II_YEAR
        AccountType.PRO_III -> if (isMonthly) Skus.SKU_PRO_III_MONTH else Skus.SKU_PRO_III_YEAR
        AccountType.PRO_LITE -> if (isMonthly) Skus.SKU_PRO_LITE_MONTH else Skus.SKU_PRO_LITE_YEAR
        else -> ""
    }
}

