package mega.privacy.android.app.data.mapper

import mega.privacy.android.app.service.iab.BillingManagerImpl
import mega.privacy.android.domain.entity.AccountType

/**
 * Map [AccountType], [Int] to [String]. Return value is String.
 */
internal fun toSkuMapper(
    subscriptionPlanLevel: AccountType?,
    subscriptionPlanMonths: Int,
): String? =
    when (subscriptionPlanLevel) {
        AccountType.PRO_LITE -> if (subscriptionPlanMonths == 1) BillingManagerImpl.SKU_PRO_LITE_MONTH else BillingManagerImpl.SKU_PRO_LITE_YEAR
        AccountType.PRO_I -> if (subscriptionPlanMonths == 1) BillingManagerImpl.SKU_PRO_I_MONTH else BillingManagerImpl.SKU_PRO_I_YEAR
        AccountType.PRO_II -> if (subscriptionPlanMonths == 1) BillingManagerImpl.SKU_PRO_II_MONTH else BillingManagerImpl.SKU_PRO_II_YEAR
        AccountType.PRO_III -> if (subscriptionPlanMonths == 1) BillingManagerImpl.SKU_PRO_III_MONTH else BillingManagerImpl.SKU_PRO_III_YEAR
        else -> null
    }