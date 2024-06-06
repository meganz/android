package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Monitor active transfers and emits last total files transfers once the totals is back to 0 (that means it has finished)
 */
class MonitorActiveTransferFinishedUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke
     * @return flow emitting last non-zero totalFileTransfers when it goes back to 0 (unless they all were already downloaded files)
     */
    operator fun invoke(transferType: TransferType) =
        transferRepository.getActiveTransferTotalsByType(transferType)
            .distinctUntilChanged(areEquivalent = { old, new ->
                // transferredBytes are not important here, this helps distinctUntilChanged emit much less values
                old.copy(transferredBytes = new.transferredBytes) == new
            })
            .scan(emptyTotals(transferType) to emptyTotals(transferType)) { (_, prev), new ->
                prev to new
            }.mapNotNull { (prev, current) ->
                prev.takeIf {
                    prev.totalCompletedFileTransfers > 0 && current.totalCompletedFileTransfers == 0
                            && prev.totalCompletedFileTransfers > prev.totalAlreadyDownloadedFiles
                }?.totalCompletedFileTransfers
            }

    private fun emptyTotals(transferType: TransferType) = ActiveTransferTotals(
        transfersType = transferType,
        totalTransfers = 0,
        totalFileTransfers = 0,
        pausedFileTransfers = 0,
        totalFinishedTransfers = 0,
        totalFinishedFileTransfers = 0,
        totalCompletedFileTransfers = 0,
        totalBytes = 0L,
        transferredBytes = 0L,
        totalAlreadyDownloadedFiles = 0
    )
}