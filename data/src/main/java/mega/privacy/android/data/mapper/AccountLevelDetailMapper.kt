package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import javax.inject.Inject

/**
 * Account Level Detail Mapper
 */
internal class AccountLevelDetailMapper @Inject constructor() {
    /**
     * Invoke
     * @return [AccountLevelDetail]
     * @param  subscriptionRenewTime
     * @param  proExpirationTime
     * @param  accountType
     * @param  subscriptionStatus
     * @param  subscriptionRenewCycleType
     */
    operator fun invoke(
        subscriptionRenewTime: Long,
        proExpirationTime: Long,
        accountType: AccountType?,
        subscriptionStatus: SubscriptionStatus?,
        subscriptionRenewCycleType: AccountSubscriptionCycle,
    ) = AccountLevelDetail(
        subscriptionStatus = subscriptionStatus,
        accountType = accountType,
        proExpirationTime = proExpirationTime,
        subscriptionRenewTime = subscriptionRenewTime,
        accountSubscriptionCycle = subscriptionRenewCycleType
    )
}