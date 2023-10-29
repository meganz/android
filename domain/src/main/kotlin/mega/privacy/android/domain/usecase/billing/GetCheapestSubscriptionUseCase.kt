package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.Skus
import javax.inject.Inject

/**
 * Get cheapest Subscription available for user (e.g. Pro Lite or Pro I)
 *
 * @property calculateCurrencyAmountUseCase     [CalculateCurrencyAmountUseCase]
 * @property getLocalPricingUseCase             [GetLocalPricingUseCase]
 * @property getAppSubscriptionOptionsUseCase   [GetAppSubscriptionOptionsUseCase]
 */

class GetCheapestSubscriptionUseCase @Inject constructor(
    private val calculateCurrencyAmountUseCase: CalculateCurrencyAmountUseCase,
    private val getLocalPricingUseCase: GetLocalPricingUseCase,
    private val getAppSubscriptionOptionsUseCase: GetAppSubscriptionOptionsUseCase,
) {
    /**
     * Invoke
     *
     * @return [Subscription]
     */
    suspend operator fun invoke(): Subscription {
        val cheapestPlan = getAppSubscriptionOptionsUseCase(1).minBy { plan ->
            plan.amount.value
        }
        val sku = getSku(cheapestPlan.accountType)
        val localPricing = sku?.let { getLocalPricingUseCase(it) }

        return Subscription(
            accountType = cheapestPlan.accountType,
            handle = cheapestPlan.handle,
            storage = cheapestPlan.storage,
            transfer = cheapestPlan.transfer,
            amount = localPricing?.let {
                calculateCurrencyAmountUseCase(
                    it.amount,
                    it.currency
                )
            }
                ?: calculateCurrencyAmountUseCase(cheapestPlan.amount, cheapestPlan.currency),
        )
    }

    private fun getSku(accountType: AccountType) = when (accountType) {
        AccountType.PRO_LITE -> Skus.SKU_PRO_LITE_MONTH
        AccountType.PRO_I -> Skus.SKU_PRO_I_MONTH
        AccountType.PRO_II -> Skus.SKU_PRO_II_MONTH
        AccountType.PRO_III -> Skus.SKU_PRO_III_MONTH
        else -> null
    }
}