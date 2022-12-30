package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.account.CurrencyPoint

/**
 * Pricing
 *
 * @property amount     Price of SKU
 * @property currency   Currency code used to to format local price
 * @property sku        SKU of the subscription option
 */

data class LocalPricing(
    val amount: CurrencyPoint.LocalCurrencyPoint,
    val currency: Currency,
    val sku: String,
)