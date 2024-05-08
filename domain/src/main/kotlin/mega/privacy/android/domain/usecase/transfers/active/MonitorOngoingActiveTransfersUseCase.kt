package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.camerauploads.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorDownloadTransfersPausedUseCase
import javax.inject.Inject

/**
 * Use case to get a flow of ongoing active transfers and it's paused state. Mainly created as base use case to update the notification in the related Workers.
 *
 * If there are no ongoing active transfers it will return a flow with just the current active transfer totals (all 0 in this case)
 * Paused is true if transfers are paused globally or all individual transfers are paused.
 */
class MonitorOngoingActiveTransfersUseCase @Inject constructor(
    private val monitorActiveTransferTotalsUseCase: MonitorActiveTransferTotalsUseCase,
    private val getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    private val monitorDownloadTransfersPausedUseCase: MonitorDownloadTransfersPausedUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
    private val monitorStorageOverQuotaUseCase: MonitorStorageOverQuotaUseCase,
) {

    /**
     * Invoke
     */
    operator fun invoke(transferType: TransferType): Flow<MonitorOngoingActiveTransfersResult> {
        val transfersFlow = monitorActiveTransferTotalsUseCase(transferType)
            .onStart { emit(getActiveTransferTotalsUseCase(transferType)) }
        val pausedFlow = monitorDownloadTransfersPausedUseCase()
        val transferOverQuotaFlow = monitorTransferOverQuotaUseCase().onStart { emit(false) }
        val storageOverQuotaFlow = monitorStorageOverQuotaUseCase().onStart { emit(false) }

        return combine(
            transfersFlow,
            pausedFlow,
            transferOverQuotaFlow,
            storageOverQuotaFlow,
        ) { transferTotals, paused, transfersOverQuota, storageOverQuota ->
            MonitorOngoingActiveTransfersResult(
                transferTotals,
                paused,
                transfersOverQuota,
                storageOverQuota
            )
        }
    }
}