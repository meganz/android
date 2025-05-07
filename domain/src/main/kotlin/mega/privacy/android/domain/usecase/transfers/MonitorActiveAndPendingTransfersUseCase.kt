package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mega.privacy.android.domain.entity.transfer.TransferProgressResult
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.pending.GetPendingTransfersByTypeUseCase
import javax.inject.Inject

/**
 * Use case to monitor Transfer Progress of a specific type and know whether there are pending work to do or not.
 * It checks ongoing ActiveTransfers and Pending transfers to decide if there are still pending work to be done.
 */
class MonitorActiveAndPendingTransfersUseCase @Inject constructor(
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
    private val getPendingTransfersByTypeUseCase: GetPendingTransfersByTypeUseCase,
) {
    /**
     * Invoke the use case
     *
     * @param transferType [TransferType] that will be checked.
     */
    operator fun invoke(transferType: TransferType): Flow<TransferProgressResult> =
        combine(
            monitorOngoingActiveTransfersUseCase(transferType),
            getPendingTransfersByTypeUseCase(transferType),
        ) { ongoingActiveTransfersResult, pendingTransfersNotSend ->
            //keep monitoring if and only if there are pending transfers or transfers in progress
            TransferProgressResult(
                monitorOngoingActiveTransfersResult = ongoingActiveTransfersResult,
                pendingTransfers = pendingTransfersNotSend.isNotEmpty(),
                ongoingTransfers = ongoingActiveTransfersResult.hasPendingWork(transferType),
            )
        }
}