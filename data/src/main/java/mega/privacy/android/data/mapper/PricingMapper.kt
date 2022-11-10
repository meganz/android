package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Pricing

typealias PricingMapper = (
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Currency?,
    @JvmSuppressWildcards String?,
) -> @JvmSuppressWildcards Pricing

internal fun toPricing(
    amount: Int,
    currency: Currency?,
    sku: String?,
) = Pricing(
    amount = amount,
    currency = currency,
    sku = sku
)