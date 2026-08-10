package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.account.AccountTransferDetail
import javax.inject.Inject

internal class AccountTransferDetailMapper @Inject constructor() {
    operator fun invoke(
        totalTransfer: Long,
        usedTransfer: Long,
    ): AccountTransferDetail {
        val usedTransferPercentage = when {
            totalTransfer <= 0 -> 0
            else -> ((usedTransfer.toFloat() / totalTransfer.toFloat()) * 100).toInt()
        }

        return AccountTransferDetail(
            totalTransfer = totalTransfer,
            usedTransfer = usedTransfer,
            usedTransferPercentage = usedTransferPercentage,
        )
    }
}
