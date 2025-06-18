package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import javax.inject.Inject

/**
 * Get the next available subscription plan based on the current subscription.
 * The logic follows these rules:
 * 1. If the current plan exists in the available plans:
 *    - Returns the next higher plan in the sorted list
 *    - If current plan is the highest, returns null as there is no higher plan available
 * 2. If the current plan doesn't exist in available plans:
 *    - Returns the first (cheapest) plan from the sorted list as there is no current plan to compare with
 *
 * @property calculateCurrencyAmountUseCase     [CalculateCurrencyAmountUseCase]
 * @property getLocalPricingUseCase             [GetLocalPricingUseCase]
 * @property getAppSubscriptionOptionsUseCase   [GetAppSubscriptionOptionsUseCase]
 * @property getCurrentSubscriptionPlanUseCase  [GetCurrentSubscriptionPlanUseCase]
 */
class GetRecommendedSubscriptionUseCase @Inject constructor(
    private val calculateCurrencyAmountUseCase: CalculateCurrencyAmountUseCase,
    private val getLocalPricingUseCase: GetLocalPricingUseCase,
    private val getAppSubscriptionOptionsUseCase: GetAppSubscriptionOptionsUseCase,
    private val getCurrentSubscriptionPlanUseCase: GetCurrentSubscriptionPlanUseCase,
) {
    /**
     * Invoke
     *
     * @return [Subscription]? The next available subscription plan based on the current plan, or null if current plan is the highest available
     */
    suspend operator fun invoke(): Subscription? {
        val currentPlan = getCurrentSubscriptionPlanUseCase()
        val availablePlans = getAppSubscriptionOptionsUseCase(1)
            .filter { it.accountType != AccountType.FREE }
            .sortedBy { it.amount.value }

        val currentPlanIndex = availablePlans.indexOfFirst { it.accountType == currentPlan }
        // if current plan is not found or is the highest, return null
        val cheapestPlan = availablePlans.getOrNull(currentPlanIndex + 1) ?: return null

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
            } ?: calculateCurrencyAmountUseCase(cheapestPlan.amount, cheapestPlan.currency),
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