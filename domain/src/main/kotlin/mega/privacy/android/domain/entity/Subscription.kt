package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.account.CurrencyAmount

/**
 * Subscription
 *
 * @property accountType     Account type (PRO I, PRO II, PRO III, PRO LITE, etc.)
 * @property handle          Subscription option handle
 * @property storage         Amount of storage of the subscription option
 * @property transfer        Amount of transfer quota of the subscription option
 * @property amount          Currency amount object, containing price amount for 1 month for subscription option and local currency
 */
data class Subscription(
    val accountType: AccountType,
    val handle: Long,
    val storage: Int,
    val transfer: Int,
    val amount: CurrencyAmount,
)