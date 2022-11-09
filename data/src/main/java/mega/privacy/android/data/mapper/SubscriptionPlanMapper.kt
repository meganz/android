package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.SubscriptionPlan

/**
 *  Mapper for Subscription plan
 *
 *  handle     Subscription plan handle
 *  level      Account type (PRO I, PRO II, PRO III, PRO LITE, etc.)
 *  months     Number of subscription months (1 for monthly or 12 for yearly)
 *  storage    Amount of storage of the subscription plan
 *  transfer   Amount of transfer quota of the subscription plan
 *  amount     Amount or price of the subscription plan
 *  currency   Currency of the subscription plan
 *  sku        SKU of the subscription plan
 */
typealias SubscriptionPlanMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards AccountType?,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Currency?,
    @JvmSuppressWildcards String?,
) -> @JvmSuppressWildcards SubscriptionPlan

internal fun toSubscriptionPlan(
    handle: Long,
    level: AccountType?,
    months: Int,
    storage: Int,
    transfer: Int,
    amount: Int,
    currency: Currency?,
    sku: String?,
): SubscriptionPlan {
    return SubscriptionPlan(
        pricing = toPricing(amount, currency, sku),
        subscription = Subscription(
            handle = handle,
            level = level,
            months = months,
            storage = storage,
            transfer = transfer))
}