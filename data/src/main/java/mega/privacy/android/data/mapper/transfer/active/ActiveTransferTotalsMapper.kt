package mega.privacy.android.data.mapper.transfer.active

import mega.privacy.android.data.database.entity.ActiveTransferTotalsEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import javax.inject.Inject

internal class ActiveTransferTotalsMapper @Inject constructor() {
    suspend operator fun invoke(activeTransferTotals: ActiveTransferTotalsEntity) =
        with(activeTransferTotals) {
            ActiveTransferTotals(
                transfersType = transfersType,
                totalTransfers = totalTransfers,
                totalFinishedTransfers = totalFinishedTransfers,
                totalBytes = totalBytes,
                transferredBytes = transferredBytes,
            )
        }
}