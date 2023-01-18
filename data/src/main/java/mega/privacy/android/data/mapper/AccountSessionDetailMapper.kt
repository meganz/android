package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.account.AccountSessionDetail

internal typealias AccountSessionDetailMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Long,
) -> @JvmSuppressWildcards AccountSessionDetail

internal fun toAccountSessionDetail(
    mostRecentSessionTimeStamp: Long,
    createSessionTimeStamp: Long,
) = AccountSessionDetail(
    mostRecentSessionTimeStamp = mostRecentSessionTimeStamp,
    createSessionTimeStamp = createSessionTimeStamp,
)