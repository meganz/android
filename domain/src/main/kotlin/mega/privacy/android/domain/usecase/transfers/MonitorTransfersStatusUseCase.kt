package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
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
            monitorOngoingActiveTransfersUseCase(it).map { results ->
                val totalsWithoutPreviews = results.activeTransferTotals.let { totals ->
                    totals.actionGroups.filter { group -> group.isPreviewDownload() }
                        .let { previewGroups ->
                            totals.copy(
                                totalFileTransfers = totals.totalFileTransfers
                                        - previewGroups.sumOf { group -> group.totalFiles },
                                totalFinishedFileTransfers = totals.totalFinishedTransfers
                                        - previewGroups.sumOf { group -> group.finishedFiles },
                                totalCompletedFileTransfers = totals.totalCompletedFileTransfers
                                        - previewGroups.sumOf { group -> group.completedFiles },
                                totalBytes = totals.totalBytes
                                        - previewGroups.sumOf { group -> group.totalBytes },
                                transferredBytes = totals.transferredBytes
                                        - previewGroups.sumOf { group -> group.transferredBytes },
                                totalAlreadyTransferredFiles = totals.totalAlreadyTransferredFiles
                                        - previewGroups.sumOf { group -> group.alreadyTransferred },
                                actionGroups = totals.actionGroups
                                    .filterNot { group -> group.isPreviewDownload() }
                            )
                        }
                }

                results.copy(activeTransferTotals = totalsWithoutPreviews)
            }
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
                    .filter { it.activeTransferTotals.hasOngoingTransfers() }.let {
                        it.isNotEmpty() && it.all { it.paused }
                    },
                transferOverQuota =
                monitorOngoingActiveTransfersResults.any { it.transfersOverQuota },
                storageOverQuota =
                monitorOngoingActiveTransfersResults.any { it.storageOverQuota },
                cancelled = monitorOngoingActiveTransfersResults.sumOf { it.activeTransferTotals.totalCancelled }
            )
        }

}