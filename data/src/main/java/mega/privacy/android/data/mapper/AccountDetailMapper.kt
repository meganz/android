package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.account.AccountDetail
import nz.mega.sdk.MegaAccountDetails
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
            accountLevelDetailMapper(
                it.subscriptionRenewTime,
                it.proExpiration,
                accountTypeMapper(it.proLevel),
                subscriptionStatusMapper(it.subscriptionStatus),
                accountSubscriptionCycleMapper(it.subscriptionCycle)
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