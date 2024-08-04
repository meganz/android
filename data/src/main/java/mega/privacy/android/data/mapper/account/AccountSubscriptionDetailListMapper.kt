package mega.privacy.android.data.mapper.account

import mega.privacy.android.data.mapper.AccountSubscriptionCycleMapper
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.PaymentMethodTypeMapper
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.data.mapper.SubscriptionStatusMapper
import mega.privacy.android.domain.entity.account.AccountSubscriptionDetail
import nz.mega.sdk.MegaAccountDetails
import javax.inject.Inject

/**
 * Account Subscription Detail List Mapper
 */
internal class AccountSubscriptionDetailListMapper @Inject constructor(
    private val subscriptionStatusMapper: SubscriptionStatusMapper,
    private val subscriptionCycleMapper: AccountSubscriptionCycleMapper,
    private val accountTypeMapper: AccountTypeMapper,
    private val paymentMethodTypeMapper: PaymentMethodTypeMapper,
    private val stringListMapper: StringListMapper,
) {
    /**
     * Invoke
     *
     * @param accountDetail [MegaAccountDetails]
     * @return [List<AccountSubscriptionDetail>]
     */
    operator fun invoke(
        accountDetail: MegaAccountDetails,
    ): List<AccountSubscriptionDetail> {
        var subscriptionDetailList = listOf<AccountSubscriptionDetail>()
        if (accountDetail.numSubscriptions > 0) {
            subscriptionDetailList = (0 until accountDetail.numSubscriptions).map {
                val subscriptionDetail = accountDetail.getSubscription(it)
                AccountSubscriptionDetail(
                    subscriptionId = subscriptionDetail.id,
                    subscriptionStatus = subscriptionStatusMapper(subscriptionDetail.status),
                    subscriptionCycle = subscriptionCycleMapper(subscriptionDetail.cycle),
                    paymentMethodType = paymentMethodTypeMapper(subscriptionDetail.paymentMethodId.toInt()),
                    renewalTime = subscriptionDetail.renewTime,
                    subscriptionLevel = accountTypeMapper(subscriptionDetail.accountLevel.toInt()),
                    featuresList = stringListMapper(subscriptionDetail.features),
                    isFreeTrial = subscriptionDetail.isTrial,
                )
            }
        }
        return subscriptionDetailList
    }
}