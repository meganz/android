package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.account.AccountTransferDetail

internal typealias AccountTransferDetailMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Long,
) -> @JvmSuppressWildcards AccountTransferDetail

internal fun toAccountTransferDetail(
    totalTransfer: Long,
    usedTransfer: Long,
) = AccountTransferDetail(
    totalTransfer = totalTransfer,
    usedTransfer = usedTransfer,
)