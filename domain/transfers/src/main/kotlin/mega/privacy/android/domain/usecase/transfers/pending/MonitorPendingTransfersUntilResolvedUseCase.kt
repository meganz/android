package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformWhile
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import javax.inject.Inject

/**
 * Use case to monitor the pending transfers while there are not yet resolved pending transfers, so they still need to be started or the SDK is still scanning them
 */
class MonitorPendingTransfersUntilResolvedUseCase @Inject constructor(
    private val getPendingTransfersByTypeUseCase: GetPendingTransfersByTypeUseCase,
) {
    /**
     * Invoke
     */
    operator fun invoke(transferType: TransferType): Flow<List<PendingTransfer>> =
        getPendingTransfersByTypeUseCase(transferType)
            .distinctUntilChanged()
            .transformWhile { pendingTransfers ->
                emit(pendingTransfers)
                pendingTransfers.filterNot { it.resolved() }.any()
            }
}