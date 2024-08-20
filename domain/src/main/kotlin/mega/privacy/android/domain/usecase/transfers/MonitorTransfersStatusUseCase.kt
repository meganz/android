package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreAllTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.GetNumPendingUploadsUseCase
import javax.inject.Inject

/**
 * Monitor active transfers and emits [TransfersStatusInfo]
 */
class MonitorTransfersStatusUseCase @Inject constructor(
    private val repository: TransferRepository,
    private val getNumPendingUploadsUseCase: GetNumPendingUploadsUseCase,
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
    private val areTransfersPaused: AreAllTransfersPausedUseCase,
) {
    private val transferMap: MutableMap<Int, Transfer> = hashMapOf()

    /**
     * Invoke.
     *
     * @return Flow of [TransfersStatusInfo]
     */

    operator fun invoke(activeTransfersInCameraUploadsFlag: Boolean): Flow<TransfersStatusInfo> =
        when {
            activeTransfersInCameraUploadsFlag -> {
                invokeNew(TransferType.entries.filter { it != TransferType.NONE })
            }

            else -> {
                invokeNew(TransferType.entries.filter {
                    it != TransferType.NONE && it != TransferType.CU_UPLOAD
                }).combine(invokeLegacy()) { totals, legacyCuTotals ->
                    yield()
                    totals.copy(
                        totalSizeToTransfer = totals.totalSizeToTransfer + legacyCuTotals.totalSizeToTransfer,
                        totalSizeTransferred = totals.totalSizeTransferred + legacyCuTotals.totalSizeTransferred,
                        pendingUploads = getNumPendingUploadsUseCase(),
                        paused = areTransfersPaused()
                    )
                }
            }
        }


    private fun invokeNew(transferTypes: List<TransferType>): Flow<TransfersStatusInfo> =
        combine(transferTypes.map {
            monitorOngoingActiveTransfersUseCase(it)
        }) { monitorOngoingActiveTransfersResults ->
            TransfersStatusInfo(
                totalSizeToTransfer = monitorOngoingActiveTransfersResults.sumOf { it.activeTransferTotals.totalBytes },
                totalSizeTransferred = monitorOngoingActiveTransfersResults.sumOf { it.activeTransferTotals.transferredBytes },
                pendingUploads = monitorOngoingActiveTransfersResults
                    .filter { it.activeTransferTotals.transfersType.isUploadType() }
                    .sumOf { it.activeTransferTotals.pendingFileTransfers },
                pendingDownloads = monitorOngoingActiveTransfersResults
                    .filter { it.activeTransferTotals.transfersType.isDownloadType() }
                    .sumOf { it.activeTransferTotals.pendingFileTransfers },
                paused = monitorOngoingActiveTransfersResults
                    .filter { it.activeTransferTotals.hasOngoingTransfers() }
                    .all { it.paused },
                transferOverQuota =
                monitorOngoingActiveTransfersResults.any { it.transfersOverQuota },
                storageOverQuota =
                monitorOngoingActiveTransfersResults.any { it.storageOverQuota },
            )
        }

    private fun invokeLegacy() = repository.monitorTransferEvents()
        .filter { it.transfer.transferType == TransferType.CU_UPLOAD }
        .distinctUntilChanged()
        .map {
            val transfer = it.transfer
            transferMap[transfer.tag] = transfer

            var totalBytes: Long = 0
            var totalTransferred: Long = 0

            val megaTransfers = transferMap.values.toList()
            megaTransfers.forEach { itemTransfer ->
                with(itemTransfer) {
                    totalBytes += this.totalBytes
                    totalTransferred +=
                        if (state == TransferState.STATE_COMPLETED) this.totalBytes
                        else transferredBytes
                }
            }
            // we only clear cache when all transfer done
            // if we remove in OnTransferFinish it can cause the progress show incorrectly
            if (megaTransfers.all { megaTransfer -> megaTransfer.isFinished }) {
                transferMap.clear()
            }
            TransfersStatusInfo(
                totalSizeToTransfer = totalBytes,
                totalSizeTransferred = totalTransferred,
                pendingDownloads = 0,
                pendingUploads = 0,
                paused = false,
            )
        }.onStart {
            //in invokeLegacy we only receive updates with transfer updates, so we need to force a first update
            emit(TransfersStatusInfo())
        }
}