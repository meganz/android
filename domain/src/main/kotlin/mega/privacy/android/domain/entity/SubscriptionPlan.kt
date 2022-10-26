package mega.privacy.android.domain.entity

/**
 * Subscription plan
 *
 * @property handle     Subscription plan handle
 * @property level      Account type (PRO I, PRO II, PRO III, PRO LITE, etc.)
 * @property months     Number of subscription months (1 for monthly or 12 for yearly)
 * @property storage    Amount of storage of the subscription plan
 * @property transfer   Amount of transfer quota of the subscription plan
 * @property amount     Amount or price of the subscription plan
 * @property currency   Currency of the subscription plan
 * @property sku        SKU of the subscription plan
 */
data class SubscriptionPlan(
    val handle: Long,
    val level: AccountType?,
    val months: Int,
    val storage: Int,
    val transfer: Int,
    val amount: Int,
    val currency: Currency?,
    val sku: String?,
)
