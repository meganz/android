package mega.privacy.android.domain.entity.account

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus

/**
 * Account detail
 *
 * @property usedCloudDrive
 * @property usedRubbish
 * @property usedIncoming
 * @property usedStorage
 * @property subscriptionMethodId
 * @property transferMax
 * @property transferUsed
 * @property accountType
 * @property subscriptionStatus
 * @property subscriptionRenewTime
 * @property proExpirationTime
 */
data class AccountDetail(
    val usedCloudDrive: Long,
    val usedRubbish: Long,
    val usedIncoming: Long,
    val usedStorage: Long,
    val subscriptionMethodId: Int,
    val transferMax: Long,
    val transferUsed: Long,
    val accountType: AccountType?,
    val subscriptionStatus: SubscriptionStatus?,
    val subscriptionRenewTime: Long?,
    val proExpirationTime: Long?,
)