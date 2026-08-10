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
            handle = pricing.getHandle(i),
            level = pricing.getProLevel(i),
            months = pricing.getMonths(i),
            storage = pricing.getGBStorage(i),
            transfer = pricing.getGBTransfer(i),
            amount = pricing.getAmount(i),
            currency = currency.currencyName,
            isBusiness = pricing.isBusinessType(i),
            discountName = pricing.getMobileOfferLabel(i),
        )
    }
)