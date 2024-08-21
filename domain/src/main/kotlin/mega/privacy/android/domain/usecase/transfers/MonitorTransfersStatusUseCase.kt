package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import javax.inject.Inject

/**
 * Monitor active transfers and emits [TransfersStatusInfo]
 */
class MonitorTransfersStatusUseCase @Inject constructor(
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
) {

    /**
     * Invoke.
     *
     * @return Flow of [TransfersStatusInfo]
     */

    operator fun invoke(): Flow<TransfersStatusInfo> =
        combine(TransferType.entries.filter { it != TransferType.NONE }.map {
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

}