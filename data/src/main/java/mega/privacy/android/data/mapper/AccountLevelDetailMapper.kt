package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountPlanDetail
import mega.privacy.android.domain.entity.account.AccountSubscriptionDetail
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
     * @param  planDetail
     * @param  subscriptionListDetail
     * @param  subscriptionMethodId
     *
     * @return [AccountLevelDetail]
     */
    operator fun invoke(
        subscriptionRenewTime: Long,
        proExpirationTime: Long,
        accountType: AccountType?,
        subscriptionStatus: SubscriptionStatus?,
        subscriptionRenewCycleType: AccountSubscriptionCycle,
        planDetail: AccountPlanDetail?,
        subscriptionListDetail: List<AccountSubscriptionDetail>,
        subscriptionMethodId: Int = 0,
    ) = AccountLevelDetail(
        subscriptionStatus = subscriptionStatus,
        accountType = accountType,
        proExpirationTime = proExpirationTime,
        subscriptionRenewTime = subscriptionRenewTime,
        accountSubscriptionCycle = subscriptionRenewCycleType,
        accountPlanDetail = planDetail,
        accountSubscriptionDetailList = subscriptionListDetail,
        subscriptionMethodId = subscriptionMethodId,
    )
}
