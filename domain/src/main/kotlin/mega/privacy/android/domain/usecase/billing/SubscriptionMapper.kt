package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.SubscriptionOption
import javax.inject.Inject

/**
 * Mapper to convert SubscriptionOption and LocalPricing to Subscription
 */
class SubscriptionMapper @Inject constructor(
    private val calculateCurrencyAmountUseCase: CalculateCurrencyAmountUseCase,
) {
    /**
     * Map SubscriptionOption and LocalPricing to Subscription
     * @param plan The subscription option plan
     * @param localPricing The local pricing information (can be null)
     * @return Subscription object
     */
    operator fun invoke(
        plan: SubscriptionOption,
        localPricing: LocalPricing?
    ): Subscription {
        // Pick the first offer if available, we may need to allow user to choose offers in future
        val offerDetail = localPricing?.offers?.firstOrNull()

        return Subscription(
            accountType = plan.accountType,
            handle = plan.handle,
            storage = plan.storage,
            transfer = plan.transfer,
            amount = localPricing?.let {
                calculateCurrencyAmountUseCase(
                    it.amount,
                    it.currency
                )
            } ?: calculateCurrencyAmountUseCase(plan.amount, plan.currency),
            discountedAmountMonthly = offerDetail?.discountedPriceMonthly?.let {
                calculateCurrencyAmountUseCase(
                    currencyPoint = it,
                    currency = localPricing.currency
                )
            },
            discountedPercentage = offerDetail?.discountPercentage,
            offerId = offerDetail?.offerId,
            offerPeriod = offerDetail?.offerPeriod,
            sku = plan.sku,
        )
    }
}
