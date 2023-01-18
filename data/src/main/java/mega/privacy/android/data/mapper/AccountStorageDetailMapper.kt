package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.account.AccountStorageDetail
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaNode

internal typealias AccountStorageDetailMapper = (
    @JvmSuppressWildcards MegaAccountDetails,
    @JvmSuppressWildcards MegaNode?,
    @JvmSuppressWildcards MegaNode?,
    @JvmSuppressWildcards List<MegaNode>,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Int,
) -> @JvmSuppressWildcards AccountStorageDetail

internal fun toAccountStorageDetail(
    details: MegaAccountDetails,
    rootNode: MegaNode?,
    rubbishNode: MegaNode?,
    inShares: List<MegaNode>,
    totalStorage: Long,
    usedStorage: Long,
    subscriptionMethodId: Int,
) = AccountStorageDetail(
    usedCloudDrive = rootNode?.let { node -> details.getStorageUsed(node.handle) } ?: 0L,
    usedRubbish = rubbishNode?.let { node -> details.getStorageUsed(node.handle) } ?: 0L,
    usedIncoming = inShares.sumOf { node -> details.getStorageUsed(node.handle) },
    totalStorage = totalStorage,
    usedStorage = usedStorage,
    subscriptionMethodId = subscriptionMethodId,
)