package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.AccountLevelDetail

internal typealias AccountLevelDetailMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards AccountType?,
    @JvmSuppressWildcards SubscriptionStatus?,
) -> @JvmSuppressWildcards AccountLevelDetail

internal fun toAccountLevelDetail(
    subscriptionRenewTime: Long,
    proExpirationTime: Long,
    accountType: AccountType?,
    subscriptionStatus: SubscriptionStatus?,
) = AccountLevelDetail(
    subscriptionStatus = subscriptionStatus,
    accountType = accountType,
    proExpirationTime = proExpirationTime,
    subscriptionRenewTime = subscriptionRenewTime
)