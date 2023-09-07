package mega.privacy.android.domain.usecase.transfer.activetransfers

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.transformWhile
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfer.monitorpaused.MonitorDownloadTransfersPausedUseCase
import javax.inject.Inject

/**
 * Use case to get a flow of ongoing active download transfers and it's paused state. Mainly created to update the notification in the DownloadsWorker.
 *
 * Once all ongoing active download transfers finish a last value is emitted and the flow ends, indicating that the notification can be dismissed and the worker can finish.
 * If there are no ongoing active transfers it will return a flow with just the current active transfer totals (all 0 in this case) and ends
 * Paused is true if if transfers are paused globally or all individual transfers are paused.
 * Active transfers monitoring is sampled to avoid too much updates.
 */
class MonitorOngoingActiveDownloadTransfersUseCase @Inject constructor(
    private val monitorActiveTransferTotalsUseCase: MonitorActiveTransferTotalsUseCase,
    private val getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    private val monitorDownloadTransfersPausedUseCase: MonitorDownloadTransfersPausedUseCase,
) {

    /**
     * Invoke
     */
    @OptIn(FlowPreview::class)
    operator fun invoke(refreshSample: Long = ON_TRANSFER_UPDATE_REFRESH_MILLIS): Flow<MonitorOngoingActiveTransfersResult> {
        val transfersFlow = monitorActiveTransferTotalsUseCase(TransferType.TYPE_DOWNLOAD)
            .sample(refreshSample)
            .onStart { emit(getActiveTransferTotalsUseCase(TransferType.TYPE_DOWNLOAD)) }
        val pausedFlow = monitorDownloadTransfersPausedUseCase()

        return combine(transfersFlow, pausedFlow) { transferTotals, paused ->
            MonitorOngoingActiveTransfersResult(transferTotals, paused)
        }.transformWhile {
            emit(it)
            it.activeTransferTotals.hasOngoingTransfers()
        }
    }

    companion object {
        private const val ON_TRANSFER_UPDATE_REFRESH_MILLIS = 1000L
    }
}