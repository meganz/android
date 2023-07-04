package mega.privacy.android.data.mapper.transfer.active

import mega.privacy.android.data.database.entity.ActiveTransferTotalsEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import javax.inject.Inject

/**
 * Mapper for converting [ActiveTransferTotalsEntity] into [ActiveTransferTotals].
 */
internal class ActiveTransferTotalsMapper @Inject constructor() {
    suspend operator fun invoke(activeTransferTotals: ActiveTransferTotalsEntity?) =
        with(activeTransferTotals) {
            ActiveTransferTotals(
                transfersType = this?.transfersType ?: TransferType.NONE,
                totalTransfers = this?.totalTransfers ?: 0,
                totalFinishedTransfers = this?.totalFinishedTransfers ?: 0,
                totalBytes = this?.totalBytes ?: 0L,
                transferredBytes = this?.transferredBytes ?: 0L,
            )
        }
}