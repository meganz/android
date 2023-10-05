package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.transformWhile
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.TransfersConstants.ON_TRANSFER_UPDATE_REFRESH_MILLIS
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorDownloadTransfersPausedUseCase
import javax.inject.Inject

/**
 * Use case to get a flow of ongoing active transfers and it's paused state. Mainly created to update the notification in the related Worker.
 *
 * Once all ongoing active transfers of this type finish a last value is emitted and the flow ends, indicating that the notification can be dismissed and the worker can finish.
 * If there are no ongoing active transfers it will return a flow with just the current active transfer totals (all 0 in this case) and ends
 * Paused is true if if transfers are paused globally or all individual transfers are paused.
 * Active transfers monitoring is sampled to avoid too much updates.
 */
class MonitorOngoingActiveTransfersUseCase @Inject constructor(
    private val monitorActiveTransferTotalsUseCase: MonitorActiveTransferTotalsUseCase,
    private val getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    private val monitorDownloadTransfersPausedUseCase: MonitorDownloadTransfersPausedUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
) {

    /**
     * Invoke
     */
    @OptIn(FlowPreview::class)
    operator fun invoke(transferType: TransferType): Flow<MonitorOngoingActiveTransfersResult> {
        val transfersFlow = monitorActiveTransferTotalsUseCase(transferType)
            .sample(ON_TRANSFER_UPDATE_REFRESH_MILLIS)
            .onStart { emit(getActiveTransferTotalsUseCase(transferType)) }
        val pausedFlow = monitorDownloadTransfersPausedUseCase()
        val overQuotaFlow = monitorTransferOverQuotaUseCase().onStart { emit(false) }

        return combine(
            transfersFlow,
            pausedFlow,
            overQuotaFlow
        ) { transferTotals, paused, overQuota ->
            MonitorOngoingActiveTransfersResult(transferTotals, paused, overQuota)
        }.transformWhile {
            emit(it)
            it.activeTransferTotals.hasOngoingTransfers() && !it.overQuota
        }
    }
}