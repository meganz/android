package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformWhile
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.TransferType
import javax.inject.Inject

/**
 * Use case to get a flow of ongoing active transfers and it's paused state. Mainly created to update the notification in the related Worker.
 *
 * Once all ongoing active transfers of this type finish a last value is emitted and the flow ends, indicating that the notification can be dismissed and the worker can finish.
 * If there are no ongoing active transfers it will return a flow with just the current active transfer totals (all 0 in this case) and ends
 * Paused is true if transfers are paused globally or all individual transfers are paused.
 */
class MonitorOngoingActiveTransfersUntilFinishedUseCase @Inject constructor(
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
) {

    /**
     * Invoke
     */
    operator fun invoke(transferType: TransferType): Flow<MonitorOngoingActiveTransfersResult> =
        monitorOngoingActiveTransfersUseCase(transferType).transformWhile {
            emit(it)
            it.activeTransferTotals.hasOngoingTransfers()
        }
}