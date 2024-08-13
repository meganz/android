package mega.privacy.android.data.mapper

import mega.privacy.android.data.mapper.account.AccountPlanDetailMapper
import mega.privacy.android.data.mapper.account.AccountSubscriptionDetailListMapper
import mega.privacy.android.domain.entity.account.AccountDetail
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaAccountPlan
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Account Detail Mapper
 */
internal class AccountDetailMapper @Inject constructor(
    private val accountStorageDetailMapper: AccountStorageDetailMapper,
    private val accountSessionDetailMapper: AccountSessionDetailMapper,
    private val accountTransferDetailMapper: AccountTransferDetailMapper,
    private val accountLevelDetailMapper: AccountLevelDetailMapper,
    private val accountTypeMapper: AccountTypeMapper,
    private val subscriptionStatusMapper: SubscriptionStatusMapper,
    private val accountSubscriptionCycleMapper: AccountSubscriptionCycleMapper,
    private val accountPlanDetailMapper: AccountPlanDetailMapper,
    private val accountSubscriptionDetailListMapper: AccountSubscriptionDetailListMapper,
) {
    operator fun invoke(
        details: MegaAccountDetails,
        numDetails: Int,
        rootNode: MegaNode?,
        rubbishNode: MegaNode?,
        inShares: List<MegaNode>,
    ) = AccountDetail(
        sessionDetail = details.takeIf { numDetails and HAS_SESSIONS_DETAILS != 0 }
            ?.getSession(0)
            ?.let {
                accountSessionDetailMapper(it.mostRecentUsage, it.creationTimestamp)
            },
        transferDetail = details.takeIf { numDetails and HAS_TRANSFER_DETAILS != 0 }
            ?.let { accountTransferDetailMapper(it.transferMax, it.transferUsed) },
        levelDetail = details.takeIf { numDetails and HAS_PRO_DETAILS != 0 }?.let {
            val megaAccountPlanList = mutableListOf<MegaAccountPlan>()
            for (i in 0 until details.numPlans) {
                megaAccountPlanList.add(details.getPlan(i))
            }
            val megaAccountProPlan = megaAccountPlanList.firstOrNull { plan -> plan.isProPlan }
            accountLevelDetailMapper(
                subscriptionRenewTime = it.subscriptionRenewTime,
                proExpirationTime = it.proExpiration,
                accountType = if (details.numPlans > 0 && megaAccountProPlan?.isProPlan == true) {
                    accountTypeMapper(megaAccountProPlan.accountLevel.toInt())
                } else accountTypeMapper(it.proLevel),
                subscriptionStatus = subscriptionStatusMapper(it.subscriptionStatus),
                subscriptionRenewCycleType = accountSubscriptionCycleMapper(it.subscriptionCycle),
                planDetail = megaAccountProPlan?.takeIf { details.numPlans > 0 }
                    ?.let { proPlan -> accountPlanDetailMapper(proPlan) },
                subscriptionListDetail = accountSubscriptionDetailListMapper(details),
            )
        },
        storageDetail = details.takeIf { numDetails and HAS_STORAGE_DETAILS != 0 }?.let {
            accountStorageDetailMapper(
                details,
                rootNode,
                rubbishNode,
                inShares,
                details.storageMax,
                details.storageUsed,
                details.subscriptionMethodId,
            )
        },
    )

    internal companion object {
        const val HAS_STORAGE_DETAILS = 0x01
        const val HAS_TRANSFER_DETAILS = 0x02
        const val HAS_PRO_DETAILS = 0x04
        const val HAS_SESSIONS_DETAILS = 0x020
    }
}