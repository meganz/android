package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.account.OfferDetail

/**
 * Pricing
 *
 * @property amount     Price of SKU
 * @property currency   Currency code used to to format local price
 * @property sku        SKU of the subscription option
 * @property offers     List of all available offers for this SKU
 */

data class LocalPricing(
    val amount: CurrencyPoint.LocalCurrencyPoint,
    val currency: Currency,
    val sku: String,
    val offers: List<OfferDetail> = emptyList(),
)