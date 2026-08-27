package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.entity.account.subscriptionSkuLevel
import mega.privacy.android.domain.entity.billing.RecommendedSubscriptionOffer
import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Get the cheapest plan that currently carries a mobile offer, used to promote a discount in the
 * landing dialog (DSN-3130).
 *
 * The account's own plan is deliberately not taken into account: campaigns pick their audience
 * server side, so whichever plan is discounted is promoted to paid and free accounts alike, even
 * when the account already sits on a higher tier. Plans are ordered by tier
 * ([subscriptionSkuLevel], then price, so the monthly option comes before the yearly one of the
 * same tier) and the cheapest one carrying an offer is returned. All billing periods are
 * considered, so a yearly-only offer is still found. Returns null when no plan has an offer.
 *
 * An offer is only promoted when its flags bitmask opts in via the mobile-offer visible bit; offers
 * without that bit, and offers carrying no flags at all, are discounted silently. They still apply
 * their discount on the upgrade screen, they are just never advertised on the Home banner, the menu
 * banner, or the offer landing screen.
 *
 * [RecommendedSubscriptionOffer.hasMultipleOffers] reports whether the campaign discounts more than
 * one plan, so the dialog can link to the full list of plans.
 *
 * @property getLocalPricingUseCase             [GetLocalPricingUseCase]
 * @property getSubscriptionOptionsUseCase      [GetSubscriptionOptionsUseCase]
 * @property subscriptionMapper                 [SubscriptionMapper]
 */
class GetRecommendedSubscriptionWithOfferUseCase @Inject constructor(
    private val getLocalPricingUseCase: GetLocalPricingUseCase,
    private val getSubscriptionOptionsUseCase: GetSubscriptionOptionsUseCase,
    private val subscriptionMapper: SubscriptionMapper,
    private val billingRepository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @return the cheapest plan with an active offer, or null if none
     */
    suspend operator fun invoke(): RecommendedSubscriptionOffer? {
        val advertisedPlans = getSubscriptionOptionsUseCase()
            .filter { it.sku.subscriptionSkuLevel != Skus.NO_LEVEL }
            .filter { it.hasOffer && it.isOfferVisible }
            .sortedWith(compareBy({ it.sku.subscriptionSkuLevel }, { it.amount.value }))
        if (advertisedPlans.isEmpty()) return null

        val skus = advertisedPlans.map { it.sku }.distinct()
        val products = billingRepository.querySkus(skus).associateBy { it.sku }

        val plansWithOffer = advertisedPlans
            .filter { products[it.sku]?.offers.orEmpty().isNotEmpty() }

        val offerPlan = plansWithOffer.firstOrNull() ?: return null

        val localPricing = getLocalPricingUseCase(offerPlan.sku)
        return RecommendedSubscriptionOffer(
            subscription = subscriptionMapper(offerPlan, localPricing),
            hasMultipleOffers = plansWithOffer.distinctBy { it.accountType }.size > 1,
        )
    }
}

/**
 * Bit 0 of the mobile offer flags bitmask (utqa "mo.f"): the campaign opts in to being advertised
 * on the mobile promotion surfaces. Absent flags are treated as opted out.
 */
private const val MOBILE_OFFER_VISIBLE_FLAG = 1L

private val SubscriptionOption.isOfferVisible: Boolean
    get() = (offerFlags ?: 0L) and MOBILE_OFFER_VISIBLE_FLAG != 0L
