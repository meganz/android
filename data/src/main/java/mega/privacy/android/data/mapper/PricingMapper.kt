package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.billing.Pricing
import nz.mega.sdk.MegaCurrency
import nz.mega.sdk.MegaPricing

/**
 * Map [MegaPricing] and [MegaCurrency] to [Pricing]
 */
internal typealias PricingMapper = (
    @JvmSuppressWildcards MegaPricing,
    @JvmSuppressWildcards MegaCurrency,
) -> @JvmSuppressWildcards Pricing

internal fun toPricing(pricing: MegaPricing, currency: MegaCurrency) = Pricing(
    (0 until pricing.numProducts).map { i ->
        Product(
            pricing.getHandle(i),
            pricing.getProLevel(i),
            pricing.getMonths(i),
            pricing.getGBStorage(i),
            pricing.getGBTransfer(i),
            pricing.getAmount(i),
            currency.currencyName,
            pricing.isBusinessType(i)
        )
    }
)