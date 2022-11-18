package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.account.AccountDetail
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaNode

internal typealias AccountDetailMapper = suspend (
    @JvmSuppressWildcards MegaAccountDetails,
    @JvmSuppressWildcards MegaNode?,
    @JvmSuppressWildcards MegaNode?,
    @JvmSuppressWildcards List<MegaNode>?,
    @JvmSuppressWildcards GetStorageUsed,
    @JvmSuppressWildcards AccountTypeMapper,
    @JvmSuppressWildcards SubscriptionStatusMapper,
) -> @JvmSuppressWildcards AccountDetail

internal typealias GetStorageUsed = suspend (List<MegaNode>, MegaAccountDetails) -> Long

internal suspend fun toAccountDetail(
    details: MegaAccountDetails,
    rootNode: MegaNode?,
    rubbishNode: MegaNode?,
    inShares: List<MegaNode>?,
    getStorageUsed: GetStorageUsed,
    accountTypeMapper: AccountTypeMapper,
    subscriptionStatusMapper: SubscriptionStatusMapper,
) = AccountDetail(
    usedCloudDrive = rootNode?.let { getStorageUsed(listOf(it), details) } ?: 0L,
    usedRubbish = rubbishNode?.let { getStorageUsed(listOf(it), details) } ?: 0L,
    usedIncoming = inShares?.let { getStorageUsed(it, details) } ?: 0L,
    usedStorage = details.storageUsed,
    subscriptionMethodId = details.subscriptionMethodId,
    transferMax = details.transferMax,
    transferUsed = details.transferUsed,
    accountType = accountTypeMapper(details.proLevel),
    subscriptionStatus = subscriptionStatusMapper(details.subscriptionStatus),
    subscriptionRenewTime = details.subscriptionRenewTime,
    proExpirationTime = details.proExpiration
)