package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.account.CurrencyPoint

/**
 * Subscription Option
 *
 * @property accountType     Account type (PRO I, PRO II, PRO III, PRO LITE, etc.)
 * @property months          Number of subscription months (1 for monthly or 12 for yearly)
 * @property handle          Subscription plan handle
 * @property storage         Amount of storage of the subscription plan
 * @property transfer        Amount of transfer quota of the subscription plan
 * @property amount          Price amount of the subscription plan
 * @property currency        Currency of the subscription plan
 */
data class SubscriptionOption(
    val accountType: AccountType,
    val months: Int,
    val handle: Long,
    val storage: Int,
    val transfer: Int,
    val amount: CurrencyPoint.SystemCurrencyPoint,
    val currency: Currency,
)