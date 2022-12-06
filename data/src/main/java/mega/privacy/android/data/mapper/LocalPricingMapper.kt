package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.account.MegaSku

/**
 * Map [MegaSku] to [LocalPricing]
 */
typealias LocalPricingMapper = (@JvmSuppressWildcards MegaSku) -> @JvmSuppressWildcards LocalPricing

/**
 * Map [MegaSku] to [LocalPricing]
 */

internal fun toLocalPricing(megaSku: MegaSku) =
    LocalPricing(
        amount = CurrencyPoint.LocalCurrencyPoint(megaSku.priceAmountMicros),
        currency = Currency(megaSku.priceCurrencyCode),
        sku = megaSku.sku
    )