package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
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
     * @return flow emitting last non-zero totalCompletedFileTransfers when it goes back to 0 (please note that completed file transfers doesn't include transfers finished with error or because they were already downloaded)
     */
    operator fun invoke(transferType: TransferType) =
        transferRepository.getActiveTransferTotalsByType(transferType)
            .map { it.totalCompletedFileTransfers }
            .distinctUntilChanged()
            .scan(0 to 0) { (_, prev), new ->
                prev to new
            }.mapNotNull { (prev, current) ->
                prev.takeIf { prev > 0 && current == 0 }
            }
}