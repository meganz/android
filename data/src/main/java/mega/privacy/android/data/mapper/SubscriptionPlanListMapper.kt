package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SubscriptionPlan
import nz.mega.sdk.MegaRequest

/**
 * Map [MegaRequest], [SubscriptionPlanMapper], [CurrencyMapper], [SkuMapper] to [List<SubscriptionPlan>]
 */
typealias SubscriptionPlanListMapper = (@JvmSuppressWildcards MegaRequest, @JvmSuppressWildcards SubscriptionPlanMapper, @JvmSuppressWildcards CurrencyMapper, @JvmSuppressWildcards SkuMapper) -> @JvmSuppressWildcards List<@JvmSuppressWildcards SubscriptionPlan>

internal fun toSubscriptionPlanList(
    request: MegaRequest,
    subscriptionPlanMapper: SubscriptionPlanMapper,
    currencyMapper: CurrencyMapper,
    skuMapper: SkuMapper,
): List<SubscriptionPlan> {
    val currency = request.currency
    val pricing = request.pricing

    return (0 until request.pricing.numProducts).map {
        val proLevel = toAccountType(pricing.getProLevel(it))
        val months = pricing.getMonths(it)
        subscriptionPlanMapper(
            pricing.getHandle(it),
            proLevel,
            months,
            pricing.getGBStorage(it),
            pricing.getGBTransfer(it),
            pricing.getAmount(it),
            currencyMapper(currency.currencyName),
            skuMapper(proLevel,
                months),
        )
    }
}
