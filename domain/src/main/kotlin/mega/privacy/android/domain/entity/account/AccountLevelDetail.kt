package mega.privacy.android.domain.entity.account

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus

/**
 * Account level detail
 *
 * @property accountType
 * @property subscriptionStatus
 * @property subscriptionRenewTime
 * @property proExpirationTime
 */
class AccountLevelDetail(
    val accountType: AccountType?,
    val subscriptionStatus: SubscriptionStatus?,
    val subscriptionRenewTime: Long,
    val proExpirationTime: Long,
)