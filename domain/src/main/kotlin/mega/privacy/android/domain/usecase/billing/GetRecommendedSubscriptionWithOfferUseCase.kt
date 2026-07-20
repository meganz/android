package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.entity.account.subscriptionSkuLevel
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import javax.inject.Inject

/**
 * Get the cheapest upgrade plan that currently carries a mobile offer, used to promote a discount in
 * the landing dialog (DSN-3130).
 *
 * Plans are ordered by tier ([subscriptionSkuLevel], then price, so the monthly option comes before
 * the yearly one of the same tier). Only plans at a strictly higher tier than the current plan are
 * considered, so the dialog never promotes the current tier (any billing period) or a downgrade; the
 * cheapest such plan that has an offer is returned. All billing periods are considered, so a
 * yearly-only offer is still found. Returns null when no higher-tier plan has an offer.
 *
 * @property getLocalPricingUseCase             [GetLocalPricingUseCase]
 * @property getSubscriptionOptionsUseCase      [GetSubscriptionOptionsUseCase]
 * @property getCurrentSubscriptionPlanUseCase  [GetCurrentSubscriptionPlanUseCase]
 * @property subscriptionMapper                 [SubscriptionMapper]
 */
class GetRecommendedSubscriptionWithOfferUseCase @Inject constructor(
    private val getLocalPricingUseCase: GetLocalPricingUseCase,
    private val getSubscriptionOptionsUseCase: GetSubscriptionOptionsUseCase,
    private val getCurrentSubscriptionPlanUseCase: GetCurrentSubscriptionPlanUseCase,
    private val subscriptionMapper: SubscriptionMapper,
    private val billingRepository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @return [Subscription]? the cheapest upgrade plan with an active offer, or null if none
     */
    suspend operator fun invoke(): Subscription? {
        val currentPlan = getCurrentSubscriptionPlanUseCase()
        val availablePlans = getSubscriptionOptionsUseCase()
            .filter { it.sku.subscriptionSkuLevel != Skus.NO_LEVEL }
            .sortedWith(compareBy({ it.sku.subscriptionSkuLevel }, { it.amount.value }))

        val currentLevel = availablePlans
            .firstOrNull { it.accountType == currentPlan }
            ?.sku.subscriptionSkuLevel

        val offerPlan = availablePlans
            .filter { it.sku.subscriptionSkuLevel > currentLevel }
            .firstOrNull { it.hasOffer }
            ?: return null

        // Pre-fetch the SKU into the billing cache so getLocalPricingUseCase can resolve local pricing.
        billingRepository.querySkus(listOf(offerPlan.sku))
        val localPricing = getLocalPricingUseCase(offerPlan.sku)
        return subscriptionMapper(offerPlan, localPricing)
    }
}
