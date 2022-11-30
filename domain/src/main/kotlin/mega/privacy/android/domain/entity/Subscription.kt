package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.account.CurrencyAmount

/**
 * Subscription
 *
 * @property accountType     Account type (PRO I, PRO II, PRO III, PRO LITE, etc.)
 * @property handle          Subscription plan handle
 * @property storage         Amount of storage of the subscription plan
 * @property transfer        Amount of transfer quota of the subscription plan
 * @property amount          Price amount for 1 month of the subscription plan
 * @property currency        Currency of the subscription plan
 */
data class Subscription(
    val accountType: AccountType,
    val handle: Long,
    val storage: Int,
    val transfer: Int,
    val amount: CurrencyAmount,
    val currency: Currency,
)