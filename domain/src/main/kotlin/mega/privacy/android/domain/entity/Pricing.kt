package mega.privacy.android.domain.entity

/**
 * Pricing
 *
 * @property amount     Amount or price of the subscription plan
 * @property currency   Currency of the subscription plan
 * @property sku        SKU of the subscription plan
 */

data class Pricing(
    val amount: Int,
    val currency: Currency?,
    val sku: String?,
)