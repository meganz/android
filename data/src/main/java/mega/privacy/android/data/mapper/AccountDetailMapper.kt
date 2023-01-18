package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.account.AccountDetail
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaNode

internal typealias AccountDetailMapper = (
    @JvmSuppressWildcards MegaAccountDetails,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards MegaNode?,
    @JvmSuppressWildcards MegaNode?,
    @JvmSuppressWildcards List<MegaNode>,
) -> @JvmSuppressWildcards AccountDetail

internal const val HAS_STORAGE_DETAILS = 0x01
internal const val HAS_TRANSFER_DETAILS = 0x02
internal const val HAS_PRO_DETAILS = 0x04
internal const val HAS_SESSIONS_DETAILS = 0x020

internal fun toAccountDetail(
    details: MegaAccountDetails,
    numDetails: Int,
    rootNode: MegaNode?,
    rubbishNode: MegaNode?,
    inShares: List<MegaNode>,
    accountStorageDetailMapper: AccountStorageDetailMapper,
    accountSessionDetailMapper: AccountSessionDetailMapper,
    accountTransferDetailMapper: AccountTransferDetailMapper,
    accountLevelDetailMapper: AccountLevelDetailMapper,
    accountTypeMapper: AccountTypeMapper,
    subscriptionStatusMapper: SubscriptionStatusMapper,
) = AccountDetail(
    sessionDetail = details.takeIf { numDetails and HAS_SESSIONS_DETAILS != 0 }
        ?.getSession(0)
        ?.let {
            accountSessionDetailMapper(it.mostRecentUsage, it.creationTimestamp)
        },
    transferDetail = details.takeIf { numDetails and HAS_TRANSFER_DETAILS != 0 }
        ?.let { accountTransferDetailMapper(it.transferMax, it.transferUsed) },
    levelDetail = details.takeIf { numDetails and HAS_PRO_DETAILS != 0 }?.let {
        accountLevelDetailMapper(it.subscriptionRenewTime,
            it.proExpiration,
            accountTypeMapper(it.proLevel),
            subscriptionStatusMapper(it.subscriptionStatus))
    },
    storageDetail = details.takeIf { numDetails and HAS_STORAGE_DETAILS != 0 }?.let {
        accountStorageDetailMapper(
            details,
            rootNode,
            rubbishNode,
            inShares,
            details.storageMax,
            details.storageUsed,
            details.subscriptionMethodId
        )
    },
)